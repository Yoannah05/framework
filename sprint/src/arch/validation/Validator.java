package arch.validation;

import arch.annotation.*;
import arch.exception.ValidationException;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Validator {
    
    public static void validate(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("Cannot validate null object");
        }
        
        Class<?> clazz = object.getClass();
        ValidationException validationException = new ValidationException(clazz.getSimpleName());
        
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            validateField(object, field, validationException);
        }
        
        if (validationException.hasErrors()) {
            throw validationException;
        }
    }
    
    private static void validateField(Object object, Field field, ValidationException validationException) {
        try {
            Object value = field.get(object);
            
            // Check Required annotation
            if (field.isAnnotationPresent(Required.class)) {
                Required required = field.getAnnotation(Required.class);
                if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                    validationException.addError(field.getName(), required.message());
                }
            }
            
            // If value is null and not required, skip other validations
            if (value == null) {
                // Check NotNull annotation
                if (field.isAnnotationPresent(NotNull.class)) {
                    NotNull notNull = field.getAnnotation(NotNull.class);
                    validationException.addError(field.getName(), notNull.message());
                }
                return;
            }
            
            // Check Text annotation
            // if (field.isAnnotationPresent(Text.class) && value instanceof String) {
            //     Text text = field.getAnnotation(Text.class);
            //     String stringValue = (String) value;
            //     if (stringValue.length() < text.minLength() || stringValue.length() > text.maxLength()) {
            //         String message = text.message()
            //             .replace("{minLength}", String.valueOf(text.minLength()))
            //             .replace("{maxLength}", String.valueOf(text.maxLength()));
            //         validationException.addError(field.getName(), message);
            //     }
            // }
            
            // Check Date annotation
            if (field.isAnnotationPresent(Date.class) && value instanceof String) {
                Date dateAnnot = field.getAnnotation(Date.class);
                String stringValue = (String) value;
                SimpleDateFormat dateFormat = new SimpleDateFormat(dateAnnot.format());
                dateFormat.setLenient(false);
                try {
                    dateFormat.parse(stringValue);
                } catch (ParseException e) {
                    String message = dateAnnot.message()
                        .replace("{format}", dateAnnot.format());
                    validationException.addError(field.getName(), message);
                }
            }
            
            // Check Numeric annotation
            if (field.isAnnotationPresent(Numeric.class)) {
                Numeric numeric = field.getAnnotation(Numeric.class);
                double numericValue;
                
                if (value instanceof Number) {
                    numericValue = ((Number) value).doubleValue();
                } else if (value instanceof String) {
                    try {
                        numericValue = Double.parseDouble((String) value);
                    } catch (NumberFormatException e) {
                        validationException.addError(field.getName(), "Value must be a number");
                        return;
                    }
                } else {
                    validationException.addError(field.getName(), "Value must be a number");
                    return;
                }
                
                if (numericValue < numeric.min() || numericValue > numeric.max()) {
                    String message = numeric.message()
                        .replace("{min}", String.valueOf(numeric.min()))
                        .replace("{max}", String.valueOf(numeric.max()));
                    validationException.addError(field.getName(), message);
                }
            }
            
        } catch (IllegalAccessException e) {
            validationException.addError(field.getName(), "Could not access field value: " + e.getMessage());
        }
    }
}