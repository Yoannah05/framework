package arch.servlet;

import arch.utils.FileUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@WebServlet("/viewImage")
public class ImageServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String filePath = request.getParameter("path");
        String originalName = request.getParameter("originalName");
        
        if (filePath == null || originalName == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }
        
        try {
            // Vérifier que c'est bien une image
            if (!FileUtils.isImage(originalName)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not an image file");
                return;
            }
            
            // Lire et décoder le fichier
            byte[] imageData = FileUtils.readAndDecodeFile(filePath);
            
            // Définir le type de contenu approprié
            response.setContentType(FileUtils.getImageMimeType(originalName));
            response.setContentLength(imageData.length);
            
            // Écrire l'image dans la réponse
            try (OutputStream out = response.getOutputStream()) {
                out.write(imageData);
                out.flush();
            }
            
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                              "Error processing image: " + e.getMessage());
        }
    }
}