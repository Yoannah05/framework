package arch;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import arch.annotation.Controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FrontController extends HttpServlet {

    private boolean controllersScanned = false;
    private List<String> controllerList = new ArrayList<>();
    private String controllerPackage;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        controllerPackage = config.getInitParameter("controller-package");
    }

    private List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
        List<Class<?>> classes = new ArrayList<>();

        // Use ClassLoader to obtain the classes
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
        response.getWriter().println("<h1>List of Scanned Controllers:</h1>");

        // If controllers haven't been scanned yet, do so
        if (!controllersScanned) {
            try {
                controllerList.clear(); // Clear the previous list
                List<Class<?>> controllerClasses = getClasses(controllerPackage);

                for (Class<?> clazz : controllerClasses) {
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        String controllerName = clazz.getName();
                        controllerList.add(controllerName);
                        System.out.println("Controller found: " + controllerName);
                    }
                }

                // Mark controllers as scanned to avoid re-scanning
                controllersScanned = true;

                // Display the list of scanned controllers
                System.out.println("Scanned controllers:");
                for (String controller : controllerList) {
                    System.out.println(controller);
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }

        // Display controllers in the response body
        if (!controllerList.isEmpty()) {
            response.getWriter().println("<ul>");
            for (String controller : controllerList) {
                response.getWriter().println("<li>" + controller + "</li>");
            }
            response.getWriter().println("</ul>");
        } else {
            response.getWriter().println("<p>No controllers found.</p>");
        }

        response.getWriter().println("</body></html>");
    }
}
