package com.minecraft.economy.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa uma categoria de itens na loja
 */
public class ShopCategory {

    private final String id;
    private final String name;
    private final Material icon;
    private final Map<String, ShopItem> items = new HashMap<>();

    /**
     * Construtor da categoria
     * @param id ID da categoria
     * @param name Nome da categoria
     * @param icon Material do ícone da categoria
     */
    public ShopCategory(String id, String name, Material icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    /**
     * Construtor simplificado para compatibilidade
     * @param id ID da categoria
     */
    public ShopCategory(String id) {
        this.id = id;
        this.name = id;
        this.icon = Material.CHEST;
    }

    /**
     * Obtém o ID da categoria
     * @return ID da categoria
     */
    public String getId() {
        return id;
    }

    /**
     * Obtém o nome da categoria
     * @return Nome da categoria
     */
    public String getName() {
        return name;
    }

    /**
     * Obtém o material do ícone da categoria
     * @return Material do ícone
     */
    public Material getIcon() {
        return icon;
    }
    
    /**
     * Adiciona um item à categoria
     * @param item Item a ser adicionado
     */
    public void addItem(ShopItem item) {
        items.put(item.getId(), item);
    }
    
    /**
     * Remove um item da categoria
     * @param itemId ID do item a ser removido
     */
    public void removeItem(String itemId) {
        items.remove(itemId);
    }
    
    /**
     * Obtém um item da categoria pelo ID
     * @param itemId ID do item
     * @return Item encontrado ou null se não existir
     */
    public ShopItem getItem(String itemId) {
        return items.get(itemId);
    }
    
    /**
     * Obtém todos os itens da categoria
     * @return Mapa de itens da categoria
     */
    public Map<String, ShopItem> getItems() {
        return items;
    }
    
    /**
     * Obtém a lista de itens da categoria
     * @return Lista de itens da categoria
     */
    public List<ShopItem> getItemsList() {
        return new ArrayList<>(items.values());
    }
}
