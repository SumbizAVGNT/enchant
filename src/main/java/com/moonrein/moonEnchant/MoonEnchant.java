package com.moonrein.moonEnchant;

import com.moonrein.moonEnchant.command.EnchantCommand;
import com.moonrein.moonEnchant.enchant.EnchantConfigLoader;
import com.moonrein.moonEnchant.enchant.EnchantRegistry;
import com.moonrein.moonEnchant.listener.EnchantListener;
import com.moonrein.moonEnchant.model.EnchantDefinition;
import com.moonrein.moonEnchant.service.EnchantService;
import com.moonrein.moonEnchant.util.ItemEnchantStorage;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class MoonEnchant extends JavaPlugin {
    private EnchantRegistry registry;
    private EnchantService enchantService;
    private ExecutorService enchantExecutor;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        File enchantFolder = new File(getDataFolder(), "enchants");
        String[] defaultEnchants = {
            "enchants/rift_edge.yml",
            "enchants/autoreel.yml",
            "enchants/replanter.yml",
            "enchants/twinge.yml",
            "enchants/jellylegs.yml",
            "enchants/experience.yml",
            "enchants/veinminer.yml",
            "enchants/hook.yml",
            "enchants/bait.yml",
            "enchants/poison.yml",
            "enchants/springs.yml",
            "enchants/perish.yml",
            "enchants/ambit.yml",
            "enchants/critical.yml",
            "enchants/inquisitive.yml",
            "enchants/lifesteal.yml",
            "enchants/overload.yml",
            "enchants/trench.yml",
            "enchants/telepathy.yml",
            "enchants/smelting.yml"
        };
        for (String resource : defaultEnchants) {
            saveResource(resource, false);
        }

        EnchantConfigLoader loader = new EnchantConfigLoader();
        registry = new EnchantRegistry();
        for (EnchantDefinition definition : loader.loadAll(enchantFolder)) {
            registry.register(definition);
        }

        ItemEnchantStorage storage = new ItemEnchantStorage(new NamespacedKey(this, "custom_enchants"));
        enchantExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            new EnchantThreadFactory()
        );
        enchantService = new EnchantService(this, registry, storage, enchantExecutor);

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
        if (enchantExecutor != null) {
            enchantExecutor.shutdownNow();
        }
    }

    private static class EnchantThreadFactory implements ThreadFactory {
        private final AtomicInteger index = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("moonEnchant-worker-" + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
