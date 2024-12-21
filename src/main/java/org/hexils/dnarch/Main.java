package org.hexils.dnarch;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.hetils.jgl17.oodp.OODP;
import org.hetils.mpdl.*;
import org.hexils.dnarch.commands.DungeonAnarchyCommandExecutor;
import org.hexils.dnarch.commands.DungeonCreatorCommandExecutor;
import org.hexils.dnarch.commands.DungeonCommandExecutor;
import org.hexils.dnarch.dungeon.Dungeon;

import java.util.*;
import java.util.logging.Level;

public final class Main extends JavaPlugin {
    public static Logger logger;
    public static JavaPlugin plugin;
    public static String name = "dungeon_anarchy";

    public static OODP dp;

    static {
        dp = new OODP();
        dp.excludeFieldsFor(Manageable.class, "gui", "aboveManageable", "underManageable", "name_sign", "renameable");
        dp.addConvertingFunction(Manageable.NameGetter.class, Manageable.NameGetter::getName);
        dp.addCreatingFunction(Manageable.NameGetter.class, om -> {
            String s = om.getString("name");
            return () -> s;
        });

        dp.addConvertingFunction(Material.class, Enum::name);

        dp.addHashConvertingFunction(NSK.class, nsk -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put("key", nsk.key.toString());
            map.put("type", NSK.getDataTypeName(nsk.type));
            return map;
        });
        dp.addCreatingFunction(NSK.class, om -> {
            log("NSK???" + NSK.getDataType(om.getString("type")).equals(PersistentDataType.BOOLEAN));
            return new NSK<>(new NamespacedKey(Main.plugin, om.getString("key").split(":")[1]), NSK.getDataType(om.getString("type")));
        });

        dp.processAs("org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack", ItemStack.class);
        //TODO enchants
        dp.addHashConvertingFunction(ItemStack.class, (item) -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put("amount", item.getAmount());
            map.put("material", item.getType().name());
            ItemMeta m = item.getItemMeta();
            if (m != null) {
                if (m.hasLocalizedName()) map.put("localized_name", m.getLocalizedName());
                if (m.hasDisplayName()) map.put("display_name", m.getDisplayName());
                if (m.hasCustomModelData()) map.put("custom_model_data", m.getCustomModelData());
                if (m.hasLore()) map.put("lore", m.getLore());
//                if (m.hasEnchants()) {
//                    Map<Enchantment, Integer> enchs = m.getEnchants();
//                    for (Map.Entry<Enchantment, Integer> e : enchs.entrySet()) {
//
//                    }
//                }
                PersistentDataContainer dc = m.getPersistentDataContainer();
                if (!dc.isEmpty()) {
                    Set<NamespacedKey> keys = dc.getKeys();
                    int size = keys.size();
                    NSK[] nsks = new NSK[size];
                    Object[] values = new Object[size];
                    int i = 0;
                    for (NamespacedKey key : keys) {
                        nsks[i] = new NSK<>(key, NSK.getDataType(dc, key));
                        log(nsks[i]);
                        values[i] = NSK.getNSK(dc, nsks[i]);
                        i++;
                    }
                    map.put("nsks", nsks);
                    map.put("values", values);
                }
            }
            return map;
        });

        dp.addCreatingFunction(ItemStack.class, om -> {
            ItemStack item = new ItemStack(Material.getMaterial(om.getString("material")), om.get("amount", int.class, 1));
            ItemMeta m = item.getItemMeta();
            if (m != null) {
                if (om.has("localized_name")) m.setLocalizedName(om.getString("localized_name"));
                if (om.has("display_name")) m.setDisplayName(om.getString("display_name"));
                if (om.has("custom_model_data")) ItemUtil.setCustomData(item, om.getInt("custom_model_data"));
                if (om.has("lore")) m.setLore(List.of(om.getStringArr("lore")));
                if (om.has("nsks")) {
                    NSK[] nsks = om.getObjectArr("nsks", NSK.class);
                    Object[] vals = new Object[nsks.length];
                    OODP.ObjectiveMap rv = om.getObjectiveMap("values");
                    for (int i = 0; i < nsks.length; i++)
                        vals[i] = rv.get(String.valueOf(i), nsks[i].type.getComplexType());
                    NSK.setNSK(m, nsks, vals);
                }
                item.setItemMeta(m);
            }
            return item;
        });
        dp.excludeFieldsFor(Dungeon.class, "editors", "sector_gui_list", "dungeon_event_list", "entranceLocation", "running", "mains", "dungeon_start", "viewers", "players", "world", "bounding_box");
        dp.excludeFieldsFor(Dungeon.Section.class, "sectionEnter", "event_gui", "item_gui", "items", "bounds", "displ_mat");
    }

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
        plugin = this;
        logger = new Logger(this.getName());
        this.getDataFolder().mkdir();
        FileManager.dungeon_dir.mkdir();
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
