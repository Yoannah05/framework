package arch.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class FileInfo {
    private String originalName;
    private String encodedPath;
    private String encodedFileName;
    
    public String getOriginalName() {
        return originalName;
    }
    
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
    
    public String getEncodedPath() {
        return encodedPath;
    }
    
    public void setEncodedPath(String encodedPath) {
        this.encodedPath = encodedPath;
    }
    
    public String getEncodedFileName() {
        return encodedFileName;
    }
    
    public void setEncodedFileName(String encodedFileName) {
        this.encodedFileName = encodedFileName;
    }
    
    public String getContentAsString() {
        try {
            byte[] encodedBytes = Files.readAllBytes(Paths.get(encodedPath));
            return new String(encodedBytes);
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }
    
    public byte[] getOriginalContent() {
        try {
            String base64Content = new String(Files.readAllBytes(Paths.get(encodedPath)));
            return Base64.getDecoder().decode(base64Content);
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
