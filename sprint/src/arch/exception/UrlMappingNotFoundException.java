package arch.exception;

public class UrlMappingNotFoundException extends FrameworkException {
    public UrlMappingNotFoundException(String url) {
        super("No method associated with URL: " + url);
    }
}