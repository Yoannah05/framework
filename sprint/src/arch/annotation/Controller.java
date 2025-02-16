package arch.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)  // This ensures the annotation is available at runtime
public @interface Controller {
    // You can add elements here if needed, but for now, it's empty
}
