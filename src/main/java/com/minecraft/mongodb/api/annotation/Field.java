package com.minecraft.mongodb.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para especificar o nome do campo no documento MongoDB
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Field {
    /**
     * Nome do campo no documento
     * @return Nome do campo
     */
    String value() default "";
}
