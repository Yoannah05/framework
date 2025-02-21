package arch.exception;

public class DuplicateUrlMappingException extends FrameworkException {
    public DuplicateUrlMappingException(String url) {
        super("Duplicate URL mapping found: " + url);
    }
}