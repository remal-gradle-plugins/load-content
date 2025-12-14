package name.remal.gradle_plugins.content_loader;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class ContentLoaderPluginFunctionalTest {

    final GradleProject project;

    @Test
    void helpTaskWorks() {
        project.getBuildFile().applyPlugin("name.remal.content-loader");
        project.assertBuildSuccessfully("help");
    }

}
