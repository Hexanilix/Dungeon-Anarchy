package org.hexils.dnarch;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.hetils.mpdl.Logger;
import org.hetils.mpdl.PluginThread;
import org.hetils.mpdl.GeneralListener;
import org.hexils.dnarch.commands.DungeonAnarchyCommandExecutor;
import org.hexils.dnarch.commands.DungeonCreatorCommandExecutor;
import org.hexils.dnarch.commands.DungeonCommandExecutor;

import java.util.logging.Level;

public final class Main extends JavaPlugin {
    public static Logger logger;
    public static JavaPlugin plugin;
    public static String name = "dungeon_anarchy";

    public static void log(String s) {logger.log(s); }
    public static void log(Object o) { logger.log(o); }
    public static void log(Level level, String s) { logger.log(level, s); }
    public static void log(Level level, Object o) { logger.log(level, o); }

    public static final ItemStack wand;
    static {
        wand = new ItemStack(Material.STICK);
        ItemMeta m = wand.getItemMeta();
        assert m != null;
        m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        m.addEnchant(Enchantment.CHANNELING, 1, true);
        m.setDisplayName(ChatColor.DARK_PURPLE + "Dungeon Master's Wand");
        wand.setItemMeta(m);
    }

    private void loadCommands() {
        if (Bukkit.getPluginCommand("dungeon_anarchy") != null) {
            Bukkit.getPluginCommand("dungeon_anarchy").setExecutor(new DungeonAnarchyCommandExecutor());
            Bukkit.getPluginCommand("dungeon_anarchy").setTabCompleter(new DungeonAnarchyCommandExecutor.tab());
        } else {
            log(Level.SEVERE, "THE PLUGIN COMMAND \"/dungeon_anarchy\" WASN'T LOADED!!! This won't impact existing dungeons and only impacts dungeon creating and management. Please restart the server or contact the developer.");
        }
        if (Bukkit.getPluginCommand("dc") != null) {
            Bukkit.getPluginCommand("dc").setExecutor(new DungeonCreatorCommandExecutor());
            Bukkit.getPluginCommand("dc").setTabCompleter(new DungeonCreatorCommandExecutor.tab());
        } else log(Level.SEVERE, "THE PLUGIN COMMAND \"/dc\" WASN'T LOADED!!! This won't impact existing dungeons and only impacts dungeon creating and management. Please restart the server or contact the developer.");
        if (Bukkit.getPluginCommand("da") != null) {
            Bukkit.getPluginCommand("da").setExecutor(new DungeonAnarchyCommandExecutor());
            Bukkit.getPluginCommand("da").setTabCompleter(new DungeonAnarchyCommandExecutor.tab());
        } else log(Level.SEVERE, "THE PLUGIN COMMAND \"/da\" WASN'T LOADED!!! This won't impact existing dungeons and only impacts dungeon creating and management. Please restart the server or contact the developer.");
        if (Bukkit.getPluginCommand("dungeon") != null) {
            Bukkit.getPluginCommand("dungeon").setExecutor(new DungeonCommandExecutor());
            Bukkit.getPluginCommand("dungeon").setTabCompleter(new DungeonCommandExecutor.Tab());
        } else log(Level.SEVERE, "THE PLUGIN COMMAND \"/dungeon\" WASN'T LOADED!!! This won't impact existing dungeons and only impacts dungeon creating and management. Please restart the server or contact the developer.");
        if (Bukkit.getPluginCommand("dg") != null) {
            Bukkit.getPluginCommand("dg").setExecutor(new DungeonCommandExecutor());
            Bukkit.getPluginCommand("dg").setTabCompleter(new DungeonCommandExecutor.Tab());
        } else log(Level.SEVERE, "THE PLUGIN COMMAND \"/dg\" WASN'T LOADED!!! This won't impact existing dungeons and only impacts dungeon creating and management. Please restart the server or contact the developer.");
    }

    @Override
    public void onEnable() {
        logger = new Logger(this.getName());
        plugin = this;
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(new MainListener(), this);
        Bukkit.getPluginManager().registerEvents(new GeneralListener(), this);
        Bukkit.getPluginManager().registerEvents(new Manageable.ManagableListener(), this);
        loadCommands();
    }

    @Override
    public void onDisable() {
        PluginThread.finish();
        super.onDisable();
    }
}
