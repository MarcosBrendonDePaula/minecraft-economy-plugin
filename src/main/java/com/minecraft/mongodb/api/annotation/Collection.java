package com.minecraft.mongodb.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para especificar o nome da coleção MongoDB para uma classe
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Collection {
    /**
     * Nome da coleção
     * @return Nome da coleção
     */
    String value();
}
