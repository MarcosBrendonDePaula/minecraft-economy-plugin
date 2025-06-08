package com.minecraft.economy.api.infrastructure;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Interface para execução de tarefas assíncronas
 */
public interface AsyncExecutor {
    
    /**
     * Executa uma tarefa de forma assíncrona
     * @param task Tarefa a ser executada
     * @return ID da tarefa
     */
    int runAsync(Runnable task);
    
    /**
     * Executa uma tarefa de forma assíncrona após um atraso
     * @param task Tarefa a ser executada
     * @param delayTicks Atraso em ticks
     * @return ID da tarefa
     */
    int runAsyncDelayed(Runnable task, long delayTicks);
    
    /**
     * Executa uma tarefa de forma assíncrona periodicamente
     * @param task Tarefa a ser executada
     * @param delayTicks Atraso inicial em ticks
     * @param periodTicks Período em ticks
     * @return ID da tarefa
     */
    int runAsyncRepeating(Runnable task, long delayTicks, long periodTicks);
    
    /**
     * Executa uma tarefa de forma síncrona (no thread principal)
     * @param task Tarefa a ser executada
     * @return ID da tarefa
     */
    int runSync(Runnable task);
    
    /**
     * Executa uma tarefa de forma síncrona após um atraso
     * @param task Tarefa a ser executada
     * @param delayTicks Atraso em ticks
     * @return ID da tarefa
     */
    int runSyncDelayed(Runnable task, long delayTicks);
    
    /**
     * Executa uma tarefa de forma síncrona periodicamente
     * @param task Tarefa a ser executada
     * @param delayTicks Atraso inicial em ticks
     * @param periodTicks Período em ticks
     * @return ID da tarefa
     */
    int runSyncRepeating(Runnable task, long delayTicks, long periodTicks);
    
    /**
     * Cancela uma tarefa
     * @param taskId ID da tarefa
     */
    void cancelTask(int taskId);
    
    /**
     * Cancela todas as tarefas
     */
    void cancelAllTasks();
    
    /**
     * Executa uma tarefa de forma assíncrona e retorna um CompletableFuture
     * @param supplier Fornecedor do resultado
     * @param <T> Tipo do resultado
     * @return CompletableFuture com o resultado
     */
    <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier);
    
    /**
     * Executa uma tarefa de forma síncrona e retorna um CompletableFuture
     * @param supplier Fornecedor do resultado
     * @param <T> Tipo do resultado
     * @return CompletableFuture com o resultado
     */
    <T> CompletableFuture<T> supplySync(Supplier<T> supplier);
}
