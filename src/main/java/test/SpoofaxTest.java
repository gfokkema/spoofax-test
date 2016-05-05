package test;

import static java.util.stream.StreamSupport.stream;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

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
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.analysis.AnalysisFacet;
import org.metaborg.spoofax.core.stratego.StrategoRuntimeFacet;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.concurrent.IClosableLock;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.HybridInterpreter;

public class SpoofaxTest {
//	final static String langPath = "org.metaborg.meta.lang.nabl-2.0.0-beta1.spoofax-language";
//	final static String sourcePath = "test.nabl";
	final static String langPath = "paplj.full";
	final static String projectPath = "/home/gerlof/spoofax-workspace/temp/";
	
	private Spoofax spoofax;
	
	public SpoofaxTest(Spoofax spoofax) {
		this.spoofax = spoofax;
	}
	
	private FileObject langLoc() {
		FileObject zipLoc = spoofax.resourceService.resolve("res:" + langPath);
		return spoofax.resourceService.resolve("zip:" + zipLoc + "!/");
	}
	
	private FileObject projectLoc() {
		return spoofax.resourceService.resolve(projectPath);
	}
	
	public void run() throws MetaborgException, IOException {
		IProject project = this.project(projectLoc());
		ILanguageImpl lang = this.lang(langLoc());
		IContext context = spoofax.contextService.get(projectLoc(), project, lang);
		
		String program = String.join("\n",
				"program",
				"run",
				"let",
				"  Num x = 0",
				"in {",
				"   x + 2;",
				"   x + 4",
				"}");
		ISpoofaxInputUnit input = spoofax.unitService.inputUnit(program, lang, null);
		ISpoofaxParseUnit parse_out = spoofax.syntaxService.parse(input);
		// PARSING DONE
		
		ITermFactory termFactory = spoofax.termFactoryService.getGeneric();
		IStrategoTerm inputTuple = termFactory.makeList(
				Arrays.asList(termFactory.makeAppl(
						termFactory.makeConstructor("File", 3),
						termFactory.makeString("null"),
						parse_out.ast(),
						termFactory.makeReal(parse_out.duration())
				))
		);
		// analyzer inputTerm: [ File(sourceLoc(), ast_in, duration) ]
		
		FacetContribution<AnalysisFacet> analysisContrib = lang.facetContribution(AnalysisFacet.class);
		IStrategoTerm analyze;
		try (IClosableLock lock = context.write()) {
			HybridInterpreter analysisRuntime = spoofax.strategoRuntimeService.runtime(analysisContrib.contributor, context);
			analyze = spoofax.strategoCommon.invoke(analysisRuntime, inputTuple, analysisContrib.facet.strategyName);
		}
		// ANALYSIS DONE
		
		IStrategoTerm ast = analyze.getSubterm(0).getSubterm(0).getSubterm(2);
		
		FacetContribution<StrategoRuntimeFacet> runContrib = lang.facetContribution(StrategoRuntimeFacet.class);
		HybridInterpreter runtime = spoofax.strategoRuntimeService.runtime(runContrib.contributor, projectLoc());
		stream(spoofax.strategoCommon.invoke(runtime, ast, "runstrat").spliterator(), false)
			.map(e -> "direct interp: " + e.toString())
			.forEach(System.out::println);
	}
	
	public IProject project(FileObject projectLoc) throws MetaborgException {
		ISimpleProjectService projectService = spoofax.injector.getInstance(SimpleProjectService.class);
		IProject project = projectService.create(projectLoc);
		
		return project;
	}
	
	public ILanguageImpl lang(FileObject langLoc) throws MetaborgException {
		// Discover languages inside zip file
		Iterable<ILanguageDiscoveryRequest> requests = spoofax.languageDiscoveryService.request(langLoc);
		Iterable<ILanguageComponent> components = spoofax.languageDiscoveryService.discover(requests);

		// Load the languages
		Set<ILanguageImpl> impls = LanguageUtils.toImpls(components);
		ILanguageImpl lang = LanguageUtils.active(impls);
		if(lang == null) throw new MetaborgException("No language implementation was found");
		
		return lang;
	}
	
	public static void main(String[] args) {
		try(Spoofax spoofax = new Spoofax()) {
			new SpoofaxTest(spoofax).run();
		} catch (IOException | MetaborgException e) {
			e.printStackTrace();
		}
	}
}
