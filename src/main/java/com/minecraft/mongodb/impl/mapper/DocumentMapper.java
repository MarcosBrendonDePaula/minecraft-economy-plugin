package com.minecraft.mongodb.impl.mapper;

import com.minecraft.mongodb.api.annotation.Collection;
import com.minecraft.mongodb.api.annotation.Document;
import com.minecraft.mongodb.api.annotation.Field;
import com.minecraft.mongodb.api.annotation.Id;
import org.bson.types.ObjectId;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe para mapear objetos para documentos MongoDB e vice-versa
 */
public class DocumentMapper {
    
    private static final Logger logger = Logger.getLogger(DocumentMapper.class.getName());
    
    /**
     * Converte um objeto para um documento MongoDB
     * @param object Objeto a ser convertido
     * @return Documento MongoDB
     */
    public static Map<String, Object> toDocument(Object object) {
        if (object == null) {
            return null;
        }
        
        Class<?> clazz = object.getClass();
        
        // Verifica se a classe está anotada com @Document
        if (!clazz.isAnnotationPresent(Document.class)) {
            throw new IllegalArgumentException("A classe " + clazz.getName() + " não está anotada com @Document");
        }
        
        Map<String, Object> document = new HashMap<>();
        
        // Mapeia os campos
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                
                // Ignora campos estáticos e transitórios
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || 
                    java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                
                // Obtém o nome do campo no documento
                String fieldName = getDocumentFieldName(field);
                
                // Obtém o valor do campo
                Object value = field.get(object);
                
                // Verifica se o campo é o ID
                if (field.isAnnotationPresent(Id.class)) {
                    if (value == null) {
                        // Gera um novo ID se for nulo
                        value = new ObjectId().toString();
                        field.set(object, value);
                    }
                    document.put("_id", value);
                } else {
                    // Adiciona o campo ao documento
                    document.put(fieldName, value);
                }
            } catch (IllegalAccessException e) {
                logger.log(Level.WARNING, "Erro ao acessar campo: " + e.getMessage(), e);
            }
        }
        
        return document;
    }
    
    /**
     * Converte um documento MongoDB para um objeto
     * @param document Documento MongoDB
     * @param clazz Classe do objeto
     * @param <T> Tipo do objeto
     * @return Objeto
     */
    public static <T> T fromDocument(Map<String, Object> document, Class<T> clazz) {
        if (document == null) {
            return null;
        }
        
        // Verifica se a classe está anotada com @Document
        if (!clazz.isAnnotationPresent(Document.class)) {
            throw new IllegalArgumentException("A classe " + clazz.getName() + " não está anotada com @Document");
        }
        
        try {
            // Cria uma nova instância do objeto
            T object = clazz.getDeclaredConstructor().newInstance();
            
            // Mapeia os campos
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    
                    // Ignora campos estáticos e transitórios
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || 
                        java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                        continue;
                    }
                    
                    // Obtém o nome do campo no documento
                    String fieldName = getDocumentFieldName(field);
                    
                    // Verifica se o campo é o ID
                    if (field.isAnnotationPresent(Id.class)) {
                        Object id = document.get("_id");
                        if (id != null) {
                            field.set(object, convertValue(id, field.getType()));
                        }
                    } else {
                        // Obtém o valor do campo no documento
                        Object value = document.get(fieldName);
                        if (value != null) {
                            field.set(object, convertValue(value, field.getType()));
                        }
                    }
                } catch (IllegalAccessException e) {
                    logger.log(Level.WARNING, "Erro ao acessar campo: " + e.getMessage(), e);
                }
            }
            
            return object;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            logger.log(Level.SEVERE, "Erro ao criar instância: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Obtém o nome da coleção para uma classe
     * @param clazz Classe
     * @return Nome da coleção
     */
    public static String getCollectionName(Class<?> clazz) {
        // Verifica se a classe está anotada com @Collection
        if (clazz.isAnnotationPresent(Collection.class)) {
            return clazz.getAnnotation(Collection.class).value();
        }
        
        // Usa o nome da classe em minúsculas como nome da coleção
        return clazz.getSimpleName().toLowerCase();
    }
    
    /**
     * Obtém o nome do campo no documento
     * @param field Campo
     * @return Nome do campo no documento
     */
    private static String getDocumentFieldName(java.lang.reflect.Field field) {
        // Verifica se o campo está anotado com @Field
        if (field.isAnnotationPresent(Field.class)) {
            String name = field.getAnnotation(Field.class).value();
            if (!name.isEmpty()) {
                return name;
            }
        }
        
        // Usa o nome do campo como nome no documento
        return field.getName();
    }
    
    /**
     * Converte um valor para o tipo especificado
     * @param value Valor a ser convertido
     * @param targetType Tipo alvo
     * @return Valor convertido
     */
    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        // Se o valor já é do tipo alvo, retorna o valor
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        // Converte tipos primitivos
        if (targetType == int.class || targetType == Integer.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
        } else if (targetType == long.class || targetType == Long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            }
        } else if (targetType == double.class || targetType == Double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            } else if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
        } else if (targetType == String.class) {
            return value.toString();
        }
        
        // Tenta converter usando valueOf
        try {
            Method valueOf = targetType.getMethod("valueOf", String.class);
            return valueOf.invoke(null, value.toString());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.log(Level.WARNING, "Erro ao converter valor: " + e.getMessage(), e);
        }
        
        return null;
    }
}
