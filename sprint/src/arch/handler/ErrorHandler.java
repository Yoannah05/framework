package arch.handler;

import arch.Mapping;
import arch.exception.FrameworkException;
import arch.exception.PackageNotFoundException;
import arch.exception.UrlMappingNotFoundException;
import arch.exception.ValidationException;
import arch.model.ModelView;
import arch.exception.UnknownResultTypeException;
import arch.registry.MappingRegistry;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe qui gère les erreurs du framework et renvoie des réponses HTTP
 * appropriées.
 */
public class ErrorHandler {

    private final MappingRegistry mappingRegistry;
    private Map<Class<? extends Exception>, Integer> exceptionStatusMap;

    /**
     * Constructeur avec un registre de mappings.
     * 
     * @param mappingRegistry Le registre de mappings pour afficher les URLs
     *                        disponibles
     */
    public ErrorHandler(MappingRegistry mappingRegistry) {
        this.mappingRegistry = mappingRegistry;
        initExceptionStatusMap();
    }

    /**
     * Initialise la map des statuts HTTP par type d'exception.
     */
    private void initExceptionStatusMap() {
        exceptionStatusMap = new HashMap<>();

        // Exceptions 400 (Bad Request)
        exceptionStatusMap.put(UrlMappingNotFoundException.class, HttpServletResponse.SC_NOT_FOUND);
        exceptionStatusMap.put(UnknownResultTypeException.class, HttpServletResponse.SC_BAD_REQUEST);
        exceptionStatusMap.put(PackageNotFoundException.class, HttpServletResponse.SC_BAD_REQUEST);

        // Par défaut, les erreurs framework sont 400
        exceptionStatusMap.put(FrameworkException.class, HttpServletResponse.SC_BAD_REQUEST);

        // Autres exceptions sont 500 (par défaut)
    }

    /**
     * Gère n'importe quelle exception et envoie une réponse HTTP appropriée.
     * 
     * @param e        L'exception à gérer
     * @param response La réponse HTTP
     * @param out      Le writer pour écrire la réponse
     */
    public void handleException(Exception e, HttpServletResponse response, PrintWriter out) {
        int statusCode = determineStatusCode(e);
        response.setStatus(statusCode);
        if (e instanceof ValidationException) {
            handleValidationException(e, out);
        }
        if (statusCode == HttpServletResponse.SC_BAD_REQUEST) {
            handleBadRequestException(e, out);
        } else if (statusCode == HttpServletResponse.SC_NOT_FOUND) {
            handleNotFoundException(e, out);
        } else {
            handleInternalServerError(e, out);
        }
    }

    // public void handleValidationException(ValidationException ve,
    // HttpServletRequest request,
    // HttpServletResponse response)
    // throws ServletException, IOException {
    // // Récupérer l'URL de la dernière requête (form)
    // String previousUrl = request.getHeader("Referer");
    // if (previousUrl == null) {
    // previousUrl = "/"; // URL par défaut si aucune URL précédente n'est trouvée
    // } else {
    // // Extraire uniquement le chemin de l'URL (URI)
    // try {
    // URI uri = new URI(previousUrl);
    // String fullPath = uri.getPath();

    // // Remove the context path
    // String contextPath = request.getContextPath();
    // if (fullPath.startsWith(contextPath)) {
    // previousUrl = fullPath.substring(contextPath.length());
    // } else {
    // previousUrl = fullPath;
    // }
    // } catch (URISyntaxException e) {
    // // En cas d'erreur de syntaxe dans l'URL, utiliser l'URL par défaut
    // previousUrl = "/";
    // }
    // }

    // // Créer un ModelView pour retourner aux données du formulaire
    // ModelView modelView = new ModelView(previousUrl);

    // // Ajouter les erreurs de validation au ModelView
    // for (String error : ve.getErrors()) {
    // String[] parts = error.split(": ", 2);
    // if (parts.length == 2) {
    // // Récupérer la valeur du paramètre qui a causé l'erreur
    // String fieldName = parts[0];
    // String errorMessage = parts[1];
    // String paramValue = request.getParameter(fieldName);

    // // Ajouter les données du formulaire avec l'erreur
    // modelView.addFormData(fieldName, paramValue, errorMessage);
    // }
    // }

