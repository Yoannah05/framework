package arch;

import arch.annotation.*;
import arch.exception.AuthorizationException;
import arch.exception.UnknownResultTypeException;
import arch.exception.ValidationException;
import arch.file.FileInfo;
import arch.handler.*;
import arch.model.ModelView;
import arch.registry.MappingRegistry;
import arch.scanner.ControllerScanner;
import arch.session.MySession;
import arch.validation.Validator;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.annotation.MultipartConfig;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import jakarta.servlet.http.Part;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.google.gson.Gson;

@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
        maxFileSize = 1024 * 1024 * 10, // 10 MB
        maxRequestSize = 1024 * 1024 * 100 // 100 MB
)
public class FrontController extends HttpServlet {
    private MappingRegistry mappingRegistry;
    private ResultHandler resultHandler;
    private ErrorHandler errorHandler;
    private Gson gson;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String controllerPackage = config.getInitParameter("controller-package");

        this.mappingRegistry = new MappingRegistry();
        this.resultHandler = new ResultHandler();
        this.errorHandler = new ErrorHandler(mappingRegistry);
        this.gson = new Gson();

        try {
            ControllerScanner scanner = new ControllerScanner(mappingRegistry, controllerPackage);
            scanner.scanControllers();
        } catch (Exception e) {
            log("Erreur lors de l'initialisation du ControllerScanner", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response, "GET");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response, "POST");
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response, String httpMethod)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try {
            if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) {
                handleFileUpload(request, response);
                return;
            }

            String requestURL = getRequestUrl(request);
            Mapping mapping = mappingRegistry.getMapping(requestURL);

            // Check if the HTTP method matches
            if (!httpMethod.equals(mapping.getVerb())) {
                Exception e = new IllegalArgumentException(
                        "L'endpoint " + requestURL + " ne supporte pas la méthode " + httpMethod);
                errorHandler.handleException(e, response, out);
                return;
            }

            Object result = invokeHandler(mapping, request);

            // Vérifier si la méthode est annotée avec @RestAPI
            Method method = getMethod(mapping);
            if (method.isAnnotationPresent(RestAPI.class)) {
                handleRestAPIResponse(result, response);
            } else {
                resultHandler.handleResult(result, request, response);
            }
        } catch (ValidationException ve) {
            // Explicitly handle ValidationException
            errorHandler.handleException(ve, response, out);
        } catch (AuthorizationException ae) {
            // Handle authorization exceptions
            errorHandler.handleException(ae, response, out);
        } catch (Exception e) {
            errorHandler.handleException(e, response, out);
        }
    }

    private Method getMethod(Mapping mapping) throws Exception {
        Class<?> clazz = Class.forName(mapping.getClassName());
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(mapping.getMethodName())) {
                return method;
            }
        }
        throw new NoSuchMethodException("Method not found: " + mapping.getMethodName());
    }

    private void handleRestAPIResponse(Object result, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if (result instanceof ModelView) {
            ModelView mv = (ModelView) result;
            Object data = mv.getData();
            out.println(gson.toJson(data));
        } else {
            out.println(gson.toJson(result));
        }
    }

    private String getRequestUrl(HttpServletRequest request) {
        String requestURL = request.getPathInfo();
        if (requestURL == null || requestURL.isEmpty()) {
            requestURL = request.getServletPath();
        }
        // Remove query parameters if present
        int queryIndex = requestURL.indexOf('?');
        if (queryIndex != -1) {
            requestURL = requestURL.substring(0, queryIndex);
        }
        return requestURL;
    }

    private void handleFileUpload(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try {
            // Récupérer le fichier uploadé
            Part filePart = request.getPart("file"); // "file" est le nom du champ dans le formulaire
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // Nom du fichier

            // Extraire le nom de l'utilisateur depuis la session (si disponible)
            String username = "default";
            if (request.getSession().getAttribute("user") != null) {
                username = request.getSession().getAttribute("user").toString();
            }

            // Chemin où stocker le fichier
            String uploadDir = System.getProperty("user.dir") + "/uploads/" + username;
            Path uploadPath = Paths.get(uploadDir);

            // Créer le dossier s'il n'existe pas
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Lire les données binaires du fichier
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream fileContent = filePart.getInputStream()) {
                byte[] data = new byte[16384]; // buffer de 16 KB
                int bytesRead;
                while ((bytesRead = fileContent.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, bytesRead);
                }
            }
            byte[] fileBytes = buffer.toByteArray();

            // Convertir les données binaires en Base64 pour stockage texte
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);

            // Écrire le contenu encodé dans un fichier texte
            String txtFileName = fileName + ".txt";
            Path filePath = uploadPath.resolve(txtFileName);

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(base64Content.getBytes());
            }

            // Réponse de succès
            out.println("<h1>File uploaded successfully: " + txtFileName + "</h1>");

            // Ajouter les infos à la requête pour être accessibles dans le contrôleur
            request.setAttribute("uploadedFilePath", filePath.toString());
            request.setAttribute("originalFileName", fileName);
            request.setAttribute("encodedFileName", txtFileName);

            // Continuer le traitement avec l'URL demandée
            String requestURL = getRequestUrl(request);
            if (requestURL != null) {
                Mapping mapping = mappingRegistry.getMapping(requestURL);
                Object result = invokeHandler(mapping, request);
                resultHandler.handleResult(result, request, response);
            }
        } catch (Exception e) {
            // Gestion des erreurs
            out.println("<h1>Error uploading file: " + e.getMessage() + "</h1>");
            e.printStackTrace(out);
        }
    }

    private Object invokeHandler(Mapping mapping, HttpServletRequest request) throws Exception {
        try {
            Class<?> clazz = Class.forName(mapping.getClassName());
            Object instance = clazz.getDeclaredConstructor().newInstance();
    
            Method methodToFind = null;
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(mapping.getMethodName())) {
                    methodToFind = m;
                    break;
                }
            }
    
            if (methodToFind == null) {
                throw new NoSuchMethodException(
                        "Méthode " + mapping.getMethodName() + " non trouvée dans " + mapping.getClassName());
            }
            
            // Check for @Auth annotation at both class and method levels
            boolean requiresAuth = false;
            String[] requiredRoles = new String[0];
            
            // First check class-level annotation
            if (clazz.isAnnotationPresent(Auth.class)) {
                requiresAuth = true;
                Auth authAnnotation = clazz.getAnnotation(Auth.class);
                requiredRoles = authAnnotation.roles();
            }
            
            // Method-level annotation overrides class-level if present
            if (methodToFind.isAnnotationPresent(Auth.class)) {
                requiresAuth = true;
                Auth authAnnotation = methodToFind.getAnnotation(Auth.class);
                requiredRoles = authAnnotation.roles(); // Override class-level roles
            }
            
            // Perform authorization check if required
            if (requiresAuth) {
                // Get authentication status from session
                Boolean isAuthenticated = (Boolean) request.getSession().getAttribute("auth");
                
                // Check if user is authenticated
                if (isAuthenticated == null || !isAuthenticated) {
                    throw new AuthorizationException("L'utilisation de cette méthode nécessite une autorisation");
                }
                
                // If roles are specified, check user's role
                if (requiredRoles.length > 0) {
                    String userRole = (String) request.getSession().getAttribute("role");
                    
                    // Check if user's role matches any of the required roles
                    boolean hasPermission = false;
                    for (String role : requiredRoles) {
                        if (role.equals(userRole)) {
                            hasPermission = true;
                            break;
                        }
                    }
                    
                    if (!hasPermission) {
                        throw new AuthorizationException("Rôle non autorisé pour l'utilisation de cette méthode");
                    }
                }
            }
    
            Parameter[] parameters = methodToFind.getParameters();
            Object[] args = new Object[parameters.length];
    
            // ... (rest of the method remains the same)
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                Class<?> paramType = param.getType();
    
                try {
                    if (paramType == MySession.class) {
                        // Gestion de la session
                        args[i] = new MySession(request.getSession());
                    } else if (param.isAnnotationPresent(Param.class)) {
                        // Paramètre simple avec @Param
                        Param annotation = param.getAnnotation(Param.class);
                        String paramName = annotation.name();
    
                        // Spécial pour le fichier uploadé
                        if (paramName.equals("file") && request.getAttribute("uploadedFilePath") != null) {
                            // Créer un FileInfo contenant les infos du fichier
                            FileInfo fileInfo = new FileInfo();
                            fileInfo.setOriginalName(request.getAttribute("originalFileName").toString());
                            fileInfo.setEncodedPath(request.getAttribute("uploadedFilePath").toString());
                            fileInfo.setEncodedFileName(request.getAttribute("encodedFileName").toString());
                            args[i] = fileInfo;
                        } else {
                            String paramValue = request.getParameter(paramName);
                            args[i] = convertParameterValue(paramValue, paramType);
                        }
                    } else if (param.isAnnotationPresent(RequestParam.class)) {
                        // Objet complexe avec @RequestParam
                        args[i] = bindRequestParameters(paramType, request);
                    } else {
                        // Paramètre sans annotation
                        String paramName = param.getName();
                        String paramValue = request.getParameter(paramName);
                        args[i] = convertParameterValue(paramValue, paramType);
                    }
                } catch (ValidationException e) {
                    // Do not catch ValidationException, let it propagate up
                    throw e;
                }
            }
    
            return methodToFind.invoke(instance, args);
        } catch (ValidationException e) {
            // Don't transform ValidationException, let it propagate up
            throw e;
        } catch (AuthorizationException e) {
            // Don't transform AuthorizationException either
            throw e;
        } catch (Exception e) {
            throw new Exception("Erreur lors de l'invocation du handler: " + e.getMessage(), e);
        }
    }

    private Object bindRequestParameters(Class<?> paramType, HttpServletRequest request) throws Exception {
        Object instance = paramType.getDeclaredConstructor().newInstance();
        List<String> validationErrors = new ArrayList<>(); // Liste pour accumuler les erreurs

        for (Field field : paramType.getDeclaredFields()) {
            field.setAccessible(true);

            String paramName;
            if (field.isAnnotationPresent(FormField.class)) {
                FormField annotation = field.getAnnotation(FormField.class);
                paramName = !annotation.name().isEmpty() ? annotation.name() : field.getName();
            } else {
                paramName = field.getName();
            }

            if (field.getType() == FileInfo.class && request.getAttribute("uploadedFilePath") != null) {
                // Gestion des fichiers uploadés avec le nouveau FileInfo
                FileInfo fileInfo = new FileInfo();
                fileInfo.setOriginalName(request.getAttribute("originalFileName").toString());
                fileInfo.setEncodedPath(request.getAttribute("uploadedFilePath").toString());
                fileInfo.setEncodedFileName(request.getAttribute("encodedFileName").toString());
                field.set(instance, fileInfo);
            } else {
                String paramValue = request.getParameter(paramName);
                if (paramValue != null && !paramValue.isEmpty()) {
                    try {
                        Object convertedValue = convertParameterValue(paramValue, field.getType());
                        field.set(instance, convertedValue);
                    } catch (ValidationException e) {
                        // Ajouter l'erreur de validation à la liste
                        validationErrors.addAll(e.getErrors());
                    }
                }
            }
        }

        // Valider l'objet après avoir rempli tous les champs
        try {
            Validator.validate(instance);
        } catch (ValidationException e) {
            // Ajouter les erreurs de validation à la liste
            validationErrors.addAll(e.getErrors());
        }

        // Si des erreurs de validation ont été détectées, lever une exception
        if (!validationErrors.isEmpty()) {
            throw new ValidationException(paramType.getSimpleName(), validationErrors);
        }

        return instance;
    }

    private Object convertParameterValue(String value, Class<?> type) {
        if (value == null)
            return null;

        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        }

        throw new UnknownResultTypeException("Type de paramètre non supporté: " + type.getName());
    }
}