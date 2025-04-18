package arch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Date {
    String format() default "yyyy-MM-dd";
    String message() default "Invalid date format. Expected format: {format}";
}
