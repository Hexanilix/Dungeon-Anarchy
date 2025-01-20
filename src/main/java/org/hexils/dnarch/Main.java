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

import java.util.*;
import java.util.logging.Level;

//TODO make version compatible
public final class Main extends JavaPlugin {

    public static final String minecraft_version;
    static {
        String v = Bukkit.getBukkitVersion().replace(".", "_");
        String[] vs = v.split("-");
        minecraft_version = "v" + vs[0].substring(0, vs[0].length()-1) + vs[1].charAt(0) + vs[0].substring(vs[0].length()-1);
    }
    public static Main plugin;
    public static final String name = "dungeon_anarchy";
    public static final Logger logger = new Logger("Dungeon Anarchy");

    public static OODP dp = new OODP();

    public static class Config {
        public boolean readable_file_data = false;
        public boolean auto_delete_empty_files = true;
    }
    public static class Debug {
        public boolean auto_exclude_fields = true;
        public boolean log_converting_process = false;
        public boolean log_dungeon_error_stacktrace = true;

        public void logConvertingProcess(boolean b) { log_converting_process = b; dp.logConversions(b); }
    }
    public static Config config = new Config();
    public static Debug debug = new Debug();

    static {
        dp.convertClassExtending(Getter.class, Getter::get);
        dp.createClass(String.class, Getter.class, s -> () -> s);

        dp.processClasAs("org.bukkit.craftbukkit."+minecraft_version+".CraftWorld", World.class);
        dp.convertClass(World.class, WorldInfo::getUID);
        dp.createClass(World.class, om -> Bukkit.getWorld(om.getUUID("id")));

        dp.convertClass(NSK.class, nsk -> Map.of("key", nsk.key.toString(), "type", NSK.getDataTypeName(nsk.type)));
        dp.createClass(NSK.class, om -> new NSK<>(new NamespacedKey(Main.plugin, om.getString("key").split(":")[1]), NSK.getDataType(om.getString("type"))));

        dp.convertClassExtending(Entity.class, Entity::getUniqueId);

        dp.processClasAs("org.bukkit.craftbukkit."+minecraft_version+".inventory.CraftItemStack", ItemStack.class);
        //TODO enchants
        dp.convertClass(ItemStack.class, (item) -> {
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
        dp.createClass(ItemStack.class, om -> {
            ItemStack item = new ItemStack(om.get("mat", Material.class), om.get("amount", int.class, 1));
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
                    Map<NSK, Object> nskm = om.toMap("nsks").asHashMap(NSK.class, Object.class);
                    for (Map.Entry<NSK, Object> e : nskm.entrySet())
                        NSK.setNSK(m, e.getKey(), e.getValue().getClass() == UUID.class ? e.getValue().toString() : e.getValue());
                }
                item.setItemMeta(m);
            }
            return item;
        });

        dp.convertClass(LocationUtil.BoundingBox.class, bb -> new double[]{bb.getMinX(), bb.getMinY(), bb.getMinZ(), bb.getMaxX(), bb.getMaxY(), bb.getMaxZ()});

        dp.convertClass(Location.class, l -> Map.of("world", l.getWorld(),"xyzyp", new double[]{l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()}));
        dp.createClass(Location.class, om -> {
            double[] a = om.getDoubleArr("xyzyp");
            return new Location(Bukkit.getWorld(om.getUUID("world")), a[0], a[1], a[2], (float) a[3], (float) a[4]);
        });
        dp.processClasAs("org.bukkit.craftbukkit."+minecraft_version+".block.CraftBlock", Block.class);
        dp.convertClass(Block.class, b -> Map.of("world", b.getWorld(),"x", b.getX(),"y", b.getY(), "z", b.getZ()));

        dp.convertClass(Trigger.class, t -> Map.of("id", t.getId(), "name", t.getName(), "actions", t.getActions().stream().map(DAItem::getId).toList(), "conditions", t.getConditions().stream().map(DAItem::getId).toList()));
        dp.createClass(Trigger.class, om -> {
            Trigger t = new Trigger(
                    om.getList("actions", new OODP.Converter<>(UUID.class, Action.class, id -> (Action) DAItem.get(id))),
                    om.getList("conditions", new OODP.Converter<>(UUID.class, Condition.class, id -> (Condition) DAItem.get(id)))
            );
            t.setId(om.getUUID("id"));
            t.setName(om.getString("name"));
            t.updateGUI();
            return t;
        });

        dp.createClass(Block.class, om -> Bukkit.getWorld(om.getUUID("world")).getBlockAt(om.getInt("x"), om.getInt("y"), om.getInt("z")));

        dp.convertClassExtending(DAItem.class, DAItem::getId);
        dp.createClassExtending(UUID.class, DAItem.class, DAItem::get);

        dp.convertFields(Dungeon.Section.class, Dungeon.Section::getId);
        dp.excludeFieldsFor(Dungeon.Section.class, "renameable", "og_name");

        dp.convertField(Dungeon.class, "items", Set.class, l -> ((Set<DAItem>) l).stream().map(da -> dp.toOodp(da, config.readable_file_data, true)).toList());
        dp.convertField(Dungeon.class, "triggers", Set.class, l -> ((Set<Trigger>) l).stream().map(tr -> dp.toOodp(tr, config.readable_file_data, true)).toList());
        dp.createClass(Dungeon.class, om -> {
            if (om.isEmpty()) return null;
            World w = om.get("world", World.class);
            double[] ar = om.getDoubleArr("bounding_box");
            Dungeon d = null;
            try {
                d = new Dungeon(om.getString("name"), w, new LocationUtil.BoundingBox(new Pair<>(new Location(w, ar[0], ar[1], ar[2]), new Location(w, ar[3], ar[4], ar[5]))), om.get("dungeon_info", Dungeon.DungeonInfo.class));
                d.getDungeonStart().setId(om.getUUID("dungeon_start"));
                Dungeon finalD = d;
                om.getList("sections", Dungeon.Section.class, som -> {
                    try {
                        double[] a = som.getDoubleArr("bounds");
                        LocationUtil.BoundingBox bb = new LocationUtil.BoundingBox(new Pair<>(new Location(w, a[0], a[1], a[2]), new Location(w, a[3], a[4], a[5])));
                        Dungeon.Section s = finalD.newSection(som.getUUID("id"), som.getString("name"), bb);
                        DAItem i = s.getWhithinBoundCondition();
                        i.setId(som.getUUID("section_enter"));
                        return s;
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
                dp.createFields(UUID.class, Dungeon.Section.class, finalD::getSection);
                Set<DAItem> items = new HashSet<>();
                for (OODP.ObjectiveMap obm : om.getObjectiveList("items")) {
                    Type ty = Type.get(obm.getString("type"));
                    DAItem da = obm.as(ty.getDAClass(), true);
                    Dungeon.Section s = d.getSection(obm.getUUID("section"));
                    if (s != null) da.setSection(s);
                    items.add(da);
                }
                items.forEach(i -> {
                    if (i instanceof BlockAction ba) ba.updateBlockData();
                    if (i instanceof RunnableDA rda) rda.start();
                });
                d.setItems(items);
                Set<Trigger> ts = om.getSet("triggers", Trigger.class);
                d.setTriggers(ts);
                d.setMains(d.getSection(om.getUUID("mains")));
            } catch (Exception e) {
                Dungeon.dungeons.remove(d);
                throw new RuntimeException(e);
            }
            return d;
        });
    }

    public static void log(String s) { logger.log(s); }
    public static void log(Object o) { logger.log(o); }
    public static void log(Level level, String s) { logger.log(level, s); }
    public static void log(Level level, Object o) { logger.log(level, o); }

    public static final ItemStack wand = new ItemStack(Material.STICK);
    static {
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
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(new MainListener(), this);
        Bukkit.getPluginManager().registerEvents(new GeneralListener(), this);
        Bukkit.getPluginManager().registerEvents(new Manageable.ManagableListener(), this);
        loadCommands();

        load();
    }

    public static void load() {
        FileManager.loadData();

        dp.pretty(config.readable_file_data);
        dp.autoExcludeFields(debug.auto_exclude_fields);
        dp.logConversions(debug.log_converting_process);
    }

    public static void reload() {
        plugin.onDisable(true);
        plugin.onEnable();
    }

    @Override
    public void onDisable() { onDisable(true); }

    public void onDisable(boolean save_data) {
        Dungeon.dungeons.clear();
        Trigger.instances.clear();
        Action.instances.clear();
        Condition.instances.clear();
        DAItem.instances.clear();
        if (save_data) {
            PluginThread.finish();
            FileManager.saveData();
        }
    }
}
