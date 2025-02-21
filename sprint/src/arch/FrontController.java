package arch;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import arch.annotation.Controller;
import arch.annotation.GET;
import arch.model.ModelView;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public class FrontController extends HttpServlet {

    private Map<String, Mapping> urlMappings = new HashMap<>();
    private String controllerPackage;
    private static final String VIEW_PREFIX = "/WEB-INF/views/";
    private static final String VIEW_SUFFIX = ".jsp";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        controllerPackage = config.getInitParameter("controller-package");
        scanControllers();
    }

    private void scanControllers() {
        try {
            List<Class<?>> controllerClasses = getClasses(controllerPackage);

            for (Class<?> clazz : controllerClasses) {
                if (clazz.isAnnotationPresent(Controller.class)) {
                    for (Method method : clazz.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(GET.class)) {
                            GET getAnnotation = method.getAnnotation(GET.class);
                            String url = getAnnotation.value();
                            Mapping mapping = new Mapping(clazz.getName(), method.getName());
                            urlMappings.put(url, mapping);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    private List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader classLoader = getClass().getClassLoader();
        String path = packageName.replace('.', '/');
        URL packageUrl = classLoader.getResource(path);

        if (packageUrl != null) {
            File directory = new File(packageUrl.getFile());
            if (directory.exists()) {
                String[] files = directory.list();
                for (String file : files) {
                    if (file.endsWith(".class")) {
                        String className = packageName + '.' + file.substring(0, file.length() - 6);
                        classes.add(Class.forName(className));
                    }
                }
            }
        } else {
            throw new IOException("Package not found: " + packageName);
        }
        return classes;
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

        String requestURL = request.getPathInfo();

        if (requestURL == null || requestURL.equals("") || requestURL.equals("")) {
            requestURL=request.getServletPath();
        }

        out.println("Request URL: " + requestURL); // Debug: print the request URL

        Mapping mapping = urlMappings.get(requestURL);
        if (mapping == null) {
            out.println("No method associated with URL: " + requestURL);
            out.println("Available mappings: " + urlMappings.keySet()); // Debug: print available mappings
            return;
        }

        try {
            String className = mapping.getClassName();
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            String methodName = mapping.getMethodName();
            Method method = clazz.getDeclaredMethod(methodName);

            Object result = method.invoke(instance);

            if (result instanceof String) {
                out.println("Result: " + result);
            } else if (result instanceof ModelView) {
                ModelView modelView = (ModelView) result;
                String url = modelView.getUrl();
                out.println("ModelView URL: " + url); // Debug: print the ModelView URL

                // Ajouter chaque entrée du HashMap en tant que paramètre de la requête
                for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }

                request.getRequestDispatcher(url).forward(request, response);
            } else {
                out.println("Unknown result type: " + result.getClass().getName());
            }
        } catch (ClassNotFoundException e) {
            out.println("Class not found: " + mapping.getClassName());
            e.printStackTrace(out); // Debug: print stack trace
        } catch (NoSuchMethodException e) {
            out.println("Method not found: " + mapping.getMethodName());
            e.printStackTrace(out); // Debug: print stack trace
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            out.println("Error invoking method: " + e.getMessage());
            e.printStackTrace(out); // Debug: print stack trace
        } finally {
            out.close();
        }
    }

    private String resolveViewPath(String viewName) {
        if (!viewName.endsWith(".jsp")) {
            viewName += ".jsp";
        }
       
        return VIEW_PREFIX + viewName;
    }
}
