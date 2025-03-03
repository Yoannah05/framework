package arch.exception;

public class PackageNotFoundException extends FrameworkException {
    public PackageNotFoundException(String packageName) {
        super("Package not found: " + packageName);
    }

    public PackageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}