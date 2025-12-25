package name.remal.gradle_plugins.content_loader.internal;

import java.io.Serializable;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ContentLoaderBuildService<T extends BuildServiceParameters> extends BuildService<T>, Serializable {
}
