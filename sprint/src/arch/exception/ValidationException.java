package arch.exception;

import java.util.ArrayList;
import java.util.List;

public class ValidationException extends RuntimeException {
    private List<String> errors = new ArrayList<>();
    private String formName;

    public ValidationException(String formName) {
        super("Validation failed for form: " + formName);
        this.formName = formName;
    }

    public ValidationException(String formName, List<String> errors) {
        super("Validation failed for form: " + formName);
        this.formName = formName;
        this.errors = errors;
    }

    public void addError(String fieldName, String message) {
        errors.add(fieldName + ": " + message);
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getFormName() {
        return formName;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder(super.getMessage());
        message.append("\nValidation errors:");
        for (String error : errors) {
            message.append("\n- ").append(error);
        }
        return message.toString();
    }
}