package com.minecraft.economy.commands;

import com.minecraft.economy.core.EconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Comando para transferir dinheiro entre jogadores
 */
public class PayCommand implements CommandExecutor {

    private final EconomyPlugin plugin;

    public PayCommand(EconomyPlugin plugin) {
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
                    plugin.getLogger().severe("Erro ao processar comando de pagamento: " + e.getMessage());
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
     * Processa o comando de pagamento
     */
    private void processCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "§cEste comando só pode ser usado por jogadores.");
            return;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "§cUso correto: /pay <jogador> <valor>");
            return;
        }
        
        Player player = (Player) sender;
        String targetName = args[0];
        String amountStr = args[1];
        
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            sendMessage(sender, "§cJogador não encontrado.");
            return;
        }
        
        if (player.equals(target)) {
            sendMessage(sender, "§cVocê não pode transferir dinheiro para si mesmo.");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            sendMessage(sender, "§cValor inválido. Use um número válido.");
            return;
        }
        
        if (amount <= 0) {
            sendMessage(sender, "§cO valor deve ser maior que zero.");
            return;
        }
        
        // Aplica taxa de transação
        double taxRate = plugin.getConfigDatabase().getDouble("economy.transaction_tax_rate", 0.05);
        double taxAmount = amount * taxRate;
        double totalAmount = amount + taxAmount;
        
        // Transfere o dinheiro
        plugin.getAsyncMongoDBManager().transferMoney(player.getUniqueId(), target.getUniqueId(), amount, "Transferência via comando /pay")
            .thenAccept(success -> {
                if (success) {
                    // Cobra a taxa
                    plugin.getAsyncMongoDBManager().withdraw(player.getUniqueId(), taxAmount, "Taxa de transação")
                        .thenAccept(taxSuccess -> {
                            if (taxSuccess) {
                                // Registra a taxa coletada
                                plugin.getConfigDatabase().getConfig("tax_collected", 0.0)
                                    .thenAccept(currentTaxes -> {
                                        double newTotal = currentTaxes + taxAmount;
                                        plugin.getConfigDatabase().setConfig("tax_collected", newTotal);
                                    });
                            }
                        });
                    
                    String currencyName = amount == 1.0 ? 
                        plugin.getConfigManager().getCurrencyName() : 
                        plugin.getConfigManager().getCurrencyNamePlural();
                    
                    sendMessage(player, "§aVocê transferiu §f" + String.format("%.2f", amount) + " " + currencyName + " §apara §f" + target.getName() + "§a.");
                    sendMessage(player, "§aTaxa de transação: §f" + String.format("%.2f", taxAmount) + " " + currencyName + "§a.");
                    sendMessage(target, "§aVocê recebeu §f" + String.format("%.2f", amount) + " " + currencyName + " §ade §f" + player.getName() + "§a.");
                } else {
                    sendMessage(player, "§cVocê não tem dinheiro suficiente para fazer esta transferência.");
                }
            });
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
