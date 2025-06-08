package com.minecraft.economy.api.repository;

import com.minecraft.economy.api.model.Transaction;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface para repositório de transações
 * Define operações para registro e consulta de transações
 */
public interface TransactionRepository {
    
    /**
     * Registra uma transação
     * @param transaction Dados da transação
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> recordTransaction(Transaction transaction);
    
    /**
     * Obtém as transações de um jogador
     * @param playerId UUID do jogador
     * @param limit Limite de transações a retornar
     * @return CompletableFuture com a lista de transações
     */
    CompletableFuture<List<Transaction>> getPlayerTransactions(UUID playerId, int limit);
    
    /**
     * Obtém as transações recentes
     * @param limit Limite de transações a retornar
     * @return CompletableFuture com a lista de transações
     */
    CompletableFuture<List<Transaction>> getRecentTransactions(int limit);
}
