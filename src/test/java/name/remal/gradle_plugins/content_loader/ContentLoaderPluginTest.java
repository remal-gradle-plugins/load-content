package name.remal.gradle_plugins.content_loader;

import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.unwrapGeneratedSubclass;
import static name.remal.gradle_plugins.toolkit.testkit.ProjectValidations.executeAfterEvaluateActions;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.TaskValidations;
import org.gradle.api.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class ContentLoaderPluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(ContentLoaderPlugin.class);
    }

    @AfterEach
    void afterEach() throws Throwable {
        for (var registration : project.getGradle().getSharedServices().getRegistrations()) {
            if (registration.getName().contains(ContentLoaderPlugin.class.getPackageName())) {
                var service = registration.getService().get();
                if (service instanceof AutoCloseable autoCloseable) {
                    autoCloseable.close();
                }
            }
        }
    }

    @Test
    void pluginTasksDoNotHavePropertyProblems() {
        executeAfterEvaluateActions(project);

        var taskClassNamePrefix = packageNameOf(ContentLoaderPlugin.class) + '.';
        project.getTasks().stream()
            .filter(task -> {
                var taskClass = unwrapGeneratedSubclass(task.getClass());
                return taskClass.getName().startsWith(taskClassNamePrefix);
            })
            .map(TaskValidations::markTaskDependenciesAsSkipped)
            .forEach(TaskValidations::assertNoTaskPropertiesProblems);
    }

    @Test
    void test() {
        var extension = project.getExtensions().getByType(ContentLoaderExtension.class);
        extension.getHttp().load(params -> {
            params.uri("https://api.foojay.io/disco/v3.0/major_versions?ga=true&discovery_scope_id=public");
            params.uri("https://services.gradle.org/versions/all");
        }).get();
    }

}
