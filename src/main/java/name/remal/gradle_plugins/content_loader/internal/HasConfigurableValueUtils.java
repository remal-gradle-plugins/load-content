package name.remal.gradle_plugins.content_loader.internal;

import java.util.function.Consumer;
import org.gradle.api.provider.HasConfigurableValue;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.SetProperty;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
interface HasConfigurableValueUtils {

    static void callConfigurableValueMethodRecursively(
        HasConfigurableValue configurable,
        Consumer<HasConfigurableValue> method
    ) {
        method.accept(configurable);
        if (configurable instanceof ListProperty<?> property) {
            property.get().forEach(item -> {
                if (item instanceof HasConfigurableValue it) {
                    callConfigurableValueMethodRecursively(it, method);
                }
            });

        } else if (configurable instanceof SetProperty<?> property) {
            property.get().forEach(item -> {
                if (item instanceof HasConfigurableValue it) {
                    callConfigurableValueMethodRecursively(it, method);
                }
            });

        } else if (configurable instanceof MapProperty<?, ?> property) {
            property.get().forEach((key, value) -> {
                if (key instanceof HasConfigurableValue it) {
                    callConfigurableValueMethodRecursively(it, method);
                }
                if (value instanceof HasConfigurableValue it) {
                    callConfigurableValueMethodRecursively(it, method);
                }
            });
        }
    }

}
