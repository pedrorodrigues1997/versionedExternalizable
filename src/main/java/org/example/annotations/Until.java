package org.example.annotations;

import org.example.adapters.FieldAdapter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SerializeWith {
    Class<? extends FieldAdapter<?>> value();
    Class<?> elementType() default Void.class;
}
