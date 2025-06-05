package com.minecraft.economy.commands;

import com.minecraft.economy.core.EconomyPlugin;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Comando para gerenciar dinheiro
 */
public class MoneyCommand implements CommandExecutor {

    private final EconomyPlugin plugin;

    public MoneyCommand(EconomyPlugin plugin) {
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
                    plugin.getLogger().severe("Erro ao processar comando de dinheiro: " + e.getMessage());
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
     * Processa o comando de dinheiro
     */
    private void processCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Comando /money - Mostra o próprio saldo
            if (!(sender instanceof Player)) {
                sendMessage(sender, "§cEste comando só pode ser usado por jogadores.");
                return;
            }
            
            Player player = (Player) sender;
            showBalance(player, player.getUniqueId());
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                showHelp(sender);
                break;
            case "pay":
                if (!(sender instanceof Player)) {
                    sendMessage(sender, "§cEste comando só pode ser usado por jogadores.");
                    return;
                }
                
                if (args.length < 3) {
                    sendMessage(sender, "§cUso correto: /money pay <jogador> <valor>");
                    return;
                }
                
                Player player = (Player) sender;
                String targetName = args[1];
                String amountStr = args[2];
                
                payMoney(player, targetName, amountStr);
                break;
            case "give":
                if (!sender.hasPermission("economy.admin")) {
                    sendMessage(sender, "§cVocê não tem permissão para usar este comando.");
                    return;
                }
                
                if (args.length < 3) {
                    sendMessage(sender, "§cUso correto: /money give <jogador> <valor>");
                    return;
                }
                
                String giveName = args[1];
                String giveAmountStr = args[2];
                
                giveMoney(sender, giveName, giveAmountStr);
                break;
            case "take":
                if (!sender.hasPermission("economy.admin")) {
                    sendMessage(sender, "§cVocê não tem permissão para usar este comando.");
                    return;
                }
                
                if (args.length < 3) {
                    sendMessage(sender, "§cUso correto: /money take <jogador> <valor>");
                    return;
                }
                
                String takeName = args[1];
                String takeAmountStr = args[2];
                
                takeMoney(sender, takeName, takeAmountStr);
                break;
            case "set":
                if (!sender.hasPermission("economy.admin")) {
                    sendMessage(sender, "§cVocê não tem permissão para usar este comando.");
                    return;
                }
                
                if (args.length < 3) {
                    sendMessage(sender, "§cUso correto: /money set <jogador> <valor>");
                    return;
                }
                
                String setName = args[1];
                String setAmountStr = args[2];
                
                setMoney(sender, setName, setAmountStr);
                break;
            case "top":
                showTopPlayers(sender);
                break;
            default:
                // Assume que é um nome de jogador
                String playerName = args[0];
                Player target = Bukkit.getPlayer(playerName);
                
                if (target == null) {
                    sendMessage(sender, "§cJogador não encontrado.");
                    return;
                }
                
                showBalance(sender, target.getUniqueId());
                break;
        }
    }
    
    /**
     * Mostra a ajuda do comando
     */
    private void showHelp(CommandSender sender) {
        sendMessage(sender, "§6=== Comandos de Dinheiro ===");
        sendMessage(sender, "§e/money §7- Mostra seu saldo");
        sendMessage(sender, "§e/money <jogador> §7- Mostra o saldo de outro jogador");
        sendMessage(sender, "§e/money pay <jogador> <valor> §7- Transfere dinheiro para outro jogador");
        sendMessage(sender, "§e/money top §7- Mostra os jogadores mais ricos");
        
        if (sender.hasPermission("economy.admin")) {
            sendMessage(sender, "§6=== Comandos Administrativos ===");
            sendMessage(sender, "§e/money give <jogador> <valor> §7- Dá dinheiro a um jogador");
            sendMessage(sender, "§e/money take <jogador> <valor> §7- Tira dinheiro de um jogador");
            sendMessage(sender, "§e/money set <jogador> <valor> §7- Define o saldo de um jogador");
        }
    }
    
    /**
     * Mostra o saldo de um jogador
     */
    private void showBalance(CommandSender sender, UUID playerId) {
        plugin.getAsyncMongoDBManager().getBalance(playerId)
            .thenAccept(balance -> {
                String currencyName = balance == 1.0 ? 
                    plugin.getConfigManager().getCurrencyName() : 
                    plugin.getConfigManager().getCurrencyNamePlural();
                
                sendMessage(sender, "§aSaldo: §f" + String.format("%.2f", balance) + " " + currencyName);
            });
    }
    
    /**
     * Transfere dinheiro para outro jogador
     */
    private void payMoney(Player sender, String targetName, String amountStr) {
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            sendMessage(sender, "§cJogador não encontrado.");
            return;
        }
        
        if (sender.equals(target)) {
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
        plugin.getAsyncMongoDBManager().transferMoney(sender.getUniqueId(), target.getUniqueId(), amount)
            .thenAccept(success -> {
                if (success) {
                    // Cobra a taxa
                    plugin.getAsyncMongoDBManager().withdraw(sender.getUniqueId(), taxAmount, "Taxa de transação")
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
                    
                    sendMessage(sender, "§aVocê transferiu §f" + String.format("%.2f", amount) + " " + currencyName + " §apara §f" + target.getName() + "§a.");
                    sendMessage(sender, "§aTaxa de transação: §f" + String.format("%.2f", taxAmount) + " " + currencyName + "§a.");
                    sendMessage(target, "§aVocê recebeu §f" + String.format("%.2f", amount) + " " + currencyName + " §ade §f" + sender.getName() + "§a.");
                } else {
                    sendMessage(sender, "§cVocê não tem dinheiro suficiente para fazer esta transferência.");
                }
            });
    }
    
    /**
     * Dá dinheiro a um jogador
     */
    private void giveMoney(CommandSender sender, String targetName, String amountStr) {
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            sendMessage(sender, "§cJogador não encontrado.");
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
        
        // Dá o dinheiro ao jogador
        plugin.getAsyncMongoDBManager().deposit(target.getUniqueId(), amount, "Comando administrativo")
            .thenAccept(success -> {
                if (success) {
                    String currencyName = amount == 1.0 ? 
                        plugin.getConfigManager().getCurrencyName() : 
                        plugin.getConfigManager().getCurrencyNamePlural();
                    
                    sendMessage(sender, "§aVocê deu §f" + String.format("%.2f", amount) + " " + currencyName + " §apara §f" + target.getName() + "§a.");
                    sendMessage(target, "§aVocê recebeu §f" + String.format("%.2f", amount) + " " + currencyName + " §ade um administrador.");
                } else {
                    sendMessage(sender, "§cOcorreu um erro ao dar dinheiro ao jogador.");
                }
            });
    }
    
    /**
     * Tira dinheiro de um jogador
     */
    private void takeMoney(CommandSender sender, String targetName, String amountStr) {
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            sendMessage(sender, "§cJogador não encontrado.");
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
        
        // Tira o dinheiro do jogador
        plugin.getAsyncMongoDBManager().withdraw(target.getUniqueId(), amount, "Comando administrativo")
            .thenAccept(success -> {
                if (success) {
                    String currencyName = amount == 1.0 ? 
                        plugin.getConfigManager().getCurrencyName() : 
                        plugin.getConfigManager().getCurrencyNamePlural();
                    
                    sendMessage(sender, "§aVocê tirou §f" + String.format("%.2f", amount) + " " + currencyName + " §ade §f" + target.getName() + "§a.");
                    sendMessage(target, "§cUm administrador tirou §f" + String.format("%.2f", amount) + " " + currencyName + " §cde você.");
                } else {
                    sendMessage(sender, "§cO jogador não tem dinheiro suficiente.");
                }
            });
    }
    
    /**
     * Define o saldo de um jogador
     */
    private void setMoney(CommandSender sender, String targetName, String amountStr) {
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            sendMessage(sender, "§cJogador não encontrado.");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            sendMessage(sender, "§cValor inválido. Use um número válido.");
            return;
        }
        
        if (amount < 0) {
            sendMessage(sender, "§cO valor não pode ser negativo.");
            return;
        }
        
        // Obtém o saldo atual
        plugin.getAsyncMongoDBManager().getBalance(target.getUniqueId())
            .thenAccept(currentBalance -> {
                // Calcula a diferença
                double diff = amount - currentBalance;
                
                if (diff > 0) {
                    // Deposita a diferença
                    plugin.getAsyncMongoDBManager().deposit(target.getUniqueId(), diff, "Comando administrativo")
                        .thenAccept(success -> {
                            if (success) {
                                String currencyName = amount == 1.0 ? 
                                    plugin.getConfigManager().getCurrencyName() : 
                                    plugin.getConfigManager().getCurrencyNamePlural();
                                
                                sendMessage(sender, "§aVocê definiu o saldo de §f" + target.getName() + " §apara §f" + String.format("%.2f", amount) + " " + currencyName + "§a.");
                                sendMessage(target, "§aSeu saldo foi definido para §f" + String.format("%.2f", amount) + " " + currencyName + " §apor um administrador.");
                            } else {
                                sendMessage(sender, "§cOcorreu um erro ao definir o saldo do jogador.");
                            }
                        });
                } else if (diff < 0) {
                    // Retira a diferença
                    plugin.getAsyncMongoDBManager().withdraw(target.getUniqueId(), -diff, "Comando administrativo")
                        .thenAccept(success -> {
                            if (success) {
                                String currencyName = amount == 1.0 ? 
                                    plugin.getConfigManager().getCurrencyName() : 
                                    plugin.getConfigManager().getCurrencyNamePlural();
                                
                                sendMessage(sender, "§aVocê definiu o saldo de §f" + target.getName() + " §apara §f" + String.format("%.2f", amount) + " " + currencyName + "§a.");
                                sendMessage(target, "§aSeu saldo foi definido para §f" + String.format("%.2f", amount) + " " + currencyName + " §apor um administrador.");
                            } else {
                                sendMessage(sender, "§cOcorreu um erro ao definir o saldo do jogador.");
                            }
                        });
                } else {
                    // Saldo já está correto
                    String currencyName = amount == 1.0 ? 
                        plugin.getConfigManager().getCurrencyName() : 
                        plugin.getConfigManager().getCurrencyNamePlural();
                    
                    sendMessage(sender, "§aO saldo de §f" + target.getName() + " §ajá é §f" + String.format("%.2f", amount) + " " + currencyName + "§a.");
                }
            });
    }
    
    /**
     * Mostra os jogadores mais ricos
     */
    private void showTopPlayers(CommandSender sender) {
        sendMessage(sender, "§6=== Jogadores Mais Ricos ===");
        
        plugin.getAsyncMongoDBManager().getTopPlayers(10)
            .thenAccept(topPlayers -> {
                if (topPlayers.isEmpty()) {
                    sendMessage(sender, "§cNenhum jogador encontrado.");
                    return;
                }
                
                int position = 1;
                for (Document playerDoc : topPlayers) {
                    String playerName = playerDoc.getString("name");
                    double balance = playerDoc.getDouble("balance");
                    
                    String currencyName = balance == 1.0 ? 
                        plugin.getConfigManager().getCurrencyName() : 
                        plugin.getConfigManager().getCurrencyNamePlural();
                    
                    sendMessage(sender, "§e" + position + ". §f" + playerName + " §7- §f" + String.format("%.2f", balance) + " " + currencyName);
                    position++;
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
