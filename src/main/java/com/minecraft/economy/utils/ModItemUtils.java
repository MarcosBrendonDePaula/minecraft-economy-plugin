package com.minecraft.economy.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;

/**
 * Utilitário para lidar com itens de mods
 */
public class ModItemUtils {

    /**
     * Cria um ItemStack a partir de um ID de item (suporta itens vanilla e de mods)
     * @param itemId ID do item (formato: MATERIAL ou namespace:key para itens de mods)
     * @return ItemStack criado, ou null se o item não for encontrado
     */
    public static ItemStack createItemStack(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        
        // Verifica se é um item de mod ou vanilla
        if (itemId.contains(":")) {
            // Item de mod (formato namespace:key)
            String[] parts = itemId.split(":");
            if (parts.length != 2) {
                return null;
            }
            
            String namespace = parts[0];
            String key = parts[1];
            
            // Tenta obter o material do registro
            NamespacedKey namespacedKey = new NamespacedKey(namespace, key);
            Material material = Registry.MATERIAL.get(namespacedKey);
            
            if (material != null) {
                return new ItemStack(material);
            }
            
            // Se não encontrar, retorna null
            return null;
        } else {
            // Item vanilla
            try {
                Material material = Material.valueOf(itemId.toUpperCase());
                return new ItemStack(material);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
    
    /**
     * Verifica se dois itens são do mesmo tipo (considerando itens de mods)
     * @param item1 Primeiro item
     * @param item2 Segundo item
     * @return true se os itens forem do mesmo tipo
     */
    public static boolean isSameItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }
        
        // Para itens vanilla, basta comparar o tipo
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        // Para itens de mods, pode ser necessário comparar mais detalhes
        // Dependendo do mod, pode ser necessário verificar NBT, metadata, etc.
        // Esta é uma implementação básica que funciona para a maioria dos casos
        
        if (item1.hasItemMeta() && item2.hasItemMeta()) {
            // Compara o display name, se existir
            if (item1.getItemMeta().hasDisplayName() && item2.getItemMeta().hasDisplayName()) {
                return item1.getItemMeta().getDisplayName().equals(item2.getItemMeta().getDisplayName());
            }
        }
        
        // Se não tiver metadata específica, considera apenas o tipo
        return true;
    }
    
    /**
     * Obtém o ID de um item (formato: MATERIAL ou namespace:key para itens de mods)
     * @param item ItemStack
     * @return ID do item
     */
    public static String getItemId(ItemStack item) {
        if (item == null) {
            return null;
        }
        
        Material material = item.getType();
        NamespacedKey key = material.getKey();
        
        // Se o namespace não for "minecraft", é um item de mod
        if (!key.getNamespace().equals("minecraft")) {
            return key.toString();
        }
        
        // Para itens vanilla, retorna apenas o nome do material
        return material.name();
    }
}
