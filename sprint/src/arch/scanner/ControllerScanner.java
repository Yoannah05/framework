package arch.scanner;

import arch.annotation.Controller;
import arch.annotation.GET;
import arch.exception.PackageNotFoundException;
import arch.Mapping;
import arch.registry.MappingRegistry;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ControllerScanner {
    private final MappingRegistry mappingRegistry;
    private final String controllerPackage;

    public ControllerScanner(MappingRegistry mappingRegistry, String controllerPackage) {
        this.mappingRegistry = mappingRegistry;
        this.controllerPackage = controllerPackage;
    }

    public void scanControllers() {
        try {
            List<Class<?>> controllerClasses = getClasses(controllerPackage);
            for (Class<?> clazz : controllerClasses) {
                scanController(clazz);
            }
        } catch (ClassNotFoundException | IOException e) {
            throw new PackageNotFoundException(controllerPackage);
        }
    }

    private void scanController(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Controller.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GET.class)) {
                    GET getAnnotation = method.getAnnotation(GET.class);
                    String url = getAnnotation.value();
                    mappingRegistry.registerMapping(url, new Mapping(clazz.getName(), method.getName()));
                }
            }
        }
    }

    private List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL packageUrl = classLoader.getResource(path);

        if (packageUrl == null) {
            throw new PackageNotFoundException(packageName);
        }

        File directory = new File(packageUrl.getFile());
        if (!directory.exists()) {
            throw new PackageNotFoundException(packageName);
        }

        String[] files = directory.list();
        if (files != null) {
            for (String file : files) {
                if (file.endsWith(".class")) {
                    String className = packageName + '.' + file.substring(0, file.length() - 6);
                    classes.add(Class.forName(className));
                }
            }
        }
        return classes;
    }
}
