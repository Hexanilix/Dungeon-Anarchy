package org.hexils.dnarch;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.hetils.jgl17.Pair;
import org.hetils.jgl17.oodp.OODP;
import org.hetils.mpdl.*;
import org.hexils.dnarch.commands.DungeonAnarchyCommandExecutor;
import org.hexils.dnarch.commands.DungeonCreatorCommandExecutor;
import org.hexils.dnarch.commands.DungeonCommandExecutor;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

public final class Main extends JavaPlugin {
    public static Logger logger;
    public static JavaPlugin plugin;
    public static String name = "dungeon_anarchy";

    public static OODP dp;

    static {
        dp = new OODP();

//        dp.customCheck(o -> {
//            if (o.getClass().getName().startsWith("org.bukkit.craftbukkit.v1_20_R2.block.")) {
//                return Block.class;
//            }
//            return null;
//        });

        dp.autoExcludeFields(false);

        dp.excludeFieldsFor(Manageable.class, "gui", "aboveManageable", "underManageable", "name_sign", "name", "renameable", "is_being_renamed");
        dp.excludeFieldsFor(Dungeon.class,
                "editors",
                "sector_gui_list",
                "dungeon_event_list",
                "entranceLocation",
                "running",
                "dungeon_start",
                "viewers",
                "players",
                "bounding_box"
                );
        dp.excludeFieldsFor(Dungeon.Section.class, "sectionEnter", "event_gui", "item_gui", "items");
        dp.excludeFieldsFor(DAItem.class, "item", "items");

        dp.processAs("org.bukkit.craftbukkit.v1_20_R2.CraftWorld", World.class);
        dp.convertClassFunc(World.class, WorldInfo::getUID);
        dp.createClassFunc(World.class, om -> Bukkit.getWorld(om.getUUID("id")));

        dp.convertClassFunc(Manageable.NameGetter.class, Manageable.NameGetter::getName);
        dp.createClassFunc(Manageable.NameGetter.class, om -> {
            String s = om.getString("name");
            return () -> s;
        });

        dp.hashConvertClassFunc(NSK.class, nsk -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put("key", nsk.key.toString());
            map.put("type", NSK.getDataTypeName(nsk.type));
            return map;
        });
        dp.createClassFunc(NSK.class, om -> new NSK<>(new NamespacedKey(Main.plugin, om.getString("key").split(":")[1]), NSK.getDataType(om.getString("type"))));

        dp.convertClassExtendingFunc(Entity.class, Entity::getUniqueId);

        //TODO make version compatible
        dp.processAs("org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack", ItemStack.class);
        //TODO enchants
        dp.hashConvertClassFunc(ItemStack.class, (item) -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put("amount", item.getAmount());
            map.put("material", item.getType());
            ItemMeta m = item.getItemMeta();
            if (m != null) {
                if (m.hasLocalizedName()) map.put("localized_name", m.getLocalizedName());
                if (m.hasDisplayName()) map.put("display_name", m.getDisplayName());
                if (m.hasCustomModelData()) map.put("custom_model_data", m.getCustomModelData());
                if (m.hasLore()) map.put("lore", m.getLore());
                if (m.hasEnchants()) map.put("enchants", m.getEnchants());
                PersistentDataContainer dc = m.getPersistentDataContainer();
                if (!dc.isEmpty()) {
                    HashMap<NSK<?, ?>, Object> nskm = new HashMap<>();
                    Set<NamespacedKey> keys = dc.getKeys();
                    for (NamespacedKey key : keys) {
                        NSK<?, ?> nsk = new NSK<>(key, NSK.getDataType(dc, key));
                        nskm.put(nsk, NSK.getNSK(dc, nsk));
                    }
                    map.put("nsks", nskm);
                }
            }
            return map;
        });
        dp.createClassFunc(ItemStack.class, om -> {
            ItemStack item = new ItemStack(om.getAs("mat", Material.class), om.get("amount", int.class, 1));
            ItemMeta m = item.getItemMeta();
            if (m != null) {
                if (om.has("localized_name")) m.setLocalizedName(om.getString("localized_name"));
                if (om.has("display_name")) m.setDisplayName(om.getString("display_name"));
                if (om.has("custom_model_data")) ItemUtil.setCustomData(item, om.getInt("custom_model_data"));
                if (om.has("lore")) m.setLore(List.of(om.getStringArr("lore")));
                if (om.has("enchantments")) {

                }
                if (om.has("nsks")) {
                    //TODO use getAs instead of map
                    Map<NSK, Object> nskm = om.getObjectiveMap("nsks").asHashMap(NSK.class, Object.class);
                    for (Map.Entry<NSK, Object> e : nskm.entrySet())
                        NSK.setNSK(m, e.getKey(), e.getValue().getClass() == UUID.class ? e.getValue().toString() : e.getValue());
                }
                item.setItemMeta(m);
            }
            return item;
        });

        dp.convertClassFunc(LocationUtil.BoundingBox.class, bb -> new double[]{bb.getMinX(), bb.getMinY(), bb.getMinZ(), bb.getMaxX(), bb.getMaxY(), bb.getMaxZ()});

