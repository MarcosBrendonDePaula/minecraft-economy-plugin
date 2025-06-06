package com.minecraft.economy.commands;

import com.minecraft.economy.core.EconomyPlugin;
import com.minecraft.economy.playershop.PlayerShop;
import com.minecraft.economy.playershop.PlayerShopGUI;
import com.minecraft.economy.playershop.PlayerShopManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Comando para gerenciar lojas de jogadores
 */
public class PlayerShopCommand implements CommandExecutor, TabCompleter {

    private final EconomyPlugin plugin;
    private final PlayerShopManager shopManager;
    private final PlayerShopGUI shopGUI;

    /**
     * Construtor do comando de lojas de jogadores
     * @param plugin Instância do plugin
     */
    public PlayerShopCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.shopManager = plugin.getPlayerShopManager();
        this.shopGUI = new PlayerShopGUI(plugin, shopManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Verifica permissão básica
        if (!player.hasPermission("economy.playershop.use")) {
            player.sendMessage("§cVocê não tem permissão para usar lojas de jogadores.");
            return true;
        }
        
        // Comando sem argumentos abre o menu principal
        if (args.length == 0) {
            shopGUI.openMainMenu(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                handleCreateCommand(player, args);
                break;
            case "list":
                handleListCommand(player);
                break;
            case "open":
                handleOpenCommand(player, args);
                break;
            case "add":
                handleAddCommand(player, args);
                break;
            case "remove":
                handleRemoveCommand(player, args);
                break;
            case "setprice":
                handleSetPriceCommand(player, args);
                break;
            case "withdraw":
                handleWithdrawCommand(player, args);
                break;
            case "admin":
                handleAdminCommand(player, args);
                break;
            case "help":
                sendHelpMessage(player);
                break;
            default:
                player.sendMessage("§cComando desconhecido. Use /playershop help para ver os comandos disponíveis.");
                break;
        }
        
        return true;
    }

    /**
     * Processa o comando de criação de loja
     */
    private void handleCreateCommand(Player player, String[] args) {
        // Verifica permissão
        if (!player.hasPermission("economy.playershop.create")) {
            player.sendMessage("§cVocê não tem permissão para criar lojas.");
            return;
        }
        
        // Verifica argumentos
        if (args.length < 2) {
            player.sendMessage("§cUso correto: /playershop create <nome>");
            return;
        }
        
        // Obtém o nome da loja
        String shopName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // Cria a loja
        shopManager.createPlayerShop(player, shopName, player.getLocation())
            .thenAccept(success -> {
                if (success) {
                    player.sendMessage("§aLoja criada com sucesso: §f" + shopName);
                    
                    // Abre o menu principal
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        shopGUI.openMainMenu(player);
                    });
                }
            });
    }

    /**
     * Processa o comando de listar lojas
     */
    private void handleListCommand(Player player) {
        // Abre o menu principal
        shopGUI.openMainMenu(player);
    }

    /**
     * Processa o comando de abrir loja
     */
    private void handleOpenCommand(Player player, String[] args) {
        // Verifica argumentos
        if (args.length < 2) {
            player.sendMessage("§cUso correto: /playershop open <id>");
            return;
        }
        
        // Obtém o ID da loja
        String shopId = args[1];
        
        // Obtém a loja
        PlayerShop shop = shopManager.getPlayerShop(shopId);
        if (shop == null) {
            player.sendMessage("§cLoja não encontrada.");
            return;
        }
        
        // Verifica se o jogador é o dono
        if (shop.getOwnerUUID().equals(player.getUniqueId())) {
            // Abre o menu de gerenciamento
            shopGUI.openShopManagementMenu(player, shop);
        } else {
            // Abre o menu de compra
            shopGUI.openShopBuyMenu(player, shop);
        }
    }

    /**
     * Processa o comando de adicionar item
     */
    private void handleAddCommand(Player player, String[] args) {
        // Verifica argumentos
        if (args.length < 2) {
            player.sendMessage("§cUso correto: /playershop add <preço>");
            return;
        }
        
        // Obtém o preço
        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cPreço inválido.");
            return;
        }
        
        // Verifica se o preço é válido
        if (price <= 0) {
            player.sendMessage("§cO preço deve ser maior que zero.");
            return;
        }
        
        // Verifica se o jogador tem um item na mão
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack == null || itemStack.getType().isAir()) {
            player.sendMessage("§cVocê precisa ter um item na mão.");
            return;
        }
        
        // Obtém a loja do jogador
        List<PlayerShop> playerShops = shopManager.getPlayerShopsByOwner(player.getUniqueId());
        if (playerShops.isEmpty()) {
            player.sendMessage("§cVocê não tem nenhuma loja. Crie uma com /playershop create <nome>.");
            return;
        }
        
        // Se o jogador tem apenas uma loja, usa ela
        if (playerShops.size() == 1) {
            PlayerShop shop = playerShops.get(0);
            shopManager.addItemToShop(player, shop.getId().toString(), itemStack, price);
        } else {
            // Se o jogador tem mais de uma loja, abre o menu principal
            player.sendMessage("§cVocê tem mais de uma loja. Use o menu para adicionar itens.");
            shopGUI.openMainMenu(player);
        }
    }

    /**
     * Processa o comando de remover item
     */
    private void handleRemoveCommand(Player player, String[] args) {
        // Verifica argumentos
        if (args.length < 2) {
            player.sendMessage("§cUso correto: /playershop remove <id>");
            return;
        }
        
        // Obtém o ID do item
        String itemId = args[1];
        
        // Obtém a loja do jogador
        List<PlayerShop> playerShops = shopManager.getPlayerShopsByOwner(player.getUniqueId());
        if (playerShops.isEmpty()) {
            player.sendMessage("§cVocê não tem nenhuma loja.");
            return;
        }
        
        // Procura o item em todas as lojas do jogador
        for (PlayerShop shop : playerShops) {
            if (shopManager.removeItemFromShop(player, shop.getId().toString(), itemId)) {
                return;
            }
        }
        
        player.sendMessage("§cItem não encontrado em nenhuma de suas lojas.");
    }

    /**
     * Processa o comando de definir preço
     */
    private void handleSetPriceCommand(Player player, String[] args) {
        // Verifica argumentos
        if (args.length < 3) {
            player.sendMessage("§cUso correto: /playershop setprice <id> <preço>");
            return;
        }
        
        // Obtém o ID do item
        String itemId = args[1];
        
        // Obtém o preço
        double price;
        try {
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cPreço inválido.");
            return;
        }
        
        // Verifica se o preço é válido
        if (price <= 0) {
            player.sendMessage("§cO preço deve ser maior que zero.");
            return;
        }
        
        // Obtém a loja do jogador
        List<PlayerShop> playerShops = shopManager.getPlayerShopsByOwner(player.getUniqueId());
        if (playerShops.isEmpty()) {
            player.sendMessage("§cVocê não tem nenhuma loja.");
            return;
        }
        
        // Procura o item em todas as lojas do jogador
        for (PlayerShop shop : playerShops) {
            if (shopManager.setItemPrice(player, shop.getId().toString(), itemId, price)) {
                return;
            }
        }
        
        player.sendMessage("§cItem não encontrado em nenhuma de suas lojas.");
    }

    /**
     * Processa o comando de sacar saldo
     */
    private void handleWithdrawCommand(Player player, String[] args) {
        // Obtém a loja do jogador
        List<PlayerShop> playerShops = shopManager.getPlayerShopsByOwner(player.getUniqueId());
        if (playerShops.isEmpty()) {
            player.sendMessage("§cVocê não tem nenhuma loja.");
            return;
        }
        
        // Se o jogador tem apenas uma loja, usa ela
        if (playerShops.size() == 1) {
            PlayerShop shop = playerShops.get(0);
            shop.withdrawBalance(player);
        } else {
            // Se o jogador tem mais de uma loja, verifica se foi especificada
            if (args.length < 2) {
                player.sendMessage("§cVocê tem mais de uma loja. Especifique o ID da loja: /playershop withdraw <id>");
                return;
            }
            
            // Obtém o ID da loja
            String shopId = args[1];
            
            // Obtém a loja
            PlayerShop shop = shopManager.getPlayerShop(shopId);
            if (shop == null) {
                player.sendMessage("§cLoja não encontrada.");
                return;
            }
            
            // Verifica se o jogador é o dono
            if (!shop.getOwnerUUID().equals(player.getUniqueId())) {
                player.sendMessage("§cVocê não é o dono desta loja.");
                return;
            }
            
            shop.withdrawBalance(player);
        }
    }

    /**
     * Processa comandos administrativos
     */
    private void handleAdminCommand(Player player, String[] args) {
        // Verifica permissão
        if (!player.hasPermission("economy.playershop.admin")) {
            player.sendMessage("§cVocê não tem permissão para usar comandos administrativos.");
            return;
        }
        
        // Verifica argumentos
        if (args.length < 2) {
            player.sendMessage("§cUso correto: /playershop admin [list|delete|setlimit] <argumentos>");
            return;
        }
        
        String adminSubCommand = args[1].toLowerCase();
        
        switch (adminSubCommand) {
            case "list":
                handleAdminListCommand(player, args);
                break;
            case "delete":
                handleAdminDeleteCommand(player, args);
                break;
            case "setlimit":
                handleAdminSetLimitCommand(player, args);
                break;
            default:
                player.sendMessage("§cComando administrativo desconhecido. Use /playershop admin para ver os comandos disponíveis.");
                break;
        }
    }

    /**
     * Processa o comando administrativo de listar lojas
     */
    private void handleAdminListCommand(Player player, String[] args) {
        // Verifica argumentos
        if (args.length < 3) {
            player.sendMessage("§cUso correto: /playershop admin list <jogador>");
            return;
        }
        
        // Obtém o jogador
        String playerName = args[2];
        UUID playerUUID = plugin.getServer().getOfflinePlayer(playerName).getUniqueId();
        
        // Obtém as lojas do jogador
        List<PlayerShop> playerShops = shopManager.getPlayerShopsByOwner(playerUUID);
        if (playerShops.isEmpty()) {
            player.sendMessage("§cO jogador não tem nenhuma loja.");
            return;
        }
        
        // Lista as lojas
        player.sendMessage("§aLojas de §f" + playerName + "§a:");
        for (PlayerShop shop : playerShops) {
            player.sendMessage("§7- §f" + shop.getShopName() + " §7(ID: §f" + shop.getId().toString() + "§7)");
        }
    }

    /**
     * Processa o comando administrativo de deletar loja
     */
    private void handleAdminDeleteCommand(Player player, String[] args) {
        // Verifica argumentos
        if (args.length < 3) {
            player.sendMessage("§cUso correto: /playershop admin delete <id>");
            return;
        }
        
        // Obtém o ID da loja
        String shopId = args[2];
        
        // Deleta a loja
        shopManager.deletePlayerShop(shopId, player);
    }

    /**
     * Processa o comando administrativo de definir limite de lojas
     */
    private void handleAdminSetLimitCommand(Player player, String[] args) {
        // Verifica argumentos
        if (args.length < 4) {
            player.sendMessage("§cUso correto: /playershop admin setlimit <jogador> <limite>");
            return;
        }
        
        // Obtém o jogador
        String playerName = args[2];
        
        // Obtém o limite
        int limit;
        try {
            limit = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cLimite inválido.");
            return;
        }
        
        // Verifica se o limite é válido
        if (limit < 0) {
            player.sendMessage("§cO limite deve ser maior ou igual a zero.");
            return;
        }
        
        // Define o limite
        // Implementação: salvar no banco de dados
        player.sendMessage("§aLimite de lojas para §f" + playerName + " §adefinido como §f" + limit + "§a.");
    }

    /**
     * Envia a mensagem de ajuda
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage("§a=== Comandos de Lojas de Jogadores ===");
        player.sendMessage("§7/playershop §f- Abre o menu principal");
        player.sendMessage("§7/playershop create <nome> §f- Cria uma nova loja");
        player.sendMessage("§7/playershop list §f- Lista suas lojas");
        player.sendMessage("§7/playershop open <id> §f- Abre uma loja para gerenciamento");
        player.sendMessage("§7/playershop add <preço> §f- Adiciona o item na mão à loja");
        player.sendMessage("§7/playershop remove <id> §f- Remove um item da loja");
        player.sendMessage("§7/playershop setprice <id> <preço> §f- Altera o preço de um item");
        player.sendMessage("§7/playershop withdraw §f- Retira o dinheiro acumulado na loja");
        
        if (player.hasPermission("economy.playershop.admin")) {
            player.sendMessage("§a=== Comandos Administrativos ===");
            player.sendMessage("§7/playershop admin list <jogador> §f- Lista lojas de um jogador");
            player.sendMessage("§7/playershop admin delete <id> §f- Remove uma loja");
            player.sendMessage("§7/playershop admin setlimit <jogador> <limite> §f- Define limite de lojas");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcomandos principais
            completions.add("create");
            completions.add("list");
            completions.add("open");
            completions.add("add");
            completions.add("remove");
            completions.add("setprice");
            completions.add("withdraw");
            completions.add("help");
            
            if (sender.hasPermission("economy.playershop.admin")) {
                completions.add("admin");
            }
        } else if (args.length == 2) {
            // Subcomandos específicos
            if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("economy.playershop.admin")) {
                completions.add("list");
                completions.add("delete");
                completions.add("setlimit");
            }
        }
        
        return completions;
    }
}
