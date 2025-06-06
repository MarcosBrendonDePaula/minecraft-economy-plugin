package com.minecraft.economy.listeners;

import com.minecraft.economy.core.EconomyPlugin;
import com.minecraft.economy.shop.ShopCategory;
import com.minecraft.economy.shop.ShopGUI;
import com.minecraft.economy.shop.ShopItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener para eventos de inventário relacionados à loja
 */
public class ShopListener implements Listener {

    private final EconomyPlugin plugin;
    private final ShopGUI shopGUI;
    private final Map<String, String> inventoryTitles = new HashMap<>();

    public ShopListener(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.shopGUI = new ShopGUI(plugin, plugin.getShopManager());
        
        // Registra os títulos de inventário para identificação
        inventoryTitles.put("§8Loja - Menu Principal", "main");
        inventoryTitles.put("§8Detalhes do Item", "details");
        inventoryTitles.put("§8Comprar Item", "buy");
        inventoryTitles.put("§8Vender Itens", "sell");
        
        // Adiciona títulos dinâmicos para categorias
        for (ShopCategory category : plugin.getShopManager().getCategories().values()) {
            inventoryTitles.put("§8Loja - " + category.getName(), "category:" + category.getId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        String title = event.getView().getTitle();
        
        // Verifica se é um inventário da loja
        if (isShopInventory(title)) {
            // Cancela o evento para evitar roubo de itens
            event.setCancelled(true);
            
            // Processa o clique com base no tipo de inventário
            String inventoryType = getInventoryType(title);
            ItemStack clickedItem = event.getCurrentItem();
            
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }
            
            // Processa cliques no menu principal
            if (inventoryType.equals("main")) {
                handleMainMenuClick(player, clickedItem, event.getSlot());
            }
            // Processa cliques em menus de categoria
            else if (inventoryType.startsWith("category:")) {
                String categoryId = inventoryType.substring(9);
                handleCategoryMenuClick(player, clickedItem, event.getSlot(), categoryId);
            }
            // Processa cliques no menu de detalhes do item
            else if (inventoryType.equals("details")) {
                handleDetailsMenuClick(player, clickedItem, event.getSlot());
            }
            // Processa cliques no menu de compra
            else if (inventoryType.equals("buy")) {
                handleBuyMenuClick(player, clickedItem, event.getSlot());
            }
            // Processa cliques no menu de venda
            else if (inventoryType.equals("sell")) {
                handleSellMenuClick(player, clickedItem, event.getSlot());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        
        // Verifica se é um inventário da loja
        if (isShopInventory(title)) {
            // Cancela o evento para evitar roubo de itens
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();
        
        // Verifica se algum dos inventários é da loja
        if (source.getType() == InventoryType.CHEST || destination.getType() == InventoryType.CHEST) {
            if (source.getHolder() != null && source.getHolder() instanceof Player) {
                Player player = (Player) source.getHolder();
                if (player.getOpenInventory() != null && isShopInventory(player.getOpenInventory().getTitle())) {
                    event.setCancelled(true);
                }
            }
            
            if (destination.getHolder() != null && destination.getHolder() instanceof Player) {
                Player player = (Player) destination.getHolder();
                if (player.getOpenInventory() != null && isShopInventory(player.getOpenInventory().getTitle())) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    /**
     * Verifica se um inventário pertence à loja
     * @param title Título do inventário
     * @return true se o inventário pertence à loja
     */
    private boolean isShopInventory(String title) {
        return title.startsWith("§8Loja") || inventoryTitles.containsKey(title);
    }
    
    /**
     * Obtém o tipo de inventário
     * @param title Título do inventário
     * @return Tipo de inventário
     */
    private String getInventoryType(String title) {
        if (inventoryTitles.containsKey(title)) {
            return inventoryTitles.get(title);
        }
        
        // Verifica se é um menu de categoria
        for (ShopCategory category : plugin.getShopManager().getCategories().values()) {
            if (title.equals("§8Loja - " + category.getName())) {
                return "category:" + category.getId();
            }
        }
        
        return "";
    }
    
    /**
     * Processa cliques no menu principal
     */
    private void handleMainMenuClick(Player player, ItemStack clickedItem, int slot) {
        // Botão de voltar (slot 45)
        if (slot == 45) {
            player.closeInventory();
            return;
        }
        
        // Botão de página anterior (slot 48)
        if (slot == 48) {
            // Implementar paginação
            return;
        }
        
        // Botão de página seguinte (slot 50)
        if (slot == 50) {
            // Implementar paginação
            return;
        }
        
        // Botão de pesquisa (slot 49)
        if (slot == 49) {
            // Implementar pesquisa
            return;
        }
        
        // Clique em uma categoria
        if (slot >= 10 && slot <= 44) {
            // Encontra a categoria clicada
            int index = 0;
            for (ShopCategory category : plugin.getShopManager().getCategories().values()) {
                int categorySlot = 10 + index;
                if ((categorySlot % 9) == 8) {
                    categorySlot += 2;
                }
                
                if (categorySlot == slot) {
                    plugin.getLogger().info("Abrindo categoria: " + category.getId() + " - " + category.getName());
                    shopGUI.openCategoryMenu(player, category.getId());
                    return;
                }
                
                index++;
            }
        }
    }
    
    /**
     * Processa cliques em menus de categoria
     */
    private void handleCategoryMenuClick(Player player, ItemStack clickedItem, int slot, String categoryId) {
        // Botão de voltar (slot 45)
        if (slot == 45) {
            shopGUI.openMainMenu(player);
            return;
        }
        
        // Botão de página anterior (slot 48)
        if (slot == 48) {
            // Implementar paginação
            return;
        }
        
        // Botão de página seguinte (slot 50)
        if (slot == 50) {
            // Implementar paginação
            return;
        }
        
        // Botão de pesquisa (slot 49)
        if (slot == 49) {
            // Implementar pesquisa
            return;
        }
        
        // Clique em um item da categoria
        if (slot >= 0 && slot < 45) {
            ShopCategory category = plugin.getShopManager().getCategory(categoryId);
            if (category == null) {
                plugin.getLogger().warning("Categoria não encontrada: " + categoryId);
                return;
            }
            
            // Encontra o item clicado
            int page = 0; // Implementar paginação adequada
            int startIndex = page * 45;
            int itemIndex = startIndex + slot;
            
            if (category.getItems().size() > 0 && itemIndex < category.getItems().size()) {
                ShopItem item = (ShopItem) category.getItems().values().toArray()[itemIndex];
                plugin.getLogger().info("Abrindo detalhes do item: " + item.getId() + " - " + item.getName());
                shopGUI.openItemDetails(player, item);
            } else {
                plugin.getLogger().warning("Item não encontrado no índice: " + itemIndex + " para categoria: " + categoryId);
                plugin.getLogger().warning("Total de itens na categoria: " + category.getItems().size());
            }
        }
    }
    
    /**
     * Processa cliques no menu de detalhes do item
     */
    private void handleDetailsMenuClick(Player player, ItemStack clickedItem, int slot) {
        // Botão de voltar (slot 18)
        if (slot == 18) {
            // Volta para o menu da categoria
            for (ShopCategory category : plugin.getShopManager().getCategories().values()) {
                shopGUI.openCategoryMenu(player, category.getId());
                return;
            }
            return;
        }
        
        // Botão de compra (slot 15)
        if (slot == 15) {
            // Abre o menu de compra
            ItemStack itemStack = player.getOpenInventory().getItem(13);
            if (itemStack != null) {
                for (ShopItem item : plugin.getShopManager().getAllItems().values()) {
                    if (item.getMaterial() == itemStack.getType()) {
                        shopGUI.openBuyMenu(player, item);
                        return;
                    }
                }
            }
        }
    }
    
    /**
     * Processa cliques no menu de compra
     */
    private void handleBuyMenuClick(Player player, ItemStack clickedItem, int slot) {
        // Botão de voltar (slot 18)
        if (slot == 18) {
            // Volta para o menu de detalhes
            ItemStack itemStack = player.getOpenInventory().getItem(13);
            if (itemStack != null) {
                for (ShopItem item : plugin.getShopManager().getAllItems().values()) {
                    if (item.getMaterial() == itemStack.getType()) {
                        shopGUI.openItemDetails(player, item);
                        return;
                    }
                }
            }
            return;
        }
        
        // Botões de compra
        ItemStack itemStack = player.getOpenInventory().getItem(13);
        if (itemStack != null) {
            ShopItem shopItem = null;
            for (ShopItem item : plugin.getShopManager().getAllItems().values()) {
                if (item.getMaterial() == itemStack.getType()) {
                    shopItem = item;
                    break;
                }
            }
            
            if (shopItem != null) {
                int amount = 0;
                
                // Botão de compra x1 (slot 10)
                if (slot == 10) {
                    amount = 1;
                }
                // Botão de compra x8 (slot 11)
                else if (slot == 11) {
                    amount = 8;
                }
                // Botão de compra x16 (slot 12)
                else if (slot == 12) {
                    amount = 16;
                }
                // Botão de compra x32 (slot 14)
                else if (slot == 14) {
                    amount = 32;
                }
                // Botão de compra x64 (slot 15)
                else if (slot == 15) {
                    amount = 64;
                }
                
                if (amount > 0) {
                    shopGUI.buyItem(player, shopItem, amount);
                }
            }
        }
    }
    
    /**
     * Processa cliques no menu de venda
     */
    private void handleSellMenuClick(Player player, ItemStack clickedItem, int slot) {
        // Botão de voltar (slot 45)
        if (slot == 45) {
            shopGUI.openMainMenu(player);
            return;
        }
        
        // Botão de página anterior (slot 48)
        if (slot == 48) {
            // Implementar paginação
            return;
        }
        
        // Botão de página seguinte (slot 50)
        if (slot == 50) {
            // Implementar paginação
            return;
        }
        
        // Clique em um item para vender
        if (slot >= 0 && slot < 45) {
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                for (ShopItem item : plugin.getShopManager().getAllItems().values()) {
                    if (item.getMaterial() == clickedItem.getType()) {
                        shopGUI.sellItem(player, item);
                        return;
                    }
                }
            }
        }
    }
}
