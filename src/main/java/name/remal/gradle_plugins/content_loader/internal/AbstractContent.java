package name.remal.gradle_plugins.content_loader.internal;

import static java.lang.System.identityHashCode;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
public abstract class AbstractContent implements BaseContent {

    @Override
    public final String toString() {
        return getSource();
    }

    @Override
    public final boolean equals(@Nullable Object other) {
        return this == other;
    }

    @Override
    public final int hashCode() {
        return identityHashCode(this);
    }

}
