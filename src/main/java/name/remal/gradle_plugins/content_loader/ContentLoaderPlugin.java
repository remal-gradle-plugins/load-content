package name.remal.gradle_plugins.content_loader;

import static name.remal.gradle_plugins.content_loader.internal.ContentBuildCache.getContentBuildCacheFor;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public abstract class ContentLoaderPlugin implements Plugin<Project> {

    public static final String CONTENT_LOADER_EXTENSION_NAME = doNotInline("contentLoader");

    @Override
    public void apply(Project project) {
        getContentBuildCacheFor(project.getGradle()).registerProject(project);
        project.getExtensions().create(CONTENT_LOADER_EXTENSION_NAME, ContentLoaderExtension.class);
    }

}
