package name.remal.gradle_plugins.content_loader.internal;

import static name.remal.gradle_plugins.content_loader.internal.SharedServices.getBuildService;

import java.util.Map;
import java.util.concurrent.Callable;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.services.BuildServiceParameters;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
@SuppressWarnings("try")
public interface ContentBuildCache
    extends ContentLoaderBuildService<BuildServiceParameters.None>, AutoCloseable {

    static ContentBuildCache getContentBuildCacheFor(Gradle gradle) {
        return getBuildService(gradle, ContentBuildCache.class, ContentBuildCacheImpl.class);
    }


    void registerProject(Project project);

    Object getOrLoadContent(Map<String, @Nullable Object> key, Callable<AutoCloseable> loader);

}
