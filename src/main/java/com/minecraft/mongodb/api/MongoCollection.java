package com.minecraft.mongodb.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface para operações em coleções MongoDB
 */
public interface MongoCollection {
    
    /**
     * Obtém o nome da coleção
     * @return Nome da coleção
     */
    String getName();
    
    /**
     * Cria uma consulta para buscar documentos
     * @return Builder de consulta
     */
    MongoQuery find();
    
    /**
     * Cria uma consulta para atualizar documentos
     * @return Builder de atualização
     */
    MongoUpdate update();
    
    /**
     * Cria uma consulta para excluir documentos
     * @return Builder de exclusão
     */
    MongoDelete delete();
    
    /**
     * Insere um documento na coleção
     * @param document Documento a ser inserido
     * @return true se a inserção foi bem-sucedida
     */
    boolean insert(Map<String, Object> document);
    
    /**
     * Insere um documento na coleção de forma assíncrona
     * @param document Documento a ser inserido
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> insertAsync(Map<String, Object> document);
    
    /**
     * Insere um objeto na coleção
     * @param object Objeto a ser inserido
     * @param <T> Tipo do objeto
     * @return true se a inserção foi bem-sucedida
     */
    <T> boolean insert(T object);
    
    /**
     * Insere um objeto na coleção de forma assíncrona
     * @param object Objeto a ser inserido
     * @param <T> Tipo do objeto
     * @return CompletableFuture com o resultado da operação
     */
    <T> CompletableFuture<Boolean> insertAsync(T object);
    
    /**
     * Insere vários documentos na coleção
     * @param documents Documentos a serem inseridos
     * @return Número de documentos inseridos
     */
    int insertManyDocuments(List<Map<String, Object>> documents);
    
    /**
     * Insere vários documentos na coleção de forma assíncrona
     * @param documents Documentos a serem inseridos
     * @return CompletableFuture com o número de documentos inseridos
     */
    CompletableFuture<Integer> insertManyDocumentsAsync(List<Map<String, Object>> documents);
    
    /**
     * Insere vários objetos na coleção
     * @param objects Objetos a serem inseridos
     * @param <T> Tipo dos objetos
     * @return Número de objetos inseridos
     */
    <T> int insertManyObjects(List<T> objects);
    
    /**
     * Insere vários objetos na coleção de forma assíncrona
     * @param objects Objetos a serem inseridos
     * @param <T> Tipo dos objetos
     * @return CompletableFuture com o número de objetos inseridos
     */
    <T> CompletableFuture<Integer> insertManyObjectsAsync(List<T> objects);
    
    /**
     * Conta o número de documentos na coleção
     * @return Número de documentos
     */
    long count();
    
    /**
     * Conta o número de documentos na coleção de forma assíncrona
     * @return CompletableFuture com o número de documentos
     */
    CompletableFuture<Long> countAsync();
    
    /**
     * Verifica se a coleção está vazia
     * @return true se a coleção está vazia
     */
    boolean isEmpty();
    
    /**
     * Verifica se a coleção está vazia de forma assíncrona
     * @return CompletableFuture com o resultado da verificação
     */
    CompletableFuture<Boolean> isEmptyAsync();
    
    /**
     * Limpa a coleção
     * @return Número de documentos removidos
     */
    long clear();
    
    /**
     * Limpa a coleção de forma assíncrona
     * @return CompletableFuture com o número de documentos removidos
     */
    CompletableFuture<Long> clearAsync();
    
    /**
     * Interface para construção de consultas
     */
    interface MongoQuery {
        /**
         * Adiciona uma condição de igualdade
         * @param field Campo a ser comparado
         * @return Builder de condição
         */
        Condition where(String field);
        
        /**
         * Limita o número de resultados
         * @param limit Limite de resultados
         * @return Builder de consulta
         */
        MongoQuery limit(int limit);
        
        /**
         * Define o deslocamento dos resultados
         * @param skip Deslocamento
         * @return Builder de consulta
         */
        MongoQuery skip(int skip);
        
        /**
         * Ordena os resultados
         * @param field Campo para ordenação
         * @param ascending true para ordem ascendente, false para descendente
         * @return Builder de consulta
         */
        MongoQuery sort(String field, boolean ascending);
        
        /**
         * Obtém o primeiro resultado como um mapa
         * @return Resultado da consulta
         */
        Optional<Map<String, Object>> first();
        
        /**
         * Obtém o primeiro resultado como um mapa de forma assíncrona
         * @return CompletableFuture com o resultado da consulta
         */
        CompletableFuture<Optional<Map<String, Object>>> firstAsync();
        
        /**
         * Obtém o primeiro resultado como um objeto
         * @param clazz Classe do objeto
         * @param <T> Tipo do objeto
         * @return Resultado da consulta
         */
        <T> Optional<T> first(Class<T> clazz);
        
