package arch.exception;

public class UnknownResultTypeException extends FrameworkException {
    public UnknownResultTypeException(String className) {
        super("Unknown result type: " + className);
    }
}