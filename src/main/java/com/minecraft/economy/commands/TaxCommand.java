package com.minecraft.economy.commands;

import com.minecraft.economy.core.EconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Comando para gerenciar impostos e dinheiro rotativo
 */
public class TaxCommand implements CommandExecutor {

    private final EconomyPlugin plugin;

    public TaxCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Executa o processamento do comando de forma assíncrona para evitar travamentos
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    processCommand(sender, args);
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro ao processar comando de impostos: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Volta para o thread principal para enviar mensagem de erro
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage("§cOcorreu um erro ao processar o comando. Tente novamente mais tarde.");
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return true;
    }
    
    /**
     * Processa o comando de impostos
     */
    private void processCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economy.tax")) {
            sendMessage(sender, "§cVocê não tem permissão para usar este comando.");
            return;
        }
        
        if (args.length == 0) {
            showHelp(sender);
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "apply":
                applyTaxes(sender);
                break;
            case "info":
                showTaxInfo(sender);
                break;
            case "rate":
                if (args.length < 2) {
                    sendMessage(sender, "§cUso correto: /tax rate <taxa>");
                    return;
                }
                setTaxRate(sender, args[1]);
                break;
            case "threshold":
                if (args.length < 2) {
                    sendMessage(sender, "§cUso correto: /tax threshold <valor>");
                    return;
                }
                setTaxThreshold(sender, args[1]);
                break;
            case "decay":
                applyInactivityDecay(sender);
                break;
            default:
                showHelp(sender);
                break;
        }
    }
    
    /**
     * Mostra a ajuda do comando
     */
    private void showHelp(CommandSender sender) {
        sendMessage(sender, "§6=== Comandos de Impostos ===");
        sendMessage(sender, "§e/tax apply §7- Aplica impostos sobre riqueza");
        sendMessage(sender, "§e/tax info §7- Mostra informações sobre impostos");
        sendMessage(sender, "§e/tax rate <taxa> §7- Define a taxa de imposto sobre riqueza");
        sendMessage(sender, "§e/tax threshold <valor> §7- Define o limite para aplicação de imposto");
        sendMessage(sender, "§e/tax decay §7- Aplica decaimento por inatividade");
    }
    
    /**
     * Aplica impostos sobre riqueza
     */
    private void applyTaxes(CommandSender sender) {
        sendMessage(sender, "§6Aplicando impostos sobre riqueza...");
        
        double taxRate = plugin.getConfigManager().getWealthTaxRate();
        double threshold = plugin.getConfigManager().getWealthTaxThreshold();
        
        // Obtém todos os jogadores com saldo acima do limite
        plugin.getAsyncMongoDBManager().getDatabase().getCollection("players").find()
            .forEach(doc -> {
                String uuidStr = doc.getString("uuid");
                double balance = doc.getDouble("balance");
                
                if (balance > threshold) {
                    UUID playerId = UUID.fromString(uuidStr);
                    double taxAmount = (balance - threshold) * taxRate;
                    
                    // Cobra o imposto
                    plugin.getAsyncMongoDBManager().withdraw(playerId, taxAmount, "Imposto sobre riqueza")
                        .thenAccept(success -> {
                            if (success) {
                                // Registra o imposto coletado
                                plugin.getConfigDatabase().getConfig("tax_collected", 0.0)
                                    .thenAccept(currentTaxes -> {
                                        double newTotal = currentTaxes + taxAmount;
                                        plugin.getConfigDatabase().setConfig("tax_collected", newTotal);
                                    });
                                
                                // Notifica o jogador se estiver online
                                Player player = Bukkit.getPlayer(playerId);
                                if (player != null) {
                                    sendMessage(player, "§cVocê pagou §f" + String.format("%.2f", taxAmount) + " §cde imposto sobre riqueza.");
                                }
                            }
                        });
                }
            });
        
        // Redistribui parte dos impostos coletados
        redistributeTaxes();
        
        sendMessage(sender, "§aImpostos aplicados com sucesso!");
    }
    
    /**
     * Redistribui parte dos impostos coletados
     */
    private void redistributeTaxes() {
        plugin.getConfigDatabase().getConfig("tax_collected", 0.0)
            .thenAccept(taxCollected -> {
                if (taxCollected <= 0) {
                    return;
                }
                
                double redistributionRate = plugin.getConfigManager().getTaxRedistributionRate();
                double amountToRedistribute = taxCollected * redistributionRate;
                
                if (amountToRedistribute <= 0) {
                    return;
                }
                
                // Obtém todos os jogadores ativos
                List<UUID> activePlayers = new ArrayList<>();
                
                plugin.getAsyncMongoDBManager().getDatabase().getCollection("players").find()
                    .forEach(doc -> {
                        String uuidStr = doc.getString("uuid");
                        long lastActivity = doc.getLong("last_activity");
                        
                        // Considera jogadores ativos nos últimos 7 dias
                        if (System.currentTimeMillis() - lastActivity < 7 * 24 * 60 * 60 * 1000) {
                            activePlayers.add(UUID.fromString(uuidStr));
                        }
                    });
                
                if (activePlayers.isEmpty()) {
                    return;
                }
                
                // Calcula o valor a ser distribuído para cada jogador
                double amountPerPlayer = amountToRedistribute / activePlayers.size();
                
                // Distribui o dinheiro
                for (UUID playerId : activePlayers) {
                    plugin.getAsyncMongoDBManager().deposit(playerId, amountPerPlayer, "Redistribuição de impostos")
                        .thenAccept(success -> {
                            if (success) {
                                // Notifica o jogador se estiver online
                                Player player = Bukkit.getPlayer(playerId);
                                if (player != null) {
                                    sendMessage(player, "§aVocê recebeu §f" + String.format("%.2f", amountPerPlayer) + " §ada redistribuição de impostos.");
                                }
                            }
                        });
                }
                
                // Atualiza o valor de impostos coletados
                double remainingTaxes = taxCollected - amountToRedistribute;
                plugin.getConfigDatabase().setConfig("tax_collected", remainingTaxes);
            });
    }
    
    /**
     * Mostra informações sobre impostos
     */
    private void showTaxInfo(CommandSender sender) {
        double taxRate = plugin.getConfigManager().getWealthTaxRate() * 100;
        double threshold = plugin.getConfigManager().getWealthTaxThreshold();
        double transactionTaxRate = plugin.getConfigManager().getTransactionTaxRate() * 100;
        double redistributionRate = plugin.getConfigManager().getTaxRedistributionRate() * 100;
        
        plugin.getConfigDatabase().getConfig("tax_collected", 0.0)
            .thenAccept(taxCollected -> {
                sendMessage(sender, "§6=== Informações sobre Impostos ===");
                sendMessage(sender, "§eTaxa de imposto sobre riqueza: §f" + String.format("%.1f", taxRate) + "%");
                sendMessage(sender, "§eLimite para aplicação de imposto: §f" + String.format("%.2f", threshold));
                sendMessage(sender, "§eTaxa de imposto sobre transações: §f" + String.format("%.1f", transactionTaxRate) + "%");
                sendMessage(sender, "§eTaxa de redistribuição: §f" + String.format("%.1f", redistributionRate) + "%");
                sendMessage(sender, "§eImpostos coletados: §f" + String.format("%.2f", taxCollected));
            });
    }
    
    /**
     * Define a taxa de imposto sobre riqueza
     */
    private void setTaxRate(CommandSender sender, String rateStr) {
        try {
            double rate = Double.parseDouble(rateStr) / 100.0;
            
            if (rate < 0 || rate > 1) {
                sendMessage(sender, "§cA taxa deve estar entre 0 e 100.");
                return;
            }
            
            plugin.getConfigDatabase().setConfig("economy.wealth_tax_rate", rate)
                .thenAccept(success -> {
                    if (success) {
                        sendMessage(sender, "§aTaxa de imposto sobre riqueza definida para §f" + String.format("%.1f", rate * 100) + "%§a.");
                    } else {
                        sendMessage(sender, "§cOcorreu um erro ao definir a taxa de imposto.");
                    }
                });
        } catch (NumberFormatException e) {
            sendMessage(sender, "§cValor inválido. Use um número válido.");
        }
    }
    
    /**
     * Define o limite para aplicação de imposto
     */
    private void setTaxThreshold(CommandSender sender, String thresholdStr) {
        try {
            double threshold = Double.parseDouble(thresholdStr);
            
            if (threshold < 0) {
                sendMessage(sender, "§cO limite deve ser maior ou igual a zero.");
                return;
            }
            
            plugin.getConfigDatabase().setConfig("economy.wealth_tax_threshold", threshold)
                .thenAccept(success -> {
                    if (success) {
                        sendMessage(sender, "§aLimite para aplicação de imposto definido para §f" + String.format("%.2f", threshold) + "§a.");
                    } else {
                        sendMessage(sender, "§cOcorreu um erro ao definir o limite.");
                    }
                });
        } catch (NumberFormatException e) {
            sendMessage(sender, "§cValor inválido. Use um número válido.");
        }
    }
    
    /**
     * Aplica decaimento por inatividade
     */
    private void applyInactivityDecay(CommandSender sender) {
        sendMessage(sender, "§6Aplicando decaimento por inatividade...");
        
        int inactivityPeriod = plugin.getConfigManager().getInactivityPeriod();
        double decayRate = plugin.getConfigManager().getInactivityDecayRate();
        long inactivityThreshold = System.currentTimeMillis() - (inactivityPeriod * 24 * 60 * 60 * 1000);
        
        // Obtém todos os jogadores inativos
        plugin.getAsyncMongoDBManager().getDatabase().getCollection("players").find()
            .forEach(doc -> {
                String uuidStr = doc.getString("uuid");
                double balance = doc.getDouble("balance");
                long lastActivity = doc.getLong("last_activity");
                
                if (lastActivity < inactivityThreshold && balance > 0) {
                    UUID playerId = UUID.fromString(uuidStr);
                    double decayAmount = balance * decayRate;
                    
                    // Aplica o decaimento
                    plugin.getAsyncMongoDBManager().withdraw(playerId, decayAmount, "Decaimento por inatividade")
                        .thenAccept(success -> {
                            if (success) {
                                // Registra o decaimento
                                plugin.getConfigDatabase().getConfig("decay_collected", 0.0)
                                    .thenAccept(currentDecay -> {
                                        double newTotal = currentDecay + decayAmount;
                                        plugin.getConfigDatabase().setConfig("decay_collected", newTotal);
                                    });
                            }
                        });
                }
            });
        
        sendMessage(sender, "§aDecaimento por inatividade aplicado com sucesso!");
    }
    
    /**
     * Envia uma mensagem para o sender de forma segura (no thread principal)
     */
    private void sendMessage(CommandSender sender, String message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                sender.sendMessage(message);
            }
        }.runTask(plugin);
    }
}
