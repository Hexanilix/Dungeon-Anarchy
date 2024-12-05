package org.hexils.dnarch;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.hetils.mpdl.General;
import org.hetils.mpdl.PluginThread;
import org.hetils.mpdl.listener.GeneralListener;
import org.hexils.dnarch.commands.da_cmd;
import org.hexils.dnarch.commands.dc_cmd;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.logging.Level;

import static org.hetils.mpdl.General.log;

public final class Main extends JavaPlugin {
    public static Plugin plugin;

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
            Bukkit.getPluginCommand("dungeon_anarchy").setExecutor(new da_cmd());
            Bukkit.getPluginCommand("dungeon_anarchy").setTabCompleter(new da_cmd.tab());
        } else {
            log(Level.SEVERE, "THE PLUGIN COMMAND \"/da\" WASN'T LOADED!!! This won't impact existing dungeons and only really impacts dungeon creating and management. Please restart the server or contact the developer.");
        }
        if (Bukkit.getPluginCommand("dc") != null) {
            Bukkit.getPluginCommand("dc").setExecutor(new dc_cmd());
            Bukkit.getPluginCommand("dc").setTabCompleter(new dc_cmd.tab());
        } else log(Level.SEVERE, "THE PLUGIN COMMAND \"/dc\" WASN'T LOADED!!! This won't impact existing dungeons and only really impacts dungeon creating and management. Please restart the server or contact the developer.");
        if (Bukkit.getPluginCommand("da") != null) {
            Bukkit.getPluginCommand("da").setExecutor(new da_cmd());
            Bukkit.getPluginCommand("da").setTabCompleter(new da_cmd.tab());
        } else log(Level.SEVERE, "THE PLUGIN COMMAND \"/da\" WASN'T LOADED!!! This won't impact existing dungeons and only really impacts dungeon creating and management. Please restart the server or contact the developer.");
    }

    @Override
    public void onEnable() {
        General.setPluginName("DungeonAnarchy");
        plugin = this;
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(new MainListener(), this);
        Bukkit.getPluginManager().registerEvents(new GeneralListener(), this);
        loadCommands();
    }

    @Override
    public void onDisable() {
        PluginThread.finish();
        super.onDisable();
    }
}
