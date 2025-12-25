package name.remal.gradle_plugins.content_loader.http;

import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.getStringProperty;

interface Constants {

    String PLUGIN_ID = getStringProperty("plugin-id");

    String HTTP_CLIENT_CACHE_VERSION = getStringProperty("httpclient.cache.version");

}
