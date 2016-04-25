package test;

import java.net.URL;
import java.util.function.Consumer;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.action.EndNamedGoal;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageDiscoveryRequest;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageUtils;
import org.metaborg.core.processing.CancellationToken;
import org.metaborg.core.processing.NullProgressReporter;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;

public class SpoofaxTest {
//	final static String langPath = "org.metaborg.meta.lang.nabl-2.0.0-beta1.spoofax-language";
//	final static String sourcePath = "test.nabl";
	final static String langPath = "paplj.full";
	final static String sourcePath = "fib.pj";
	
	public static void main(String[] args) throws InterruptedException {
		try(final Spoofax spoofax = new Spoofax()) {
			// Load zip file given by path
			URL langUrl = SpoofaxTest.class.getClassLoader().getResource(langPath);
			FileObject langLocation = spoofax.resourceService.resolve("zip:" + langUrl + "!/");

			// Discover languages inside zip file
			Iterable<ILanguageDiscoveryRequest> requests = spoofax.languageDiscoveryService.request(langLocation);
			Iterable<ILanguageComponent> components = spoofax.languageDiscoveryService.discover(requests);

			// Load the languages
			ILanguageImpl lang = LanguageUtils.active(LanguageUtils.toImpls(components));
			if(lang == null) throw new MetaborgException("No language implementation was found");

			// Initalize project
			IProject project = spoofax.injector.getInstance(SimpleProjectService.class)
									.create(spoofax.resourceService.resolve("~/project"));

			// Context is managed by build input
			BuildInput build = new BuildInputBuilder(project)
										.addSource(spoofax.resourceService.resolve("res:" + sourcePath))
										.addTransformGoal(new EndNamedGoal("Run"))
										.build(spoofax.dependencyService, spoofax.languagePathService);
			spoofax.builder.build(build, new NullProgressReporter(), new CancellationToken()).transformResults()
				.forEach(new Consumer<ISpoofaxTransformUnit<?>>() {
					@Override
					public void accept(ISpoofaxTransformUnit<?> t) {
						System.out.println("Output: " + t.ast());
					}
			});
		} catch(MetaborgException e) {
			e.printStackTrace();
		}
	}
}
