package com.minecraft.economy.playershop;

import com.minecraft.economy.core.EconomyPlugin;
import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gerenciador de lojas de jogadores
 */
public class PlayerShopManager {

    private final EconomyPlugin plugin;
    private final Map<String, PlayerShop> playerShops = new ConcurrentHashMap<>();
    private final Map<UUID, List<PlayerShop>> playerShopsByOwner = new ConcurrentHashMap<>();

    public PlayerShopManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        loadPlayerShops();
    }

    /**
     * Carrega todas as lojas de jogadores do banco de dados
     */
    private void loadPlayerShops() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    plugin.getLogger().info("Carregando lojas de jogadores...");
                    
                    playerShops.clear();
                    playerShopsByOwner.clear();
                    
                    plugin.getMongoDBManager().getDatabase().getCollection("player_shops")
                        .find()
                        .forEach(doc -> {
                            try {
                                PlayerShop shop = new PlayerShop(plugin, doc);
                                playerShops.put(shop.getId().toString(), shop);
                                
                                // Adiciona à lista de lojas do jogador
                                UUID ownerUUID = shop.getOwnerUUID();
                                playerShopsByOwner.computeIfAbsent(ownerUUID, k -> new ArrayList<>()).add(shop);
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.SEVERE, "Erro ao carregar loja de jogador: " + e.getMessage(), e);
                            }
                        });
                    
                    plugin.getLogger().info("Lojas de jogadores carregadas com sucesso: " + playerShops.size() + " lojas");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao carregar lojas de jogadores: " + e.getMessage(), e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Cria uma nova loja para um jogador
     * @param player Jogador que está criando a loja
     * @param shopName Nome da loja
     * @param location Localização da loja (opcional)
     * @return CompletableFuture com o resultado da operação
     */
    public CompletableFuture<Boolean> createPlayerShop(Player player, String shopName, Location location) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        UUID playerUUID = player.getUniqueId();
        String playerName = player.getName();
        
        // Verifica se o jogador já atingiu o limite de lojas
        int maxShops = plugin.getConfig().getInt("playershop.max_shops_per_player", 3);
        List<PlayerShop> playerShops = getPlayerShopsByOwner(playerUUID);
        if (playerShops.size() >= maxShops) {
            player.sendMessage("§cVocê já atingiu o limite de lojas (" + maxShops + ").");
            future.complete(false);
            return future;
        }
        
        // Verifica se o jogador tem dinheiro suficiente para criar uma loja
        double creationCost = plugin.getConfig().getDouble("playershop.creation_cost", 1000.0);
        if (creationCost > 0) {
            plugin.getMongoDBManager().hasBalance(playerUUID, creationCost).thenAccept(hasMoney -> {
                if (!hasMoney) {
                    player.sendMessage("§cVocê não tem dinheiro suficiente para criar uma loja. Custo: " + 
                                      plugin.getEconomyProvider().format(creationCost));
                    future.complete(false);
                    return;
                }
                
                // Cobra o jogador
                plugin.getMongoDBManager().withdraw(playerUUID, creationCost, "Criação de loja: " + shopName)
                    .thenAccept(success -> {
                        if (!success) {
                            player.sendMessage("§cOcorreu um erro ao processar o pagamento.");
                            future.complete(false);
                            return;
                        }
                        
                        // Cria a loja
                        createShop(player, shopName, location, future);
                    });
            });
        } else {
            // Cria a loja sem cobrar
            createShop(player, shopName, location, future);
        }
        
        return future;
    }
    
    /**
     * Método auxiliar para criar uma loja
     */
    private void createShop(Player player, String shopName, Location location, CompletableFuture<Boolean> future) {
        UUID playerUUID = player.getUniqueId();
        String playerName = player.getName();
        
        // Cria a loja
        PlayerShop shop = new PlayerShop(plugin, playerUUID, playerName, shopName, location);
        
        // Salva a loja no banco de dados
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Document doc = shop.toDocument();
                    plugin.getMongoDBManager().getDatabase().getCollection("player_shops")
                        .insertOne(doc);
                    
                    // Adiciona à lista de lojas
                    playerShops.put(shop.getId().toString(), shop);
                    playerShopsByOwner.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(shop);
                    
                    // Notifica o jogador
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage("§aLoja criada com sucesso: §f" + shopName);
                            future.complete(true);
                        }
                    }.runTask(plugin);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao salvar loja de jogador: " + e.getMessage(), e);
                    
                    // Notifica o jogador
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage("§cOcorreu um erro ao criar a loja. Tente novamente mais tarde.");
                            future.complete(false);
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Deleta uma loja de jogador
     * @param shopId ID da loja
     * @param player Jogador que está deletando (deve ser o dono ou um administrador)
     * @return true se a loja foi deletada com sucesso
     */
    public boolean deletePlayerShop(String shopId, Player player) {
        PlayerShop shop = playerShops.get(shopId);
        if (shop == null) {
            player.sendMessage("§cLoja não encontrada.");
            return false;
        }
        
        // Verifica se o jogador é o dono ou um administrador
        if (!shop.getOwnerUUID().equals(player.getUniqueId()) && 
            !player.hasPermission("economy.playershop.admin")) {
            player.sendMessage("§cVocê não tem permissão para deletar esta loja.");
            return false;
        }
        
        // Remove a loja
        playerShops.remove(shopId);
        List<PlayerShop> ownerShops = playerShopsByOwner.get(shop.getOwnerUUID());
        if (ownerShops != null) {
            ownerShops.removeIf(s -> s.getId().toString().equals(shopId));
        }
        
        // Deleta a loja do banco de dados
        shop.delete();
        
        player.sendMessage("§aLoja deletada com sucesso.");
        return true;
    }

    /**
     * Adiciona um item à loja de um jogador
     * @param player Jogador que está adicionando o item
     * @param shopId ID da loja
     * @param itemStack Item a ser adicionado
     * @param price Preço do item
     * @return true se o item foi adicionado com sucesso
     */
    public boolean addItemToShop(Player player, String shopId, ItemStack itemStack, double price) {
        PlayerShop shop = playerShops.get(shopId);
        if (shop == null) {
            player.sendMessage("§cLoja não encontrada.");
            return false;
        }
        
        // Verifica se o jogador é o dono
        if (!shop.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("§cVocê não é o dono desta loja.");
            return false;
        }
        
        // Verifica se o preço é válido
        if (price <= 0) {
            player.sendMessage("§cO preço deve ser maior que zero.");
            return false;
        }
        
        // Cria o item da loja
        PlayerShopItem shopItem = new PlayerShopItem(plugin, itemStack, price);
        
        // Adiciona o item à loja
        if (!shop.addItem(shopItem)) {
            player.sendMessage("§cA loja já atingiu o limite de itens.");
            return false;
        }
        
        // Remove o item do inventário do jogador
        player.getInventory().removeItem(itemStack);
        
        // Salva a loja no banco de dados
        shop.saveAsync();
        
        player.sendMessage("§aItem adicionado à loja com sucesso.");
        return true;
    }

    /**
     * Remove um item da loja de um jogador
     * @param player Jogador que está removendo o item
     * @param shopId ID da loja
     * @param itemId ID do item
     * @return true se o item foi removido com sucesso
     */
    public boolean removeItemFromShop(Player player, String shopId, String itemId) {
        PlayerShop shop = playerShops.get(shopId);
        if (shop == null) {
            player.sendMessage("§cLoja não encontrada.");
            return false;
        }
        
        // Verifica se o jogador é o dono
        if (!shop.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("§cVocê não é o dono desta loja.");
            return false;
        }
        
        // Remove o item da loja
        PlayerShopItem item = shop.removeItem(itemId);
        if (item == null) {
            player.sendMessage("§cItem não encontrado.");
            return false;
        }
        
        // Devolve o item ao jogador se ainda estiver disponível
        if (item.isAvailable()) {
            player.getInventory().addItem(item.createItemStack());
        }
        
        // Salva a loja no banco de dados
        shop.saveAsync();
        
        player.sendMessage("§aItem removido da loja com sucesso.");
        return true;
    }

    /**
     * Altera o preço de um item na loja
     * @param player Jogador que está alterando o preço
     * @param shopId ID da loja
     * @param itemId ID do item
     * @param newPrice Novo preço
     * @return true se o preço foi alterado com sucesso
     */
    public boolean setItemPrice(Player player, String shopId, String itemId, double newPrice) {
        PlayerShop shop = playerShops.get(shopId);
        if (shop == null) {
            player.sendMessage("§cLoja não encontrada.");
            return false;
        }
        
        // Verifica se o jogador é o dono
        if (!shop.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("§cVocê não é o dono desta loja.");
            return false;
        }
        
        // Verifica se o preço é válido
        if (newPrice <= 0) {
            player.sendMessage("§cO preço deve ser maior que zero.");
            return false;
        }
        
        // Obtém o item
        PlayerShopItem item = shop.getItem(itemId);
        if (item == null) {
            player.sendMessage("§cItem não encontrado.");
            return false;
        }
        
        // Altera o preço
        item.setPrice(newPrice);
        
        // Salva a loja no banco de dados
        shop.saveAsync();
        
        player.sendMessage("§aPreço alterado com sucesso para §f" + plugin.getEconomyProvider().format(newPrice) + "§a.");
        return true;
    }

    /**
     * Obtém uma loja pelo ID
     * @param shopId ID da loja
     * @return A loja, ou null se não foi encontrada
     */
    public PlayerShop getPlayerShop(String shopId) {
        return playerShops.get(shopId);
    }

    /**
     * Obtém todas as lojas de um jogador
     * @param ownerUUID UUID do jogador
     * @return Lista de lojas do jogador
     */
    public List<PlayerShop> getPlayerShopsByOwner(UUID ownerUUID) {
        return playerShopsByOwner.getOrDefault(ownerUUID, new ArrayList<>());
    }

    /**
     * Obtém todas as lojas
     * @return Mapa de lojas
     */
    public Map<String, PlayerShop> getAllPlayerShops() {
        return new HashMap<>(playerShops);
    }
}
