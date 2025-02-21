package arch;

import arch.exception.FrameworkException;
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
import java.lang.reflect.Method;

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
            Object result = invokeHandler(mapping);
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
        return requestURL;
    }

    private Object invokeHandler(Mapping mapping) throws Exception {
        Class<?> clazz = Class.forName(mapping.getClassName());
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Method method = clazz.getDeclaredMethod(mapping.getMethodName());
        return method.invoke(instance);
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