package name.remal.gradle_plugins.content_loader.internal;

import static name.remal.gradle_plugins.content_loader.internal.HasConfigurableValueUtils.callConfigurableValueMethodRecursively;

import com.google.errorprone.annotations.ForOverride;
import java.util.stream.Stream;
import org.gradle.api.provider.HasConfigurableValue;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public abstract class HasConfigurableValues implements HasConfigurableValue {

    @ForOverride
    protected abstract Stream<HasConfigurableValue> streamConfigurableValues();


    @Override
    public void finalizeValue() {
        streamConfigurableValues().forEach(value ->
            callConfigurableValueMethodRecursively(value, HasConfigurableValue::finalizeValue)
        );
    }

    @Override
    public void finalizeValueOnRead() {
        streamConfigurableValues().forEach(value ->
            callConfigurableValueMethodRecursively(value, HasConfigurableValue::finalizeValueOnRead)
        );
    }

    @Override
    public void disallowChanges() {
        streamConfigurableValues().forEach(value ->
            callConfigurableValueMethodRecursively(value, HasConfigurableValue::disallowChanges)
        );
    }

    @Override
    public void disallowUnsafeRead() {
        streamConfigurableValues().forEach(value ->
            callConfigurableValueMethodRecursively(value, HasConfigurableValue::disallowUnsafeRead)
        );
    }

}
