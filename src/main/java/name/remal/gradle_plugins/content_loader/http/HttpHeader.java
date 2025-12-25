package name.remal.gradle_plugins.content_loader.http;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import name.remal.gradle_plugins.content_loader.internal.HasConfigurableValues;
import org.gradle.api.provider.HasConfigurableValue;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.jspecify.annotations.Nullable;

public abstract class HttpHeader extends HasConfigurableValues {

    @Input
    public abstract Property<String> getName();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<String> getValue();


    @Override
    @OverridingMethodsMustInvokeSuper
    protected Stream<HasConfigurableValue> streamConfigurableValues() {
        return Stream.of(
            getName(),
            getValue()
        );
    }

    protected Map<String, @Nullable Object> toMap() {
        var map = new LinkedHashMap<String, @Nullable Object>();
        map.put("name", getName().getOrElse(""));
        map.put("value", getValue().getOrElse(""));

        return map;
    }

    protected void validate() {
        // required properties:
        getName().get();
    }

}
