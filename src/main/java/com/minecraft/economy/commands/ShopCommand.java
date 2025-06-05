package com.minecraft.economy.commands;

import com.minecraft.economy.core.EconomyPlugin;
import com.minecraft.economy.shop.ShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para abrir a loja
 */
public class ShopCommand implements CommandExecutor {

    private final EconomyPlugin plugin;
    private final ShopGUI shopGUI;

    /**
     * Construtor do comando de loja
     * @param plugin Instância do plugin
     */
    public ShopCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.shopGUI = new ShopGUI(plugin, plugin.getShopManager());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Abre o menu principal da loja
            shopGUI.openMainMenu(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "sell":
                // Abre o menu de venda
                shopGUI.openSellMenu(player);
                break;
            case "category":
                if (args.length < 2) {
                    player.sendMessage("§cUso correto: /shop category <categoria>");
                    return true;
                }
                
                String categoryId = args[1].toLowerCase();
                shopGUI.openCategoryMenu(player, categoryId);
                break;
            default:
                player.sendMessage("§cComando desconhecido. Use /shop para abrir a loja ou /shop sell para vender itens.");
                break;
        }
        
        return true;
    }
}