        /**
         * Obtém o primeiro resultado como um objeto de forma assíncrona
         * @param clazz Classe do objeto
         * @param <T> Tipo do objeto
         * @return CompletableFuture com o resultado da consulta
         */
        <T> CompletableFuture<Optional<T>> firstAsync(Class<T> clazz);
        
        /**
         * Obtém todos os resultados como mapas
         * @return Lista de resultados
         */
        List<Map<String, Object>> toList();
        
        /**
         * Obtém todos os resultados como mapas de forma assíncrona
         * @return CompletableFuture com a lista de resultados
         */
        CompletableFuture<List<Map<String, Object>>> toListAsync();
        
        /**
         * Obtém todos os resultados como objetos
         * @param clazz Classe dos objetos
         * @param <T> Tipo dos objetos
         * @return Lista de resultados
         */
        <T> List<T> toList(Class<T> clazz);
        
        /**
         * Obtém todos os resultados como objetos de forma assíncrona
         * @param clazz Classe dos objetos
         * @param <T> Tipo dos objetos
         * @return CompletableFuture com a lista de resultados
         */
        <T> CompletableFuture<List<T>> toListAsync(Class<T> clazz);
        
        /**
         * Conta o número de documentos que correspondem à consulta
         * @return Número de documentos
         */
        long count();
        
        /**
         * Conta o número de documentos que correspondem à consulta de forma assíncrona
         * @return CompletableFuture com o número de documentos
         */
        CompletableFuture<Long> countAsync();
        
        /**
         * Verifica se existe algum documento que corresponda à consulta
         * @return true se existe algum documento
         */
        boolean exists();
        
        /**
         * Verifica se existe algum documento que corresponda à consulta de forma assíncrona
         * @return CompletableFuture com o resultado da verificação
         */
        CompletableFuture<Boolean> existsAsync();
        
        /**
         * Interface para construção de condições
         */
        interface Condition {
            /**
             * Adiciona uma condição de igualdade
             * @param value Valor a ser comparado
             * @return Builder de consulta
             */
            MongoQuery isEqualTo(Object value);
            
            /**
             * Adiciona uma condição de desigualdade
             * @param value Valor a ser comparado
             * @return Builder de consulta
             */
            MongoQuery notEquals(Object value);
            
            /**
             * Adiciona uma condição de maior que
             * @param value Valor a ser comparado
             * @return Builder de consulta
             */
            MongoQuery greaterThan(Object value);
            
            /**
             * Adiciona uma condição de maior ou igual a
             * @param value Valor a ser comparado
             * @return Builder de consulta
             */
            MongoQuery greaterThanOrEquals(Object value);
            
            /**
             * Adiciona uma condição de menor que
             * @param value Valor a ser comparado
             * @return Builder de consulta
             */
            MongoQuery lessThan(Object value);
            
            /**
             * Adiciona uma condição de menor ou igual a
             * @param value Valor a ser comparado
             * @return Builder de consulta
             */
            MongoQuery lessThanOrEquals(Object value);
            
            /**
             * Adiciona uma condição de contém
             * @param value Valor a ser comparado
             * @return Builder de consulta
             */
            MongoQuery contains(String value);
            
            /**
             * Adiciona uma condição de começa com
             * @param value Valor a ser comparado
             * @return Builder de consulta
             */
            MongoQuery startsWith(String value);
            
            /**
             * Adiciona uma condição de termina com
             * @param value Valor a ser comparado
             * @return Builder de consulta
             */
            MongoQuery endsWith(String value);
            
            /**
             * Adiciona uma condição de está em
             * @param values Valores a serem comparados
             * @return Builder de consulta
             */
            MongoQuery in(List<?> values);
            
            /**
             * Adiciona uma condição de não está em
             * @param values Valores a serem comparados
             * @return Builder de consulta
             */
            MongoQuery notIn(List<?> values);
            
            /**
             * Adiciona uma condição de existe
             * @return Builder de consulta
             */
            MongoQuery exists();
            
            /**
             * Adiciona uma condição de não existe
             * @return Builder de consulta
             */
            MongoQuery notExists();
        }
    }
    
    /**
     * Interface para construção de atualizações
     */
    interface MongoUpdate {
        /**
         * Adiciona uma condição de igualdade
         * @param field Campo a ser comparado
         * @return Builder de condição
         */
        Condition where(String field);
        
        /**
         * Define um valor para um campo
         * @param field Campo a ser atualizado
         * @param value Novo valor
         * @return Builder de atualização
         */
        MongoUpdate set(String field, Object value);
        
        /**
         * Incrementa um valor em um campo
         * @param field Campo a ser atualizado
         * @param value Valor a ser incrementado
         * @return Builder de atualização
         */
        MongoUpdate increment(String field, Number value);
        
        /**
         * Decrementa um valor em um campo
         * @param field Campo a ser atualizado
         * @param value Valor a ser decrementado
         * @return Builder de atualização
         */
        MongoUpdate decrement(String field, Number value);
        
