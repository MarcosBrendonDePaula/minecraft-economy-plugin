package com.minecraft.economy.api.infrastructure;

/**
 * Interface para gerenciamento de conexão com banco de dados
 */
public interface DatabaseConnection {
    
    /**
     * Inicializa a conexão com o banco de dados
     * @return true se a conexão foi estabelecida com sucesso
     */
    boolean connect();
    
    /**
     * Fecha a conexão com o banco de dados
     */
    void disconnect();
    
    /**
     * Verifica se a conexão está ativa
     * @return true se a conexão está ativa
     */
    boolean isConnected();
    
    /**
     * Obtém o nome do banco de dados
     * @return Nome do banco de dados
     */
    String getDatabaseName();
    
    /**
     * Obtém a string de conexão
     * @return String de conexão
     */
    String getConnectionString();
    
    /**
     * Verifica se a conexão está ativa e tenta reconectar se necessário
     * @return true se a conexão está ativa ou foi restabelecida
     */
    boolean ensureConnected();
}
