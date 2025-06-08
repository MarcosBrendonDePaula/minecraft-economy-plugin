package com.minecraft.economy.listeners;

import com.minecraft.economy.core.EconomyPlugin;
import com.minecraft.economy.playershop.PlayerShop;
import com.minecraft.economy.playershop.PlayerShopGUI;
import com.minecraft.economy.playershop.PlayerShopItem;
import com.minecraft.economy.playershop.PlayerShopManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listener para eventos de chat relacionados às lojas de jogadores
 */
public class PlayerChatListener implements Listener {

    private final EconomyPlugin plugin;
    private final PlayerShopManager shopManager;
    private final PlayerShopGUI shopGUI;
    
    // Mapas para controlar o estado de cada jogador
    private final Map<UUID, ChatState> playerChatStates = new HashMap<>();
    private final Map<UUID, PlayerShop> pendingShops = new HashMap<>();
    private final Map<UUID, PlayerShopItem> pendingItems = new HashMap<>();
    private final Map<UUID, ItemStack> pendingAddItems = new HashMap<>();
    private final Map<UUID, Boolean> pendingDynamicPrices = new HashMap<>();
    
    // Estados possíveis para o chat
    public enum ChatState {
        NONE,
        CREATING_SHOP,
        CONFIRMING_DELETE_SHOP,
        SETTING_ITEM_PRICE,
        MANAGING_ITEM
    }

    public PlayerChatListener(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.shopManager = plugin.getPlayerShopManager();
        this.shopGUI = new PlayerShopGUI(plugin, shopManager);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        String message = event.getMessage();
        
        // Verifica se o jogador está em algum estado de chat
        ChatState state = playerChatStates.getOrDefault(playerId, ChatState.NONE);
        if (state == ChatState.NONE) {
            return;
        }
        
        // Cancela o evento para não mostrar a mensagem no chat
        event.setCancelled(true);
        
        // Processa a mensagem com base no estado
        switch (state) {
            case CREATING_SHOP:
                handleShopCreation(player, message);
                break;
            case CONFIRMING_DELETE_SHOP:
                handleShopDeletion(player, message);
                break;
            case SETTING_ITEM_PRICE:
                handleItemPriceSet(player, message);
                break;
            case MANAGING_ITEM:
                handleItemManagement(player, message);
                break;
        }
    }
    
    /**
     * Define o estado de chat de um jogador
     * @param player Jogador
     * @param state Estado de chat
     */
    public void setChatState(Player player, ChatState state) {
        playerChatStates.put(player.getUniqueId(), state);
    }
    
    /**
     * Define a loja pendente de um jogador
     * @param player Jogador
     * @param shop Loja
     */
    public void setPendingShop(Player player, PlayerShop shop) {
        pendingShops.put(player.getUniqueId(), shop);
    }
    
    /**
     * Define o item pendente de um jogador
     * @param player Jogador
     * @param item Item
     */
    public void setPendingItem(Player player, PlayerShopItem item) {
        pendingItems.put(player.getUniqueId(), item);
    }
    
    /**
     * Define o item a ser adicionado de um jogador
     * @param player Jogador
     * @param item Item
     */
    public void setPendingAddItem(Player player, ItemStack item) {
        pendingAddItems.put(player.getUniqueId(), item);
    }
    
    /**
     * Define se o preço pendente é dinâmico
     * @param player Jogador
     * @param isDynamic Se o preço é dinâmico
     */
    public void setPendingDynamicPrice(Player player, boolean isDynamic) {
        pendingDynamicPrices.put(player.getUniqueId(), isDynamic);
    }
    
