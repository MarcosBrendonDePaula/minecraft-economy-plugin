package com.minecraft.mongodb.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface para operações em bancos de dados MongoDB
 */
public interface MongoDatabase {
    
    /**
     * Obtém o nome do banco de dados
     * @return Nome do banco de dados
     */
    String getName();
    
    /**
     * Obtém uma coleção do banco de dados
     * @param collectionName Nome da coleção
     * @return Interface para operações na coleção
     */
    MongoCollection getCollection(String collectionName);
    
    /**
     * Verifica se uma coleção existe
     * @param collectionName Nome da coleção
     * @return true se a coleção existe
     */
    boolean collectionExists(String collectionName);
    
    /**
     * Verifica se uma coleção existe de forma assíncrona
     * @param collectionName Nome da coleção
     * @return CompletableFuture com o resultado da verificação
     */
    CompletableFuture<Boolean> collectionExistsAsync(String collectionName);
    
    /**
     * Cria uma coleção
     * @param collectionName Nome da coleção
     * @return true se a coleção foi criada com sucesso
     */
    boolean createCollection(String collectionName);
    
    /**
     * Cria uma coleção de forma assíncrona
     * @param collectionName Nome da coleção
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> createCollectionAsync(String collectionName);
    
    /**
     * Exclui uma coleção
     * @param collectionName Nome da coleção
     * @return true se a coleção foi excluída com sucesso
     */
    boolean dropCollection(String collectionName);
    
    /**
     * Exclui uma coleção de forma assíncrona
     * @param collectionName Nome da coleção
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> dropCollectionAsync(String collectionName);
    
    /**
     * Lista as coleções do banco de dados
     * @return Lista de nomes de coleções
     */
    List<String> listCollections();
    
    /**
     * Lista as coleções do banco de dados de forma assíncrona
     * @return CompletableFuture com a lista de nomes de coleções
     */
    CompletableFuture<List<String>> listCollectionsAsync();
    
    /**
     * Executa um comando no banco de dados
     * @param command Comando a ser executado
     * @return Resultado do comando
     */
    Object runCommand(Object command);
    
    /**
     * Executa um comando no banco de dados de forma assíncrona
     * @param command Comando a ser executado
     * @return CompletableFuture com o resultado do comando
     */
    CompletableFuture<Object> runCommandAsync(Object command);
}
