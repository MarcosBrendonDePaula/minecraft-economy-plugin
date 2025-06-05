package com.minecraft.economy.shop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um item na loja
 */
public class ShopItem {

    private final String id;
    private final String name;
    private final ItemStack itemStack;
    private final String description;
    private final double basePrice;
    private double currentPrice;
    private int stock;
    private final ShopCategory category;

    /**
     * Construtor completo do item
     * @param id ID do item
     * @param name Nome do item
     * @param itemStack ItemStack do item
     * @param description Descrição do item
     * @param basePrice Preço base do item
     * @param currentPrice Preço atual do item
     * @param stock Estoque do item
     */
    public ShopItem(String id, String name, ItemStack itemStack, String description, double basePrice, double currentPrice, int stock) {
        this.id = id;
        this.name = name;
        this.itemStack = itemStack;
        this.description = description;
        this.basePrice = basePrice;
        this.currentPrice = currentPrice;
        this.stock = stock;
        this.category = null; // Sem categoria
    }

    /**
     * Construtor simplificado para compatibilidade
     * @param id ID do item
     * @param name Nome do item
     * @param material Material do item
     * @param basePrice Preço base do item
     * @param category Categoria do item
     */
    public ShopItem(String id, String name, Material material, double basePrice, ShopCategory category) {
        this.id = id;
        this.name = name;
        this.itemStack = new ItemStack(material);
        this.description = "";
        this.basePrice = basePrice;
        this.currentPrice = basePrice;
        this.stock = 0;
        this.category = category;
    }

    /**
     * Obtém o ID do item
     * @return ID do item
     */
    public String getId() {
        return id;
    }
    
    /**
     * Obtém o ID do item (método alternativo para compatibilidade)
     * @return ID do item
     */
    public String getItemId() {
        return id;
    }

    /**
     * Obtém o nome do item
     * @return Nome do item
     */
    public String getName() {
        return name;
    }
    
    /**
     * Obtém o nome de exibição do item
     * @return Nome de exibição do item
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * Obtém o ItemStack do item
     * @return ItemStack do item
     */
    public ItemStack getItemStack() {
        return itemStack.clone();
    }
    
    /**
     * Cria um ItemStack com a quantidade especificada
     * @param amount Quantidade de itens
     * @return ItemStack com a quantidade especificada
     */
    public ItemStack createItemStack(int amount) {
        ItemStack stack = itemStack.clone();
        stack.setAmount(amount);
        
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a" + name);
            
            List<String> lore = new ArrayList<>();
            if (!description.isEmpty()) {
                lore.add("§7" + description);
            }
            lore.add("§7Preço: §f" + String.format("%.2f", currentPrice));
            lore.add("§7Estoque: §f" + stock);
            
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        
        return stack;
    }

    /**
     * Obtém o material do item
     * @return Material do item
     */
    public Material getMaterial() {
        return itemStack.getType();
    }

    /**
     * Obtém a descrição do item
     * @return Descrição do item
     */
    public String getDescription() {
        return description;
    }

    /**
     * Obtém o preço base do item
     * @return Preço base do item
     */
    public double getBasePrice() {
        return basePrice;
    }

    /**
     * Obtém o preço atual do item
     * @return Preço atual do item
     */
    public double getCurrentPrice() {
        return currentPrice;
    }

    /**
     * Define o preço atual do item
     * @param currentPrice Novo preço atual
     */
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    /**
     * Obtém o estoque do item
     * @return Estoque do item
     */
    public int getStock() {
        return stock;
    }

    /**
     * Define o estoque do item
     * @param stock Novo estoque
     */
    public void setStock(int stock) {
        this.stock = stock;
    }

    /**
     * Obtém a categoria do item
     * @return Categoria do item
     */
    public ShopCategory getCategory() {
        return category;
    }
}
