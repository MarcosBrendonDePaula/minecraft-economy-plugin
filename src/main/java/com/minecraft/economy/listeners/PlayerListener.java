package com.minecraft.economy.listeners;

import com.minecraft.economy.core.EconomyPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final EconomyPlugin plugin;

    public PlayerListener(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        
        // Verifica se o jogador já tem uma conta, se não, cria uma
        plugin.getMongoDBManager().hasAccount(uuid).thenAccept(hasAccount -> {
            if (!hasAccount) {
                double initialBalance = plugin.getConfig().getDouble("economy.starting_balance", 1000.0);
                plugin.getMongoDBManager().createAccount(uuid, playerName, initialBalance).thenAccept(created -> {
                    if (created) {
                        plugin.getLogger().info("Conta criada para o jogador " + playerName);
                        
                        // Notifica o jogador sobre sua nova conta
                        String message = plugin.getConfig().getString("plugin.prefix", "&8[&6Economia&8] &r") + 
                                         "&aBem-vindo! Uma conta foi criada para você com &f" + 
                                         plugin.getEconomyProvider().format(initialBalance) + "&a.";
                        
                        player.sendMessage(message.replace("&", "§"));
                    }
                });
            } else {
                // Atualiza o nome do jogador se necessário
                plugin.getMongoDBManager().getPlayersCollection().updateOne(
                    new org.bson.Document("uuid", uuid.toString()),
                    new org.bson.Document("$set", new org.bson.Document("name", playerName))
                );
            }
        });
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Podemos implementar lógica adicional quando o jogador sair, se necessário
        // Por exemplo, salvar dados em cache para o banco de dados
    }
}
