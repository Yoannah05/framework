package arch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that require authentication and/or specific roles.
 * If no roles are specified, just authentication is required.
 * If roles are specified, both authentication and appropriate role are required.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auth {
    /**
     * List of roles that are allowed to access the annotated method.
     * An empty array means any authenticated user can access the method.
     */
    String[] roles() default {};
}