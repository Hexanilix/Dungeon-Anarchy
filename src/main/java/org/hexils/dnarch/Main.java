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
import org.hexils.dnarch.commands.da_cmd;
import org.hexils.dnarch.commands.dc_cmd;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.logging.Level;

public final class Main extends JavaPlugin {
    public static Plugin plugin;

    public static void log(Object msg) {
        log(Level.INFO, msg);
    }
    /**
     * The plugin console logger. This method sends the string value of {@code msg} through the
     * Bukkit plugin with the disclosure that the message was sent by this plugin, in the form of a string
     * in front of the message: <i>{@code [DA]}</i>
     *
     * @param msg The object converted to a string to send
     * @param lv The level of the message
     */
    public static void log(Level lv, Object msg) {
        Bukkit.getLogger().log(lv, "[DA] " + msg);
    }

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
        plugin = this;
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(new MainListener(), this);
        loadCommands();
    }

    public static ItemMeta addGlint(ItemMeta meta) {
        if (meta != null) {
            meta.addEnchant(Enchantment.LUCK, 1, false);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return meta;
    }

    public static ItemStack addGlint(ItemStack item) {
        if (item != null && item.hasItemMeta()) item.setItemMeta(addGlint(item.getItemMeta()));
        return item;
    }

    public static String toReadableString(Material material) {
        if (material == null) return null;
        String[] spl = material.name().toLowerCase().split("_");
        StringBuilder s = new StringBuilder().append((char) (spl[0].charAt(0) - 32)).append(spl[0], 1, spl[0].length());
        for (int i = 1; i < spl.length; i++) {
            String st = spl[i];
            s.append(' ').append(((char) (st.charAt(0) - 32))).append(st, 1, st.length());
        }
        return s.toString();
    }

    public static void displayAllParticles(@NotNull Player p) {
        new PluginThread() {
            @Override
            public void run() {
                Location l = p.getEyeLocation().add(5, 0, -20);
                double sz = l.getZ();
                Particle[] pl = Particle.values();
                try {
                    while (this.r) {
                        int i = 0;
                        int y;
                        while (i < pl.length) {
                            y = ((i * 2) / 40);
                            try {
                                p.getWorld().spawnParticle(pl[i], l.getX(), y + l.getY(), ((double) ((i * 2) % 40) + sz) + l.getZ(), 1, 0, 0, 0, 0);
                            } catch (Exception ignore) { log("ex: " + ignore.getLocalizedMessage()); }
                            i += 1;
                        }
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {}
            }
        }.start();
    }
    public static void dispao(Player p) {
        Location l = p.getEyeLocation().add(5, 0, -20);
        double sz = l.getZ();
        Particle[] pl = Particle.values();
        log(Arrays.toString(pl));
        int i = 0;
        int y;
        while (i < pl.length) {
            y = ((i * 2) / 40);
            try {
                p.getWorld().spawnParticle(pl[i], l.getX(), y + l.getY(), ((double) ((i * 2) % 40) + sz) + l.getZ(), 1, 0, 0, 0, 0);
            } catch (Exception ignore) { log("ex: " + ignore.getLocalizedMessage()); }
            i += 1;
        }
    }

    @Override
    public void onDisable() {
        PluginThread.finish();
        super.onDisable();
    }
}
