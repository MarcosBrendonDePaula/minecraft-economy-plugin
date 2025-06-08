package com.minecraft.economy.infrastructure;

import com.minecraft.economy.api.infrastructure.AsyncExecutor;
import com.minecraft.economy.core.EconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Implementação de AsyncExecutor usando o agendador do Bukkit
 */
public class BukkitAsyncExecutor implements AsyncExecutor {

    private final EconomyPlugin plugin;
    
    /**
     * Construtor
     * @param plugin Instância do plugin
     */
    public BukkitAsyncExecutor(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int runAsync(Runnable task) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        return bukkitTask.getTaskId();
    }

    @Override
    public int runAsyncDelayed(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        return bukkitTask.getTaskId();
    }

    @Override
    public int runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return bukkitTask.getTaskId();
    }

    @Override
    public int runSync(Runnable task) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTask(plugin, task);
        return bukkitTask.getTaskId();
    }

    @Override
    public int runSyncDelayed(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        return bukkitTask.getTaskId();
    }

    @Override
    public int runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return bukkitTask.getTaskId();
    }

    @Override
    public void cancelTask(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
    }

    @Override
    public void cancelAllTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                T result = supplier.get();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }

    @Override
    public <T> CompletableFuture<T> supplySync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        // Se já estamos no thread principal, executa diretamente
        if (Bukkit.isPrimaryThread()) {
            try {
                T result = supplier.get();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else {
            // Caso contrário, agenda para o thread principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    T result = supplier.get();
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }
        
        return future;
    }
}