    // // Rediriger avec les données du formulaire
    // request.setAttribute("modelView", modelView);
    // request.getRequestDispatcher(modelView.getUrl()).forward(request, response);
    // }

    public void handleValidationException(ValidationException ve, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        // Récupérer l'URL précédente
        String previousUrl = request.getHeader("Referer");
        if (previousUrl == null) {
            previousUrl = "/";
        } else {
            try {
                URI uri = new URI(previousUrl);
                String fullPath = uri.getPath();

                String contextPath = request.getContextPath();
                if (fullPath.startsWith(contextPath)) {
                    previousUrl = fullPath.substring(contextPath.length());
                } else {
                    previousUrl = fullPath;
                }
            } catch (URISyntaxException e) {
                previousUrl = "/";
            }
        }

        // Créer le ModelView avec l'URL précédente
        ModelView modelView = new ModelView(previousUrl);

        // D'abord, ajouter tous les paramètres de la requête originale
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().length > 0 ? entry.getValue()[0] : "";
            modelView.addFormData(key, value);
        }

        // Ensuite, traiter chaque erreur de validation et ajouter les messages d'erreur
        for (String error : ve.getErrors()) {
            String[] parts = error.split(": ", 2);
            if (parts.length == 2) {
                String fieldName = parts[0];
                String errorMessage = parts[1];

                // Récupérer la FieldData existante et y ajouter l'erreur
                if (modelView.getFormData().containsKey(fieldName)) {
                    ModelView.FieldData fieldData = modelView.getFormData(fieldName);
                    fieldData.setError(errorMessage);
                } else {
                    // Au cas où le champ n'existe pas dans les paramètres
                    String paramValue = request.getParameter(fieldName);
                    modelView.addFormData(fieldName, paramValue, errorMessage);
                }
            }
        }

        // Stocker le ModelView dans les attributs de la requête
        request.setAttribute("modelView", modelView);

        // Récupérer le mapping pour l'URL d'erreur
        Mapping errorMapping = mappingRegistry.getMapping(modelView.getUrl());

        // Récupérer le verbe HTTP attendu pour l'URL d'erreur
        String errorHttpMethod = errorMapping != null ? errorMapping.getVerb() : "GET";

        // Récupérer le verbe HTTP de la requête d'origine
        String httpMethod = request.getMethod();

        // Vérifier si le verbe HTTP de la requête correspond à celui attendu par l'URL
        // d'erreur
        if (!httpMethod.equalsIgnoreCase(errorHttpMethod)) {
            // Créer un HttpServletRequestWrapper pour forcer la méthode HTTP
            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getMethod() {
                    return errorHttpMethod; // Utiliser le verbe HTTP de l'errorUrl
                }
            };

