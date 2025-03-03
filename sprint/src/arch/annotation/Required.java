package arch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Required {
    String message() default "This field is required";
}



// @Retention(RetentionPolicy.RUNTIME)
// @Target(ElementType.FIELD)
// public @interface Text {
//     int minLength() default 0;
//     int maxLength() default Integer.MAX_VALUE;
//     String message() default "Text length must be between {minLength} and {maxLength}";
// }



