package com.moonrein.moonEnchant.command;

import com.moonrein.moonEnchant.enchant.EnchantConfigLoader;
import com.moonrein.moonEnchant.enchant.EnchantRegistry;
import com.moonrein.moonEnchant.model.EnchantDefinition;
import com.moonrein.moonEnchant.service.EnchantService;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EnchantCommand implements CommandExecutor, TabCompleter {
    private final EnchantService service;
    private final EnchantRegistry registry;
    private final EnchantConfigLoader loader;
    private final File enchantFolder;

    public EnchantCommand(EnchantService service, EnchantRegistry registry, EnchantConfigLoader loader, File enchantFolder) {
        this.service = service;
        this.registry = registry;
        this.loader = loader;
        this.enchantFolder = enchantFolder;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "MoonEnchant commands: give, list, info, reload, debug, test");
            return true;
        }
        return switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            case "debug" -> handleDebug(sender);
            case "test" -> handleTest(sender);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                yield true;
            }
        };
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.List.of("give", "list", "info", "reload", "debug", "test");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return registry.getAll().stream().map(EnchantDefinition::getId).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return registry.getAll().stream().map(EnchantDefinition::getId).toList();
        }
        return java.util.Collections.emptyList();
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /ce give <player> <enchant> [level]");
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        Optional<EnchantDefinition> definition = registry.getById(args[2]);
        if (definition.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Enchant not found.");
            return true;
        }
        int level = 1;
        if (args.length >= 4) {
            try {
                level = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
                level = 1;
            }
        }
        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            sender.sendMessage(ChatColor.RED + "Target is not holding an item.");
            return true;
        }
        Map<String, Integer> enchants = new HashMap<>(service.getItemEnchantments(item));
        enchants.put(definition.get().getId(), Math.min(level, definition.get().getMaxLevel()));
        service.setItemEnchantments(item, enchants);
        target.getInventory().setItemInMainHand(item);
        service.refreshPlayer(target);
        sender.sendMessage(ChatColor.GREEN + "Applied " + definition.get().getName() + " to item.");
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ce info <enchant>");
            return true;
        }
        Optional<EnchantDefinition> definition = registry.getById(args[1]);
        if (definition.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Enchant not found.");
            return true;
        }
        EnchantDefinition info = definition.get();
        sender.sendMessage(ChatColor.AQUA + "Enchant info: " + info.getName());
        sender.sendMessage(ChatColor.GRAY + "ID: " + info.getId());
        sender.sendMessage(ChatColor.GRAY + "Rarity: " + info.getRarity());
        sender.sendMessage(ChatColor.GRAY + "Max level: " + info.getMaxLevel());
        sender.sendMessage(ChatColor.GRAY + "Enchant slots: " + info.getEnchantSlotCost());
        if (info.getTableRequirement().enabled()) {
            sender.sendMessage(ChatColor.GRAY + "Table level: " + info.getTableRequirement().minLevel()
                + "-" + info.getTableRequirement().maxLevel());
            sender.sendMessage(ChatColor.GRAY + "Min bookshelves: " + info.getTableRequirement().minBookshelves());
        } else {
            sender.sendMessage(ChatColor.GRAY + "Table: disabled");
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "Registered enchants:");
        for (EnchantDefinition definition : registry.getAll()) {
            sender.sendMessage(ChatColor.GRAY + "- " + definition.getId() + " (" + definition.getName() + ")");
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        registry.clear();
        for (EnchantDefinition definition : loader.loadAll(enchantFolder)) {
            registry.register(definition);
        }
        sender.sendMessage(ChatColor.GREEN + "MoonEnchant configs reloaded.");
        return true;
    }

    private boolean handleDebug(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this.");
            return true;
        }
        service.toggleDebug(player);
        return true;
    }

    private boolean handleTest(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this.");
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        Map<String, Integer> enchants = service.getItemEnchantments(item);
        if (enchants.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No custom enchants on held item.");
            return true;
        }
        sender.sendMessage(ChatColor.AQUA + "Held item enchants:");
        enchants.forEach((id, level) -> sender.sendMessage(ChatColor.GRAY + "- " + id + " " + level));
        return true;
    }
}
