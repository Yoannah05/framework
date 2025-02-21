package arch.handler;

import arch.exception.UnknownResultTypeException;
import arch.model.ModelView;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class ResultHandler {
    private static final String VIEW_PREFIX = "views/";
    private static final String VIEW_SUFFIX = ".jsp";

    public void handleResult(Object result, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();

        try {
            if (result instanceof String) {
                handleStringResult((String) result, out);
            } else if (result instanceof ModelView) {
                handleModelViewResult((ModelView) result, request, response);
            } else {
                throw new UnknownResultTypeException(result.getClass().getName());
            }
        } finally {
            out.close();
        }
    }

    private void handleStringResult(String result, PrintWriter out) {
        out.println("Result: " + result);
    }

    private void handleModelViewResult(ModelView modelView, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String url = resolveViewPath(modelView.getUrl());

        for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
        }

        request.getRequestDispatcher(url).forward(request, response);
    }

    private String resolveViewPath(String viewName) {
        return VIEW_PREFIX + viewName + VIEW_SUFFIX;
    }
}
