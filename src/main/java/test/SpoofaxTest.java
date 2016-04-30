package test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageDiscoveryRequest;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageUtils;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.core.transform.TransformException;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.action.ActionFacet;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;
import org.metaborg.util.concurrent.IClosableLock;

public class SpoofaxTest {
//	final static String langPath = "org.metaborg.meta.lang.nabl-2.0.0-beta1.spoofax-language";
//	final static String sourcePath = "test.nabl";
	final static String langPath = "paplj.full";
	final static String sourcePath = "fib.pj";
	final static String projectPath = "/home/gerlof/spoofax-workspace/declare-your-language/paplj/paplj-examples";
	
	private Spoofax spoofax;
	
	public SpoofaxTest(Spoofax spoofax) {
		this.spoofax = spoofax;
	}
	
	private FileObject langLoc() {
		FileObject zipLoc = spoofax.resourceService.resolve("res:" + langPath);
		return spoofax.resourceService.resolve("zip:" + zipLoc + "!/");
	}
	
	private FileObject sourceLoc() {
		return spoofax.resourceService.resolve("res:" + sourcePath);
	}
	
	private FileObject projectLoc() {
		return spoofax.resourceService.resolve(projectPath);
	}
	
	public void run() throws MetaborgException, IOException {
		IProject project = this.project(projectLoc());
		ILanguageImpl lang = this.lang(langLoc());
		
		ISpoofaxParseUnit parse_out = this.parse(lang, sourceLoc());
		IContext context = spoofax.contextService.get(parse_out.source(), project, lang);
		
		ISpoofaxAnalyzeUnit analyze;
		try(IClosableLock lock = context.write()) {
			analyze = spoofax.analysisService.analyze(parse_out, context).result();
		}
		
		List<String> goals = Arrays.asList("Show abstract syntax", "Desugar AST", "Run");
		
		StreamSupport.stream(lang.facets(ActionFacet.class).spliterator(), false)
			.flatMap(af -> af.actions.entries().stream())
			.map(entry -> entry.getKey())
			.filter(goal ->
				goals.stream()
					.map(e -> "'" + e + "'")
					.map(e -> goal.toString().equals(e))
					.reduce(false, (a, b) -> a || b)
			)
			.flatMap(goal -> {
				Stream<ISpoofaxTransformUnit<ISpoofaxAnalyzeUnit>> term = null;
				try {
					term = spoofax.transformService
						.transform(analyze, context, goal)
						.stream();
				} catch (TransformException e) {
					e.printStackTrace();
				}
				return term;
			})
			.map(term -> term.ast())
			.collect(Collectors.toList())
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
	
	public ISpoofaxParseUnit parse(ILanguageImpl lang, FileObject sourceFile) throws IOException, MetaborgException {
		// Load a file in this language
		String sourceContents    = spoofax.sourceTextService.text(sourceFile);
		ISpoofaxInputUnit input  = spoofax.unitService.inputUnit(sourceFile, sourceContents, lang, null);
		
		// Parse it using Spoofax
		ISpoofaxParseUnit output = spoofax.syntaxService.parse(input);
		if(!output.valid()) throw new MetaborgException("Could not parse " + sourceFile);
		
		return output;
	}
	
	public static void main(String[] args) {
		try(Spoofax spoofax = new Spoofax()) {
			new SpoofaxTest(spoofax).run();
		} catch (IOException | MetaborgException e) {
			e.printStackTrace();
		}
	}
}
