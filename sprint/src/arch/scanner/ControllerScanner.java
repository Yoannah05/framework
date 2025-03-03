package arch.scanner;

import arch.annotation.Controller;
import arch.annotation.GET;
import arch.annotation.POST;
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
            if (controllerClasses.isEmpty()) {
                throw new PackageNotFoundException("Aucune classe trouvée dans le package " + controllerPackage);
            }
            
            for (Class<?> clazz : controllerClasses) {
                scanController(clazz);
            }
            
            // Vérifier si des mappings ont été trouvés
            if (mappingRegistry.getRegisteredUrls().isEmpty()) {
                throw new PackageNotFoundException("Aucun contrôleur annoté avec @Controller trouvé dans le package " + controllerPackage);
            }
        } catch (ClassNotFoundException e) {
            throw new PackageNotFoundException("Impossible de charger les classes du package " + controllerPackage + ": " + e.getMessage());
        } catch (IOException e) {
            throw new PackageNotFoundException("Erreur d'accès au package " + controllerPackage + ": " + e.getMessage());
        } catch (Exception e) {
            throw new PackageNotFoundException("Erreur lors du scan des contrôleurs: " + e.getMessage());
        }
    }

    private void scanController(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Controller.class)) {
            boolean hasEndpoints = false;
            
            for (Method method : clazz.getDeclaredMethods()) {
                // Register GET methods
                if (method.isAnnotationPresent(GET.class)) {
                    GET getAnnotation = method.getAnnotation(GET.class);
                    String url = getAnnotation.value();
                    Mapping mapping = new Mapping(clazz.getName(), method.getName());
                    mapping.setVerb("GET");
                    mappingRegistry.registerMapping(url, mapping);
                    hasEndpoints = true;
                }
                
                // Register POST methods
                if (method.isAnnotationPresent(POST.class)) {
                    POST postAnnotation = method.getAnnotation(POST.class);
                    String url = postAnnotation.value();
                    Mapping mapping = new Mapping(clazz.getName(), method.getName());
                    mapping.setVerb("POST");
                    mappingRegistry.registerMapping(url, mapping);
                    hasEndpoints = true;
                }
            }
            
            if (!hasEndpoints) {
                System.out.println("Attention: Le contrôleur " + clazz.getName() + 
                                  " est annoté avec @Controller mais ne contient aucune méthode annotée avec @GET ou @POST");
            }
        }
    }

    private List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL packageUrl = classLoader.getResource(path);

        if (packageUrl == null) {
            throw new PackageNotFoundException("Package " + packageName + " introuvable");
        }

        File directory = new File(packageUrl.getFile());
        if (!directory.exists()) {
            throw new PackageNotFoundException("Le répertoire correspondant au package " + packageName + " n'existe pas");
        }

        String[] files = directory.list();
        if (files == null || files.length == 0) {
            throw new PackageNotFoundException("Le package " + packageName + " ne contient aucun fichier");
        }
        
        for (String file : files) {
            if (file.endsWith(".class")) {
                try {
                    String className = packageName + '.' + file.substring(0, file.length() - 6);
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    System.out.println("Erreur lors du chargement de la classe: " + file + ": " + e.getMessage());
                    // Continue with other classes instead of failing completely
                }
            }
        }
        return classes;
    }
}