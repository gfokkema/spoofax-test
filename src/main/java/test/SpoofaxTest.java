package test;

import java.io.IOException;
import java.net.URL;
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
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.Spoofax;
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
	final static String projectPath = "~/spoofax-workspace/declare-your-language/paplj/paplj-examples";
	
	public static void main(String[] args) {
		try(final Spoofax spoofax = new Spoofax()) {
			new SpoofaxTest().run(spoofax);
		} catch(MetaborgException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run(Spoofax spoofax) throws MetaborgException, IOException {
		IProject project = this.project(spoofax);
		ILanguageImpl lang = this.lang(spoofax);
		
		ISpoofaxParseUnit parse_out = this.parse(spoofax, lang);
		IContext context = spoofax.contextService.get(parse_out.source(), project, lang);
		
		ISpoofaxAnalyzeUnit analyze;
		try(IClosableLock lock = context.write()) {
			analyze = spoofax.analysisService.analyze(parse_out, context).result();
		}
		Collection<ISpoofaxTransformUnit<ISpoofaxAnalyzeUnit>> transform = spoofax.transformService.transform(analyze, context, new EndNamedGoal("Run"));
		transform.forEach(t -> System.out.println(t.ast()));
	}
	
	private IProject project(Spoofax spoofax) throws MetaborgException {
		ISimpleProjectService projectService = spoofax.injector.getInstance(SimpleProjectService.class);
		FileObject projectLocation = spoofax.resourceService.resolve(projectPath);
		IProject project = projectService.create(projectLocation);
		
		return project;
	}
	
	private ILanguageImpl lang(Spoofax spoofax) throws MetaborgException {
		// Load zip file given by path
		URL langUrl = SpoofaxTest.class.getClassLoader().getResource(langPath);
		FileObject langLocation = spoofax.resourceService.resolve("zip:" + langUrl + "!/");

		// Discover languages inside zip file
		Iterable<ILanguageDiscoveryRequest> requests = spoofax.languageDiscoveryService.request(langLocation);
		Iterable<ILanguageComponent> components = spoofax.languageDiscoveryService.discover(requests);

		// Load the languages
		Set<ILanguageImpl> impls = LanguageUtils.toImpls(components);
		ILanguageImpl lang = LanguageUtils.active(impls);
		if(lang == null) {
			throw new MetaborgException("No language implementation was found");
		}
		
		return lang;
	}
	
	private ISpoofaxParseUnit parse(Spoofax spoofax, ILanguageImpl lang) throws IOException, ParseException {
		// Load a file in this language
		FileObject sourceFile    = spoofax.resourceService.resolve("res:" + sourcePath);
		String sourceContents    = spoofax.sourceTextService.text(sourceFile);
		ISpoofaxInputUnit input  = spoofax.unitService.inputUnit(sourceFile, sourceContents, lang, null);
		
		// Parse it using Spoofax
		ISpoofaxParseUnit output = spoofax.syntaxService.parse(input);
		if(!output.valid()) throw new IOException("Could not parse " + sourceFile);
		
		System.out.println("Parsed: " + output.ast());
		return output;
	}
}
