package arch.exception;

/**
 * Exception thrown when authentication or authorization requirements are not met.
 */
public class AuthorizationException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public AuthorizationException(String message) {
        super(message);
    }
}