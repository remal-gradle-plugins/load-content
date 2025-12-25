package name.remal.gradle_plugins.content_loader;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static name.remal.gradle_plugins.content_loader.internal.JacksonUtils.JSON_MAPPER;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest
@RequiredArgsConstructor
class ContentLoaderPluginFunctionalTest {

    final GradleProject project;
    final WireMockRuntimeInfo wireMockInfo;

    @BeforeEach
    void beforeEach() {
        project.forBuildFile(build -> {
            build.applyPlugin("name.remal.content-loader");
        });
    }


    @Test
    void json() {
        @Language("JSON") var json = """
            {
              "id": "u-1",
              "active": true,
              "profile": {
                "name": "John Doe",
                "age": 30,
                "tags": ["vip", "beta"],
                "address": {
                  "city": "Los Angeles",
                  "zip": null
                }
              },
              "items": [
                { "sku": "A1", "qty": 1, "price": 10.5 },
                { "sku": "B2", "qty": 2, "price": 3.0 }
              ]
            }
            """;
        var jsonNode = JSON_MAPPER.readTree(json);
        stubFor(get("/json")
            .willReturn(okForJson(jsonNode))
        );

        project.forBuildFile(build -> {
            build.line("""
                def contentProvider = contentLoader.http.load {
                    uri('${{BASE_URL}}/json')
                }
                """.replace("${{BASE_URL}}", wireMockInfo.getHttpBaseUrl())
            );
            build.block("tasks.register('pluginTest')", task -> {
                task.block("doLast", doLast -> {
                    doLast.line("""
                        logger.lifecycle('Executing task {}', name)
                        def content = contentProvider.get().asJson()
                        content = content.selectByJsonPath('$..items[?(@.qty >= 2)].sku')
                        def skus = content.value
                        assert skus == ['B2']
                        """);
                });
            });
        });

        project.assertBuildSuccessfully("pluginTest");
    }

}