            // Forward vers l'URL d'erreur avec le bon verbe HTTP
            RequestDispatcher dispatcher = wrappedRequest.getRequestDispatcher(modelView.getUrl());
            dispatcher.forward(wrappedRequest, response);
        } else {
            // Si la méthode HTTP correspond déjà, effectuer un forward directement
            RequestDispatcher dispatcher = request.getRequestDispatcher(modelView.getUrl());
            dispatcher.forward(request, response);
        }
    }

    public void handleValidationException(Exception e, PrintWriter out) {
        ValidationException ve = (ValidationException) e;
        out.println("<h1>Validation Error</h1>");
        out.println("<h2>Form: " + ve.getFormName() + "</h2>");
        out.println("<ul>");
        for (String error : ve.getErrors()) {
            out.println("<li>" + error + "</li>");
        }
        out.println("</ul>");
    }

    /**
     * Détermine le code de statut HTTP en fonction du type d'exception.
     * 
     * @param e L'exception
     * @return Le code de statut HTTP
     */
    private int determineStatusCode(Exception e) {
        // Chercher d'abord la classe exacte
        Integer statusCode = exceptionStatusMap.get(e.getClass());

        // Si pas trouvé, chercher les classes parentes
        if (statusCode == null) {
            for (Class<? extends Exception> exceptionClass : exceptionStatusMap.keySet()) {
                if (exceptionClass.isAssignableFrom(e.getClass())) {
                    return exceptionStatusMap.get(exceptionClass);
                }
            }
        } else {
            return statusCode;
        }

        // Par défaut: 500 Internal Server Error
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    /**
     * Gère les exceptions de type 400 Bad Request.
     * 
     * @param e   L'exception
     * @param out Le writer pour écrire la réponse
     */
    private void handleBadRequestException(Exception e, PrintWriter out) {
        out.println("<html>");
        out.println("<head><title>Erreur 400 - Requête incorrecte</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }");
        out.println(".error-container { border: 1px solid #e74c3c; padding: 20px; border-radius: 5px; }");
        out.println("h1 { color: #e74c3c; }");
        out.println("h2 { color: #333; font-size: 18px; margin-top: 20px; }");
        out.println("pre { background-color: #f8f8f8; padding: 10px; border-radius: 4px; overflow-x: auto; }");
        out.println("</style></head>");
        out.println("<body>");
        out.println("<div class='error-container'>");
        out.println("<h1>Erreur 400 : Requête incorrecte</h1>");
        out.println("<p>" + e.getMessage() + "</p>");

        if (e instanceof UrlMappingNotFoundException && mappingRegistry != null) {
            out.println("<h2>URLs disponibles :</h2>");
            out.println("<ul>");
            for (String url : mappingRegistry.getRegisteredUrls()) {
                out.println("<li>" + url + "</li>");
            }
            out.println("</ul>");
        }

        out.println("</div>");
        out.println("</body></html>");
    }

    /**
     * Gère les exceptions de type 404 Not Found.
     * 
     * @param e   L'exception
     * @param out Le writer pour écrire la réponse
     */
    private void handleNotFoundException(Exception e, PrintWriter out) {
        out.println("<html>");
        out.println("<head><title>Erreur 404 - Page non trouvée</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }");
        out.println(".error-container { border: 1px solid #3498db; padding: 20px; border-radius: 5px; }");
        out.println("h1 { color: #3498db; }");
        out.println("h2 { color: #333; font-size: 18px; margin-top: 20px; }");
        out.println("</style></head>");
        out.println("<body>");
        out.println("<div class='error-container'>");
        out.println("<h1>Erreur 404 : Page non trouvée</h1>");
        out.println("<p>" + e.getMessage() + "</p>");

        if (mappingRegistry != null) {
            out.println("<h2>URLs disponibles :</h2>");
            out.println("<ul>");
            for (String url : mappingRegistry.getRegisteredUrls()) {
                out.println("<li>" + url + "</li>");
            }
            out.println("</ul>");
        }

        out.println("</div>");
        out.println("</body></html>");
    }

    /**
     * Gère les exceptions de type 500 Internal Server Error.
     * 
     * @param e   L'exception
     * @param out Le writer pour écrire la réponse
     */
    private void handleInternalServerError(Exception e, PrintWriter out) {
        out.println("<html>");
        out.println("<head><title>Erreur 500 - Erreur interne du serveur</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }");
        out.println(".error-container { border: 1px solid #e74c3c; padding: 20px; border-radius: 5px; }");
        out.println("h1 { color: #e74c3c; }");
        out.println("h2 { color: #333; font-size: 18px; margin-top: 20px; }");
        out.println("pre { background-color: #f8f8f8; padding: 10px; border-radius: 4px; overflow-x: auto; }");
        out.println("</style></head>");
        out.println("<body>");
        out.println("<div class='error-container'>");
        out.println("<h1>Erreur 500 : Erreur interne du serveur</h1>");
        out.println("<p>Une erreur inattendue s'est produite lors du traitement de votre requête.</p>");
        out.println("<p>Message d'erreur : " + e.getMessage() + "</p>");

        out.println("<h2>Détails techniques :</h2>");
        out.println("<pre>");
        e.printStackTrace(out);
        out.println("</pre>");

        out.println("</div>");
        out.println("</body></html>");
    }

    /**
     * Ajoute un mapping personnalisé entre un type d'exception et un code de statut
     * HTTP.
     * 
     * @param exceptionClass La classe d'exception
     * @param statusCode     Le code de statut HTTP
     */
    public void addExceptionMapping(Class<? extends Exception> exceptionClass, int statusCode) {
        exceptionStatusMap.put(exceptionClass, statusCode);
    }
}