package arch;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import arch.annotation.Controller;
import arch.annotation.GET;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public class FrontController extends HttpServlet {

    private Map<String, Mapping> urlMappings = new HashMap<>();  
    private String controllerPackage;
    private int test = 0;

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
                        test++;
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.getWriter().println("<html><body>");
        response.getWriter().println("<h1>URL Mapping:</h1>");

        String requestUrl = request.getPathInfo();
        Mapping mapping = urlMappings.get(requestUrl); 

        if (mapping != null) {
            try {
                Class<?> controllerClass = Class.forName(mapping.getClassName());
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                Method method = controllerClass.getDeclaredMethod(mapping.getMethodName());
                if (method.getReturnType() != String.class) {
                    throw new ServletException("La méthode doit retourner un String");
                }
                String result = (String) method.invoke(controllerInstance);
                response.getWriter().println("<p>URL: " + requestUrl + "</p>");
                response.getWriter().println("<p>Mapped to: " + mapping + "</p>");
                response.getWriter().println("<p>Method result: " + result + "</p>");
                
            } catch (ClassNotFoundException e) {
                response.getWriter().println("<p>Error: Controller class not found</p>");
                e.printStackTrace();
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                response.getWriter().println("<p>Error: Could not execute controller method</p>");
                e.printStackTrace();
            }
        } else {
            response.getWriter().println("<p>No method associated with this URL: " + requestUrl + "</p>");
        }
        
        response.getWriter().println("</body></html>");
    }
}