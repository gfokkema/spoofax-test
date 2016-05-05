package test;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.FacetContribution;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageDiscoveryRequest;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageUtils;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.analysis.AnalysisFacet;
import org.metaborg.spoofax.core.stratego.StrategoRuntimeFacet;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.concurrent.IClosableLock;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.HybridInterpreter;

import java.io.IOException;
import java.util.Set;

/**
 * Test class to play around with Spoofax's API.
 */
public final class SpoofaxTest {
	private Spoofax spoofax;
	private ILanguageImpl lang;
	private IContext context;

	public SpoofaxTest(Spoofax spoofax, String langPath, String projectPath) throws MetaborgException {
		this.spoofax = spoofax;
		this.lang = this.lang(langLoc(langPath));

		IProject project = this.project(projectLoc(projectPath));
		this.context = spoofax.contextService.get(projectLoc(projectPath), project, lang);
	}
	
	private FileObject langLoc(String langPath) {
		FileObject zipLoc = spoofax.resourceService.resolve("res:" + langPath);
		return spoofax.resourceService.resolve("zip:" + zipLoc + "!/");
	}

	private FileObject projectLoc(String projectPath) {
		return spoofax.resourceService.resolve(projectPath);
	}

	private IProject project(FileObject projectLoc) throws MetaborgException {
		ISimpleProjectService projectService =
				spoofax.injector.getInstance(SimpleProjectService.class);
		return projectService.create(projectLoc);
	}

	private ILanguageImpl lang(FileObject langLoc) throws MetaborgException {
		// Discover languages inside zip file
		Iterable<ILanguageDiscoveryRequest> requests =
				spoofax.languageDiscoveryService.request(langLoc);
		Iterable<ILanguageComponent> components =
				spoofax.languageDiscoveryService.discover(requests);

		// Load the languages
		Set<ILanguageImpl> impls = LanguageUtils.toImpls(components);
		ILanguageImpl lang = LanguageUtils.active(impls);
		if (lang == null) {
			throw new MetaborgException("No language implementation was found");
		}

		return lang;
	}

	private ISpoofaxParseUnit parse(String source) throws ParseException {
		ISpoofaxInputUnit input = spoofax.unitService.inputUnit(source, lang, null);
		return spoofax.syntaxService.parse(input);
	}

	private IStrategoTerm analyze(ISpoofaxParseUnit parseUnit) throws MetaborgException {
		ITermFactory termFactory = spoofax.termFactoryService.getGeneric();
		IStrategoTerm inputTuple = termFactory.makeList(
			termFactory.makeAppl(
			  termFactory.makeConstructor("File", 3)
			, termFactory.makeString("null")
			, parseUnit.ast()
			, termFactory.makeReal(parseUnit.duration())
			)
		);

		// analyzer inputTerm:
		//   [ File(source, ast_in, duration) ]

		// analyzer outputTerm:
		//   Result(
		//     [ FileResult(source, ast_in, ast_out, [ errors ], [ warnings ], [ notes ] ) ]
		//   , [ MessageResult(source, [ errors ], [ warnings ], [ notes ] ]
		//   , [ sources ]
		//   , DebugResult(_)
		//   , TimeResult(_)
		//   )

		FacetContribution<AnalysisFacet> analysisContrib = lang.facetContribution(AnalysisFacet.class);

		IStrategoTerm analyze;
		try (IClosableLock lock = context.write()) {
			HybridInterpreter analysisRuntime = spoofax.strategoRuntimeService.runtime(analysisContrib.contributor, context);
			analyze = spoofax.strategoCommon.invoke(analysisRuntime, inputTuple, analysisContrib.facet.strategyName);
		}

		return analyze;
	}

	private IStrategoTerm interp(IStrategoTerm analyzeAst) throws MetaborgException {
		FacetContribution<StrategoRuntimeFacet> runContrib = context.language().facetContribution(StrategoRuntimeFacet.class);
		HybridInterpreter runtime = spoofax.strategoRuntimeService.runtime(runContrib.contributor, this.context);
		IStrategoTerm interp = spoofax.strategoCommon.invoke(runtime, analyzeAst, "runstrat");

		return interp;
	}

	public IStrategoTerm run(String source) throws MetaborgException, IOException {
		ISpoofaxParseUnit parseOut = this.parse(source);
		IStrategoTerm analyzeResult = this.analyze(parseOut);

		IStrategoTerm analyzeAst = analyzeResult.getSubterm(0).getSubterm(0).getSubterm(2);
		IStrategoTerm analyzeErrors = analyzeResult.getSubterm(0).getSubterm(0).getSubterm(3);
		
		if (analyzeErrors.getSubtermCount() > 0)
			return analyzeErrors;
		else
			return this.interp(analyzeAst);
	}
}