//        dp.convertClassFunc(Type.class, t -> t.getClass().getName());

        dp.hashConvertClassExtendingFunc(Location.class, l -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put("world", l.getWorld());
            map.put("x", l.getX());
            map.put("y", l.getY());
            map.put("z", l.getZ());
            map.put("yaw", l.getYaw());
            map.put("pitch", l.getPitch());
            return map;
        });
        dp.createClassFunc(Location.class, om -> new Location(Bukkit.getWorld(om.getUUID("world")), om.getDouble("x"), om.getDouble("y"), om.getDouble("z"), om.getFloat("yaw"), om.getFloat("pitch")));
        dp.processAs("org.bukkit.craftbukkit.v1_20_R2.block.CraftBlock", Block.class);
        dp.convertClassFunc(Block.class, b -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put("world", b.getWorld());
            map.put("x", b.getX());
            map.put("y", b.getY());
            map.put("z", b.getZ());
            return map;
        });
        dp.createClassFunc(Block.class, om -> Bukkit.getWorld(om.getUUID("world")).getBlockAt(om.getInt("x"), om.getInt("y"), om.getInt("z")));

        dp.hashConvertClassFunc(Dungeon.Section.class, s -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getName());
            map.put("bounds", s.getBounds());
            map.put("section_enter_id", s.getWhithinBoundCondition().getId());
            //TODO figure out how to check if class is List<?>
            map.put("items", s.getItems());
            return map;
        });
        dp.hashConvertClassFunc(Dungeon.class, d -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put("world", d.getWorld().getUID());
            map.put("name", d.getName());
            map.put("dungeon_start_id", d.getDungeonStart().getId());
            map.put("bounding_box", d.getBoundingBox());
            map.put("dungeon_info", d.getDungeonInfo());
            map.put("mains_id", d.getMains().getId());
            map.put("sections", d.getSections());
            return map;
        });

        dp.processFieldsExtendingClass(DAItem.class, item -> item.getId().toString());

        dp.createClassFunc(Dungeon.class, om -> {
            if (om.isEmpty()) return null;
            World w = Bukkit.getWorld(om.getUUID("world"));
            double[] ar = om.getDoubleArr("bounding_box");
            Dungeon d = null;
            try {
                d = new Dungeon(om.getString("name"), w, new LocationUtil.BoundingBox(new Pair<>(new Location(w, ar[0], ar[1], ar[2]), new Location(w, ar[3], ar[4], ar[5]))), om.getAs("dungeon_info", Dungeon.DungeonInfo.class));
                d.getDungeonStart().setId(om.getUUID("dungeon_start_id"));
                Dungeon finalD = d;
                dp.createClassFunc(Dungeon.Section.class, som -> {
                    try {
                        double[] a = som.getDoubleArr("bounds");
                        LocationUtil.BoundingBox bb = new LocationUtil.BoundingBox(new Pair<>(new Location(w, a[0], a[1], a[2]), new Location(w, a[3], a[4], a[5])));
                        List<DAItem> items = som.getObjectiveList("items").stream().map(im -> (DAItem) im.as(Type.get(im.getString("type")).getDAClass())).toList();
                        Dungeon.Section s = finalD.newSection(som.getUUID("id"), som.getString("name"), items, bb);
                        DAItem i = s.getWhithinBoundCondition();
                        i.setId(som.getUUID("section_enter_id"));
                        return s;
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
                om.getObjectList("sections", Dungeon.Section.class );
                d.setMains(d.getSection(om.getUUID("mains_id")));
            } catch (Exception e) {
                Dungeon.dungeons.remove(d);
                throw new RuntimeException(e);
            }
            return d;
        });
    }

    public static void forceSetField(Object o, @NotNull Field f, Object val) {
        f.setAccessible(true);
        try {
            f.set(o, val);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(new MainListener(), this);
        Bukkit.getPluginManager().registerEvents(new GeneralListener(), this);
        Bukkit.getPluginManager().registerEvents(new Manageable.ManagableListener(), this);
        loadCommands();
        FileManager.loadData();
        try {
            Class<?> c = Class.forName("org.bukkit.craftbukkit.v1_20_R2.block.impl.CraftLeaves");
            while (c.getSuperclass() != null) {
                log(c.getSuperclass());
                c = c.getSuperclass();
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        PluginThread.finish();
        FileManager.saveData();
        super.onDisable();
    }
}
