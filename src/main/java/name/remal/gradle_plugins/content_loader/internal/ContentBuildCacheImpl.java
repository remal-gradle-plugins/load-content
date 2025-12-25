package name.remal.gradle_plugins.content_loader.internal;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.synchronizedSortedMap;
import static lombok.AccessLevel.PUBLIC;
import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.getStringProperty;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;
import static name.remal.gradle_plugins.toolkit.PathUtils.normalizePath;
import static name.remal.gradle_plugins.toolkit.PathUtils.tryToDeleteRecursivelyIgnoringFailure;
import static name.remal.gradle_plugins.toolkit.ProxyUtils.toDynamicInterface;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrowsFunction;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.gradle.api.Project;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
@NoArgsConstructor(access = PUBLIC, onConstructor_ = {@Inject})
@CustomLog
@SuppressWarnings("try")
public abstract class ContentBuildCacheImpl implements ContentBuildCache {

    private static final String PLUGIN_ID = getStringProperty("plugin-id");

    private final SortedMap<URI, LazyValue<URI>> cacheFolders = synchronizedSortedMap(new TreeMap<>());

    @Override
    public void registerProject(Project project) {
        var projectPath = normalizePath(project.getProjectDir().toPath()).toUri();
        var cacheFolder = lazyValue(() ->
            normalizePath(
                project
                    .getLayout()
                    .getBuildDirectory()
                    .get()
                    .getAsFile()
                    .toPath()
                    .resolve("tmp")
                    .resolve(PLUGIN_ID)
            ).toUri()
        );
        cacheFolders.put(projectPath, cacheFolder);
    }

    private Path getCacheDir() {
        
    }


    private final ConcurrentMap<Map<String, Object>, FileContent> cache = new ConcurrentHashMap<>();


    @Override
    public Object getOrLoadContent(Map<String, @Nullable Object> key, Callable<AutoCloseable> loader) {
        var canonizedKey = canonizeKeyValue(key);
        //canonizedKey.put("_nanos", System.nanoTime());
        return cache.computeIfAbsent(canonizedKey, sneakyThrowsFunction(__ -> {
            try (var untypedContent = loader.call()) {
                var content = untypedContent instanceof StreamingContent typedContent
                    ? typedContent
                    : toDynamicInterface(untypedContent, StreamingContent.class);
                var tempFile = normalizePath(createTempFile(ContentBuildCache.class.getSimpleName() + '-', ".tmp"));
                logger.warn("Writing {}", tempFile);
                try (var in = content.getInputStream()) {
                    copy(in, tempFile, REPLACE_EXISTING);
                }
                return new FileContent(content.getSource(), tempFile, content.getCharset());
            }
        }));
    }

    @Override
    public void close() {
        for (var content : cache.values()) {
            logger.warn("Removing {}", content.getContentFilePath());
            tryToDeleteRecursivelyIgnoringFailure(content.getContentFilePath());
        }
        cache.clear();
    }


    private static Object canonizeKeyValue(Object object) {
        return switch (object) {
            case Map<?, ?> map -> canonizeKeyValue(map);
            case Iterable<?> list -> canonizeKeyValue(list);
            default -> object;
        };
    }

    private static Map<String, Object> canonizeKeyValue(Map<?, ?> object) {
        var result = new HashMap<String, Object>();
        object.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }

            result.put(key.toString(), canonizeKeyValue(value));
        });
        return result;
    }

    private static List<Object> canonizeKeyValue(Iterable<?> object) {
        var result = new ArrayList<>();
        for (var item : object) {
            if (item == null) {
                continue;
            }
            result.add(canonizeKeyValue(item));
        }
        return result;
    }

}
