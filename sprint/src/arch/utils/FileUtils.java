package arch.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class FileUtils {
    
    /**
     * Détermine si un fichier est une image basé sur son nom original
     */
    public static boolean isImage(String originalFileName) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            return false;
        }
        
        String lowerFileName = originalFileName.toLowerCase();
        return lowerFileName.endsWith(".jpg") || 
               lowerFileName.endsWith(".jpeg") || 
               lowerFileName.endsWith(".png") || 
               lowerFileName.endsWith(".gif") || 
               lowerFileName.endsWith(".bmp") ||
               lowerFileName.endsWith(".webp");
    }
    
    /**
     * Détermine le type MIME d'une image basé sur son extension
     */
    public static String getImageMimeType(String originalFileName) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            return "application/octet-stream";
        }
        
        String lowerFileName = originalFileName.toLowerCase();
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFileName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerFileName.endsWith(".webp")) {
            return "image/webp";
        }
        
        return "application/octet-stream";
    }
    
    /**
     * Lit et décode le contenu d'un fichier Base64 en bytes originaux
     */
    public static byte[] readAndDecodeFile(String filePath) throws IOException {
        String base64Content = new String(Files.readAllBytes(Paths.get(filePath)));
        return Base64.getDecoder().decode(base64Content);
    }
    
    /**
     * Génère une balise img HTML avec les données en Base64 inline
     */
    public static String generateBase64ImageTag(String filePath, String originalFileName) {
        try {
            String base64Content = new String(Files.readAllBytes(Paths.get(filePath)));
            String mimeType = getImageMimeType(originalFileName);
            return "<img src=\"data:" + mimeType + ";base64," + base64Content + "\" alt=\"" + originalFileName + "\" class=\"preview-image\">";
        } catch (IOException e) {
            return "<p class=\"error\">Impossible de générer l'aperçu de l'image: " + e.getMessage() + "</p>";
        }
    }
}