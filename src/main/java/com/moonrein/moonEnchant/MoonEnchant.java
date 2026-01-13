package com.moonrein.moonEnchant;

import com.moonrein.moonEnchant.command.EnchantCommand;
import com.moonrein.moonEnchant.enchant.EnchantConfigLoader;
import com.moonrein.moonEnchant.enchant.EnchantRegistry;
import com.moonrein.moonEnchant.listener.EnchantListener;
import com.moonrein.moonEnchant.model.EnchantDefinition;
import com.moonrein.moonEnchant.service.EnchantService;
import com.moonrein.moonEnchant.util.ItemEnchantStorage;
import java.io.File;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class MoonEnchant extends JavaPlugin {
    private EnchantRegistry registry;
    private EnchantService enchantService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        File enchantFolder = new File(getDataFolder(), "enchants");
        saveResource("enchants/rift_edge.yml", false);

        EnchantConfigLoader loader = new EnchantConfigLoader();
        registry = new EnchantRegistry();
        for (EnchantDefinition definition : loader.loadAll(enchantFolder)) {
            registry.register(definition);
        }

        ItemEnchantStorage storage = new ItemEnchantStorage(new NamespacedKey(this, "custom_enchants"));
        enchantService = new EnchantService(registry, storage);

        EnchantListener listener = new EnchantListener(enchantService);
        getServer().getPluginManager().registerEvents(listener, this);

        EnchantCommand command = new EnchantCommand(enchantService, registry, loader, enchantFolder);
        if (getCommand("ce") != null) {
            getCommand("ce").setExecutor(command);
            getCommand("ce").setTabCompleter(command);
        }

        getServer().getScheduler().runTaskTimer(this, enchantService::tickPassiveEffects, 20L, 20L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