        /**
         * Multiplica um valor em um campo
         * @param field Campo a ser atualizado
         * @param value Valor a ser multiplicado
         * @return Builder de atualização
         */
        MongoUpdate multiply(String field, Number value);
        
        /**
         * Divide um valor em um campo
         * @param field Campo a ser atualizado
         * @param value Valor a ser dividido
         * @return Builder de atualização
         */
        MongoUpdate divide(String field, Number value);
        
        /**
         * Adiciona um valor a um array
         * @param field Campo a ser atualizado
         * @param value Valor a ser adicionado
         * @return Builder de atualização
         */
        MongoUpdate push(String field, Object value);
        
        /**
         * Remove um valor de um array
         * @param field Campo a ser atualizado
         * @param value Valor a ser removido
         * @return Builder de atualização
         */
        MongoUpdate pull(String field, Object value);
        
        /**
         * Remove um campo
         * @param field Campo a ser removido
         * @return Builder de atualização
         */
        MongoUpdate unset(String field);
        
        /**
         * Executa a atualização
         * @return Número de documentos atualizados
         */
        int execute();
        
        /**
         * Executa a atualização de forma assíncrona
         * @return CompletableFuture com o número de documentos atualizados
         */
        CompletableFuture<Integer> executeAsync();
        
        /**
         * Interface para construção de condições
         */
        interface Condition {
            /**
             * Adiciona uma condição de igualdade
             * @param value Valor a ser comparado
             * @return Builder de atualização
             */
            MongoUpdate isEqualTo(Object value);
            
            /**
             * Adiciona uma condição de desigualdade
             * @param value Valor a ser comparado
             * @return Builder de atualização
             */
            MongoUpdate notEquals(Object value);
            
            /**
             * Adiciona uma condição de maior que
             * @param value Valor a ser comparado
             * @return Builder de atualização
             */
            MongoUpdate greaterThan(Object value);
            
            /**
             * Adiciona uma condição de maior ou igual a
             * @param value Valor a ser comparado
             * @return Builder de atualização
             */
            MongoUpdate greaterThanOrEquals(Object value);
            
            /**
             * Adiciona uma condição de menor que
             * @param value Valor a ser comparado
             * @return Builder de atualização
             */
            MongoUpdate lessThan(Object value);
            
            /**
             * Adiciona uma condição de menor ou igual a
             * @param value Valor a ser comparado
             * @return Builder de atualização
             */
            MongoUpdate lessThanOrEquals(Object value);
            
            /**
             * Adiciona uma condição de está em
             * @param values Valores a serem comparados
             * @return Builder de atualização
             */
            MongoUpdate in(List<?> values);
            
            /**
             * Adiciona uma condição de não está em
             * @param values Valores a serem comparados
             * @return Builder de atualização
             */
            MongoUpdate notIn(List<?> values);
        }
    }
    
    /**
     * Interface para construção de exclusões
     */
    interface MongoDelete {
        /**
         * Adiciona uma condição de igualdade
         * @param field Campo a ser comparado
         * @return Builder de condição
         */
        Condition where(String field);
        
        /**
         * Executa a exclusão
         * @return Número de documentos excluídos
         */
        int execute();
        
        /**
         * Executa a exclusão de forma assíncrona
         * @return CompletableFuture com o número de documentos excluídos
         */
        CompletableFuture<Integer> executeAsync();
        
        /**
         * Interface para construção de condições
         */
        interface Condition {
            /**
             * Adiciona uma condição de igualdade
             * @param value Valor a ser comparado
             * @return Builder de exclusão
             */
            MongoDelete isEqualTo(Object value);
            
            /**
             * Adiciona uma condição de desigualdade
             * @param value Valor a ser comparado
             * @return Builder de exclusão
             */
            MongoDelete notEquals(Object value);
            
            /**
             * Adiciona uma condição de maior que
             * @param value Valor a ser comparado
             * @return Builder de exclusão
             */
            MongoDelete greaterThan(Object value);
            
            /**
             * Adiciona uma condição de maior ou igual a
             * @param value Valor a ser comparado
             * @return Builder de exclusão
             */
            MongoDelete greaterThanOrEquals(Object value);
            
            /**
             * Adiciona uma condição de menor que
             * @param value Valor a ser comparado
             * @return Builder de exclusão
             */
            MongoDelete lessThan(Object value);
            
            /**
             * Adiciona uma condição de menor ou igual a
             * @param value Valor a ser comparado
             * @return Builder de exclusão
             */
            MongoDelete lessThanOrEquals(Object value);
            
            /**
             * Adiciona uma condição de está em
             * @param values Valores a serem comparados
             * @return Builder de exclusão
             */
            MongoDelete in(List<?> values);
            
            /**
             * Adiciona uma condição de não está em
             * @param values Valores a serem comparados
             * @return Builder de exclusão
             */
            MongoDelete notIn(List<?> values);
        }
    }
}
