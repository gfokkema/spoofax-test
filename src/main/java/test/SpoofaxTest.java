package test;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

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
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

public class SpoofaxTest {
//	final static String langPath = "org.metaborg.meta.lang.nabl-2.0.0-beta1.spoofax-language";
//	final static String sourcePath = "test.nabl";
	final static String langPath = "paplj.full";
	final static String sourcePath = "fib.pj";
	final static String projectPath = "~/spoofax-workspace/declare-your-language/paplj/paplj-examples";
	
	public static void main(String[] args) {
		try(final Spoofax spoofax = new Spoofax()) {
			SpoofaxTest test = new SpoofaxTest();
			
			IProject project = test.project(spoofax);
			ILanguageImpl lang = test.lang(spoofax);
			if(lang == null) {
				System.out.println("No language implementation was found");
				return;
			}
			ISpoofaxParseUnit output = test.load(spoofax, lang);
			System.out.println("Parsed: " + output.ast());
			
			IContext context = spoofax.contextService.get(output.source(), project, lang);
			ISpoofaxAnalyzeUnit analyze = spoofax.analysisService.analyze(output, context).result();
//			Collection<ISpoofaxTransformUnit<ISpoofaxAnalyzeUnit>> transform = spoofax.transformService.transform(analyze, context, new EndNamedGoal("Run"));
//			
//			System.out.println(transform);
		} catch(MetaborgException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private IProject project(Spoofax spoofax) throws MetaborgException {
		ISimpleProjectService projectService = spoofax.injector.getInstance(SimpleProjectService.class);
		FileObject projectLocation = spoofax.resourceService.resolve(projectPath);
		
		return projectService.create(projectLocation);
	}
	
	private ILanguageImpl lang(Spoofax spoofax) throws MetaborgException {
		// Load zip file given by path
		URL langUrl = SpoofaxTest.class.getClassLoader().getResource(langPath);
		FileObject langLocation = spoofax.resourceService.resolve("zip:" + langUrl + "!/");

		// Discover languages inside zip file
		Iterable<ILanguageDiscoveryRequest> requests =
				spoofax.languageDiscoveryService.request(langLocation);
		Iterable<ILanguageComponent> components =
				spoofax.languageDiscoveryService.discover(requests);

		// Load the languages
		Set<ILanguageImpl> implementations = LanguageUtils.toImpls(components);
		
		return LanguageUtils.active(implementations);
	}
	
	private ISpoofaxParseUnit load(Spoofax spoofax, ILanguageImpl lang) throws IOException, ParseException {
		// Load a file in this language
		FileObject sourceFile    = spoofax.resourceService.resolve("res:" + sourcePath);
		String sourceContents    = spoofax.sourceTextService.text(sourceFile);
		ISpoofaxInputUnit input  = spoofax.unitService.inputUnit(sourceFile, sourceContents, lang, null);
		
		// Parse it using Spoofax
		ISpoofaxParseUnit output = spoofax.syntaxService.parse(input);
		if(!output.valid()) throw new IOException("Could not parse " + sourceFile);
		
		return output;
	}
}
