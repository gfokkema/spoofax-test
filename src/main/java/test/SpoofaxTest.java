package test;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.action.EndNamedGoal;
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
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;

import rx.Observable;

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
		IContext context = spoofax.contextService.get(sourceLoc(), project, lang);
		
//		List<String> goals = Arrays.asList("Show abstract syntax", "Desugar AST", "Run");
//		Iterable<ActionFacet> facets = lang.facets(ActionFacet.class);
		
		String source = spoofax.sourceTextService.text(sourceLoc());
		ISpoofaxInputUnit input = spoofax.unitService.inputUnit(sourceLoc(), source, lang, null);
		
		spoofax.analysisResultProcessor
			.request(input, context)
			.doOnNext(analysis -> System.out.println("Analyze: " + analysis.ast()))
			.flatMap(analysis -> {
				Collection<ISpoofaxTransformUnit<ISpoofaxAnalyzeUnit>> terms = null;
				try {
					terms = spoofax.transformService
							.transform(analysis, context, new EndNamedGoal("Run"));
				} catch (TransformException e) {
					e.printStackTrace();
				}
				return Observable.from(terms);
			})
			.map(term -> term.ast())
			.doOnNext(term -> System.out.println("Result: " + term))
			.subscribe()
			;
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
