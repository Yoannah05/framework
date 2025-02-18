package arch;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import arch.annotation.Controller;
import arch.annotation.GET;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
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

    // Méthode pour scanner les contrôleurs et associer les méthodes GET à des URL
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

    // Méthode pour obtenir les classes dans un package donné
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

    // Méthode pour traiter la requête et afficher le Mapping associé à l'URL
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.getWriter().println("<html><body>");
        response.getWriter().println("<h1>URL Mapping:</h1>");

        String requestUrl = request.getPathInfo();

        Mapping mapping = urlMappings.get(requestUrl); 

        if (mapping != null) {
            response.getWriter().println("<p>URL: " + requestUrl + "</p>");
            response.getWriter().println("<p>Mapped to: " + mapping + "</p>");
        } else {
            response.getWriter().println("<p>No method associated with this URL : </p>" + requestUrl);
        }
        response.getWriter().println("<p>"+test+"</p>");

        response.getWriter().println("</body></html>");
    }
}
