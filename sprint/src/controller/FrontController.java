package controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class FrontController extends HttpServlet {

    // Méthode générique pour traiter les requêtes (peut être utilisée pour GET et POST)
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Définir le type de contenu de la réponse
        response.setContentType("text/html");
        response.getWriter().println("<html><body>");
        response.getWriter().println("<h1>Bienvenue sur la page d'accueil</h1>");
        response.getWriter().println("</body></html>");
        
    }

    // Redéfinir la méthode doGet() pour appeler processRequest()
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    // Redéfinir la méthode doPost() pour appeler processRequest()
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}
