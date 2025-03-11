package arch.wrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class FormDataRequestWrapper extends HttpServletRequestWrapper {
    private Map<String, String[]> modifiedParameters;

    public FormDataRequestWrapper(HttpServletRequest request, Map<String, String[]> additionalParams) {
        super(request);
        
        // Copier tous les paramètres existants
        modifiedParameters = new HashMap<>(request.getParameterMap());
        
        // Ajouter ou remplacer les paramètres supplémentaires
        if (additionalParams != null) {
            modifiedParameters.putAll(additionalParams);
        }
    }

    @Override
    public String getParameter(String name) {
        String[] values = modifiedParameters.get(name);
        return (values != null && values.length > 0) ? values[0] : null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(modifiedParameters);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(modifiedParameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return modifiedParameters.get(name);
    }
}

