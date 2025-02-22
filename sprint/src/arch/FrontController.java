package arch;

import arch.annotation.FormField;
import arch.annotation.Param;
import arch.annotation.RequestParam;
import arch.exception.FrameworkException;
import arch.exception.UnknownResultTypeException;
import arch.exception.UrlMappingNotFoundException;
import arch.handler.ResultHandler;
import arch.registry.MappingRegistry;
import arch.scanner.ControllerScanner;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class FrontController extends HttpServlet {
    private MappingRegistry mappingRegistry;
    private ResultHandler resultHandler;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String controllerPackage = config.getInitParameter("controller-package");

        this.mappingRegistry = new MappingRegistry();
        this.resultHandler = new ResultHandler();

        ControllerScanner scanner = new ControllerScanner(mappingRegistry, controllerPackage);
        scanner.scanControllers();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try {
            String requestURL = getRequestUrl(request);
            Mapping mapping = mappingRegistry.getMapping(requestURL);
            Object result = invokeHandler(mapping, request);
            resultHandler.handleResult(result, request, response);
        } catch (FrameworkException e) {
            handleFrameworkException(e, out);
        } catch (Exception e) {
            handleUnexpectedException(e, out);
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

    // FrontController.java - Méthode invokeHandler modifiée
    private Object invokeHandler(Mapping mapping, HttpServletRequest request) throws Exception {
        try {
            // Chargement de la classe
            Class<?> clazz = Class.forName(mapping.getClassName());
            Object instance = clazz.getDeclaredConstructor().newInstance();

            // Récupération de la méthode avec ses paramètres
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

            // Récupération des paramètres de la méthode
            Parameter[] parameters = methodToFind.getParameters();
            Object[] args = new Object[parameters.length];

            // Traitement des paramètres
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                if (param.isAnnotationPresent(Param.class)) {
                    // Paramètre simple avec @Param
                    Param annotation = param.getAnnotation(Param.class);
                    String paramName = annotation.name();
                    String paramValue = request.getParameter(paramName);
                    args[i] = convertParameterValue(paramValue, param.getType());
                } else if (param.isAnnotationPresent(RequestParam.class)) {
                    // Objet complexe avec @RequestParam
                    args[i] = bindRequestParameters(param.getType(), request);
                } else {
                    // Si pas d'annotation, utiliser le nom du paramètre directement
                    String paramName = param.getName();
                    String paramValue = request.getParameter(paramName);
                    args[i] = convertParameterValue(paramValue, param.getType());
                }
            }

            // Invocation de la méthode avec les arguments
            return methodToFind.invoke(instance, args);

        } catch (NoSuchMethodException e) {
            throw new Exception("Méthode introuvable : " + mapping.getMethodName(), e);
        } catch (InvocationTargetException e) {
            throw new Exception("Erreur lors de l'exécution de la méthode : " + mapping.getMethodName(), e);
        } catch (Exception e) {
            throw new Exception("Erreur lors de l'invocation du handler pour " + mapping.getClassName(), e);
        }
    }

    private Object bindRequestParameters(Class<?> paramType, HttpServletRequest request) throws Exception {
        Object instance = paramType.getDeclaredConstructor().newInstance();

        for (Field field : paramType.getDeclaredFields()) {
            field.setAccessible(true);

            String paramName;
            if (field.isAnnotationPresent(FormField.class)) {
                FormField annotation = field.getAnnotation(FormField.class);
                paramName = !annotation.name().isEmpty() ? annotation.name() : field.getName();
            } else {
                paramName = field.getName();
            }

            String paramValue = request.getParameter(paramName);
            if (paramValue != null && !paramValue.isEmpty()) {
                Object convertedValue = convertParameterValue(paramValue, field.getType());
                field.set(instance, convertedValue);
            }
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

        throw new UnknownResultTypeException("Unsupported parameter type: " + type.getName());
    }

    private void handleFrameworkException(FrameworkException e, PrintWriter out) {
        out.println("Framework Error: " + e.getMessage());
        if (e instanceof UrlMappingNotFoundException) {
            out.println("Available mappings: " + mappingRegistry.getRegisteredUrls());
        }
    }

    private void handleUnexpectedException(Exception e, PrintWriter out) {
        out.println("Internal Server Error: " + e.getMessage());
        e.printStackTrace(out);
    }
}