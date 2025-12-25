package name.remal.gradle_plugins.content_loader.content;

import name.remal.gradle_plugins.content_loader.content.json.JsonContent;
import name.remal.gradle_plugins.content_loader.content.json.JsonContentDefault;
import name.remal.gradle_plugins.content_loader.internal.BaseContent;

public interface Content extends BaseContent {

    default JsonContent asJson() {
        return new JsonContentDefault(this);
    }

}
