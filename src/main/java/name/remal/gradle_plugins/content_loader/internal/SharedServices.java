package name.remal.gradle_plugins.content_loader.internal;

import static name.remal.gradle_plugins.toolkit.ProxyUtils.toDynamicInterface;

import org.gradle.api.Action;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.services.BuildServiceSpec;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
interface SharedServices {

    static <T extends BuildService<P>, P extends BuildServiceParameters> T getBuildService(
        Gradle gradle,
        Class<T> interfaceClass,
        Class<? extends T> serviceClass
    ) {
        return getBuildService(
            gradle,
            interfaceClass,
            serviceClass,
            spec -> {
                // No special configuration
            }
        );
    }

    static <T extends BuildService<P>, P extends BuildServiceParameters> T getBuildService(
        Gradle gradle,
        Class<T> interfaceClass,
        Class<? extends T> serviceClass,
        Action<? super BuildServiceSpec<P>> configureAction
    ) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Not an interface: " + interfaceClass);
        }

        while (true) {
            var parentGradle = gradle.getParent();
            if (parentGradle == null) {
                break;
            } else {
                gradle = parentGradle;
            }
        }

        var serviceName = interfaceClass.getName();
        var existingServiceRegistration = gradle.getSharedServices().getRegistrations().findByName(serviceName);
        var service = existingServiceRegistration != null
            ? existingServiceRegistration.getService().get()
            : null;

        if (service == null) {
            service = gradle.getSharedServices().registerIfAbsent(serviceName, serviceClass, configureAction).get();
        }

        return interfaceClass.isInstance(service)
            ? interfaceClass.cast(service)
            : toDynamicInterface(service, interfaceClass);
    }

}