    /**
     * Processa a criação de uma loja
     * @param player Jogador
     * @param message Mensagem
     */
    private void handleShopCreation(Player player, String message) {
        // Limpa o estado
        playerChatStates.remove(player.getUniqueId());
        
        // Verifica se o jogador cancelou
        if (message.equalsIgnoreCase("cancelar")) {
            player.sendMessage("§cCriação de loja cancelada.");
            return;
        }
        
        // Cria a loja
        new BukkitRunnable() {
            @Override
            public void run() {
                shopManager.createPlayerShop(player, message, null)
                    .thenAccept(success -> {
                        if (success) {
                            player.sendMessage("§aLoja criada com sucesso: §f" + message);
                            
                            // Abre o menu principal
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    shopGUI.openMainMenu(player);
                                }
                            }.runTask(plugin);
                        }
                    });
            }
        }.runTask(plugin);
    }
    
    /**
     * Processa a exclusão de uma loja
     * @param player Jogador
     * @param message Mensagem
     */
    private void handleShopDeletion(Player player, String message) {
        // Limpa o estado
        playerChatStates.remove(player.getUniqueId());
        
        // Verifica se o jogador cancelou
        if (message.equalsIgnoreCase("cancelar")) {
            player.sendMessage("§cExclusão de loja cancelada.");
            
            // Abre o menu de gerenciamento
            PlayerShop shop = pendingShops.remove(player.getUniqueId());
            if (shop != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        shopGUI.openShopManagementMenu(player, shop);
                    }
                }.runTask(plugin);
            }
            return;
        }
        
        // Verifica se o jogador confirmou
        if (message.equalsIgnoreCase("confirmar")) {
            PlayerShop shop = pendingShops.remove(player.getUniqueId());
            if (shop != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        shopManager.deletePlayerShop(shop.getId().toString(), player);
                        shopGUI.openMainMenu(player);
                    }
                }.runTask(plugin);
            }
        } else {
            player.sendMessage("§cComando inválido. Exclusão de loja cancelada.");
        }
    }
    
    /**
     * Processa a definição de preço de um item
     * @param player Jogador
     * @param message Mensagem
     */
    private void handleItemPriceSet(Player player, String message) {
        // Limpa o estado
        playerChatStates.remove(player.getUniqueId());
        
        // Verifica se o jogador cancelou
        if (message.equalsIgnoreCase("cancelar")) {
            player.sendMessage("§cAdição de item cancelada.");
            
            // Abre o menu de gerenciamento
            PlayerShop shop = pendingShops.remove(player.getUniqueId());
            pendingAddItems.remove(player.getUniqueId());
            pendingDynamicPrices.remove(player.getUniqueId());
            
            if (shop != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        shopGUI.openShopManagementMenu(player, shop);
                    }
                }.runTask(plugin);
            }
            return;
        }
        
        // Tenta converter o preço
        double price;
        try {
            price = Double.parseDouble(message.replace(",", "."));
        } catch (NumberFormatException e) {
            player.sendMessage("§cPreço inválido. Adição de item cancelada.");
            return;
        }
        
        // Verifica se o preço é válido
        if (price <= 0) {
            player.sendMessage("§cO preço deve ser maior que zero. Adição de item cancelada.");
            return;
        }
        
        // Adiciona o item à loja
        PlayerShop shop = pendingShops.remove(player.getUniqueId());
        ItemStack item = pendingAddItems.remove(player.getUniqueId());
        boolean isDynamic = pendingDynamicPrices.remove(player.getUniqueId());
        
        if (shop != null && item != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    shopGUI.addItemToShop(player, shop, item, price, isDynamic);
                }
            }.runTask(plugin);
        }
    }
    
    /**
     * Processa o gerenciamento de um item
     * @param player Jogador
     * @param message Mensagem
     */
    private void handleItemManagement(Player player, String message) {
        // Limpa o estado
        playerChatStates.remove(player.getUniqueId());
        
        // Verifica se o jogador cancelou
        if (message.equalsIgnoreCase("cancelar")) {
            player.sendMessage("§cOperação cancelada.");
            
            // Abre o menu de gerenciamento
            PlayerShop shop = pendingShops.remove(player.getUniqueId());
            pendingItems.remove(player.getUniqueId());
            
            if (shop != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        shopGUI.openShopManagementMenu(player, shop);
                    }
                }.runTask(plugin);
            }
            return;
        }
        
        // Obtém a loja e o item
        PlayerShop shop = pendingShops.get(player.getUniqueId());
        PlayerShopItem item = pendingItems.get(player.getUniqueId());
        
        if (shop == null || item == null) {
            player.sendMessage("§cOcorreu um erro. Operação cancelada.");
            return;
        }
        
        // Processa o comando
        if (message.equalsIgnoreCase("remover")) {
            // Remove o item da loja
            new BukkitRunnable() {
                @Override
                public void run() {
                    shopManager.removeItemFromShop(player, shop.getId().toString(), item.getId().toString());
                    shopGUI.openShopManagementMenu(player, shop);
                }
            }.runTask(plugin);
        } else if (message.toLowerCase().startsWith("preço ")) {
            // Altera o preço do item
            try {
                String priceStr = message.substring(6).trim().replace(",", ".");
                double price = Double.parseDouble(priceStr);
                
                if (price <= 0) {
                    player.sendMessage("§cO preço deve ser maior que zero.");
                    return;
                }
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        shopManager.setItemPrice(player, shop.getId().toString(), item.getId().toString(), price);
                        shopGUI.openShopManagementMenu(player, shop);
                    }
                }.runTask(plugin);
            } catch (Exception e) {
                player.sendMessage("§cPreço inválido. Formato correto: preço <valor>");
            }
        } else if (message.equalsIgnoreCase("dinâmico sim")) {
            // Ativa o preço dinâmico
            new BukkitRunnable() {
                @Override
                public void run() {
                    item.setDynamicPrice(true);
                    shop.saveAsync();
                    player.sendMessage("§aPreço dinâmico ativado para este item.");
                    shopGUI.openShopManagementMenu(player, shop);
                }
            }.runTask(plugin);
        } else if (message.equalsIgnoreCase("dinâmico não")) {
            // Desativa o preço dinâmico
            new BukkitRunnable() {
                @Override
                public void run() {
                    item.setDynamicPrice(false);
                    shop.saveAsync();
                    player.sendMessage("§aPreço dinâmico desativado para este item.");
                    shopGUI.openShopManagementMenu(player, shop);
                }
            }.runTask(plugin);
        } else if (message.toLowerCase().startsWith("estoque adicionar ")) {
            // Adiciona itens ao estoque
            try {
                String amountStr = message.substring(17).trim();
                int amount = Integer.parseInt(amountStr);
                
                if (amount <= 0) {
                    player.sendMessage("§cA quantidade deve ser maior que zero.");
                    return;
                }
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Verifica se o jogador tem o item no inventário
                        ItemStack itemInInventory = null;
                        for (ItemStack invItem : player.getInventory().getContents()) {
                            if (invItem != null && invItem.isSimilar(item.getItemStack())) {
                                itemInInventory = invItem;
                                break;
                            }
                        }
                        
                        if (itemInInventory == null || itemInInventory.getAmount() < amount) {
                            player.sendMessage("§cVocê não tem itens suficientes no inventário.");
                            shopGUI.openShopManagementMenu(player, shop);
                            return;
                        }
                        
                        // Remove os itens do inventário
                        ItemStack toRemove = itemInInventory.clone();
                        toRemove.setAmount(amount);
                        player.getInventory().removeItem(toRemove);
                        
                        // Adiciona ao estoque
                        item.addStock(amount);
                        shop.saveAsync();
                        
                        player.sendMessage("§aAdicionados §f" + amount + " §aitens ao estoque. Novo estoque: §f" + item.getQuantity());
                        shopGUI.openShopManagementMenu(player, shop);
                    }
                }.runTask(plugin);
            } catch (NumberFormatException e) {
                player.sendMessage("§cQuantidade inválida. Formato correto: estoque adicionar <quantidade>");
            }
        } else if (message.toLowerCase().startsWith("estoque remover ")) {
            // Remove itens do estoque
            try {
                String amountStr = message.substring(16).trim();
                int amount = Integer.parseInt(amountStr);
                
                if (amount <= 0) {
                    player.sendMessage("§cA quantidade deve ser maior que zero.");
                    return;
                }
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Verifica se há estoque suficiente
                        if (!item.hasStock(amount)) {
                            player.sendMessage("§cNão há estoque suficiente. Estoque atual: §f" + item.getQuantity());
                            shopGUI.openShopManagementMenu(player, shop);
                            return;
                        }
                        
                        // Remove do estoque
                        item.removeStock(amount);
                        
                        // Dá os itens ao jogador
                        ItemStack toGive = item.getItemStack().clone();
                        toGive.setAmount(amount);
                        player.getInventory().addItem(toGive);
                        
                        shop.saveAsync();
                        
                        player.sendMessage("§aRemovidos §f" + amount + " §aitens do estoque. Novo estoque: §f" + item.getQuantity());
                        shopGUI.openShopManagementMenu(player, shop);
                    }
                }.runTask(plugin);
            } catch (NumberFormatException e) {
                player.sendMessage("§cQuantidade inválida. Formato correto: estoque remover <quantidade>");
            }
        } else {
            player.sendMessage("§cComando inválido. Operação cancelada.");
            
            // Abre o menu de gerenciamento
            new BukkitRunnable() {
                @Override
                public void run() {
                    shopGUI.openShopManagementMenu(player, shop);
                }
            }.runTask(plugin);
        }
        
        // Limpa os mapas
        pendingShops.remove(player.getUniqueId());
        pendingItems.remove(player.getUniqueId());
    }
}
