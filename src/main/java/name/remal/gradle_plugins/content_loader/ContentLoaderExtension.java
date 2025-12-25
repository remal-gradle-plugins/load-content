package name.remal.gradle_plugins.content_loader;

import name.remal.gradle_plugins.content_loader.http.HttpLoader;
import org.gradle.api.tasks.Nested;

public interface ContentLoaderExtension {

    @Nested
    HttpLoader getHttp();

}
