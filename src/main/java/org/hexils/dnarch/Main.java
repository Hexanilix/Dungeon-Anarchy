package org.hexils.dnarch;

import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.hetils.jgl17.Getter;
import org.hetils.jgl17.oodp.OODP;
import org.hetils.mpdl.*;
import org.hetils.mpdl.location.BoundingBox;
import org.hetils.mpdl.plugin.PluginAbbrev;
import org.hetils.mpdl.plugin.PluginThread;
import org.hexils.dnarch.commands.DungeonAnarchyCommandExecutor;
import org.hexils.dnarch.commands.DungeonCreatorCommandExecutor;
import org.hexils.dnarch.commands.DungeonCommandExecutor;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.hetils.mpdl.MPDL.loadCommand;
import static org.hetils.mpdl.MPDL.registerEvents;

//TODO make version compatible
public final class Main extends JavaPlugin implements PluginAbbrev {

    public static <T> @Nullable T noimpl(DungeonMaster dm) {
        if (dm != null) dm.sendWarning("This feature has not been implemented yet!");
        return null;
    }
    public static <T> @Nullable T noimpl() {
        log(Level.WARNING, "This feature has not been implemented yet!");
        return null;
    }

    private static Main plugin;
    public static Main plugin() { return plugin; }
    public static final String NAME = "dungeon_anarchy";
    public static final String ABBREV = "DA";
    private static Logger logger;

    @Override
    public String getAbbreviation() { return ABBREV; }

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

    //Creating the plugin "object to file" class
    public static final OODP dp = MPDL.mcCompatibleOodp();
    static {
        dp.autoExcludeFields(false);

        dp.convertClassExtending(Getter.class, Getter::get);
        dp.createClass(String.class, Getter.class, s -> () -> s);

        dp.convertClass(Trigger.class, t -> Map.of("id", t.getId(), "name", t.getName(), "actions", t.getActions().stream().map(DAItem::getId).toList(), "conditions", t.getConditions().stream().map(DAItem::getId).toList()));

        dp.createClass(Trigger.class, om -> om.as(Trigger.class, true));

        dp.convertClassExtending(DAItem.class, DAItem::getId);
        dp.createClassExtending(UUID.class, DAItem.class, DAItem::get);

        dp.convertFields(Dungeon.Section.class, Dungeon.Section::getId);
        dp.excludeFieldsFor(Dungeon.Section.class, "renameable", "og_name");

        dp.excludeFieldsFor(Trigger.class, "section");

        dp.convertField(Dungeon.class, "items", Set.class, l -> ((Set<DAItem>) l).stream().map(da -> dp.toOodp(da, config.readable_file_data, true)).toList());
        dp.convertField(Dungeon.class, "triggers", Set.class, l -> ((Set<Trigger>) l).stream().map(tr -> dp.toOodp(tr, config.readable_file_data, true)).toList());
        dp.createClass(Dungeon.class, om -> {
            if (om.isEmpty()) return null;

            Dungeon d = null;
            try {
                long start = System.currentTimeMillis();

                d = new Dungeon(om.getString("name"), om.get("world", World.class), om.get("bounding_box", BoundingBox.class), om.get("dungeon_info", Dungeon.DungeonInfo.class));
                d.getDungeonStart().setId(om.getUUID("dungeon_start"));
                Dungeon finalD = d;
                //Create the sections
                om.getList("sections", Dungeon.Section.class, som -> {
                    try {
                        BoundingBox bb = som.get("bounds", BoundingBox.class);
                        Dungeon.Section s = finalD.newSection(som.getUUID("id"), som.getString("name"), bb);
                        DAItem i = s.getWhithinBoundCondition();
                        i.setId(som.getUUID("section_enter"));
                        return s;
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
                dp.createClass(UUID.class, Dungeon.Section.class, finalD::getSection);

                //Set the main section
                d.setMains(om.get("mains", Dungeon.Section.class));

                //Creating items
                Set<OODP.ObjectiveMap> itemMaps = new HashSet<>(om.getObjectiveList("items"));
                //Create a set of all the item ids
                Set<String> itemIds = itemMaps.stream().map(m -> m.getRaw("id")).collect(Collectors.toSet());

                HashMap<OODP.ObjectiveMap, Set<String>> queue = new HashMap<>();
                for (OODP.ObjectiveMap obm : itemMaps) {
                    //getting all UUIDs present in the data and then checking if they're a daitem id
                    Set<String> foreign_ids = extractUUIDs(obm).stream().filter(itemIds::contains).collect(Collectors.toSet());
                    //Checking if items uses other daitems
                    if (!foreign_ids.isEmpty()) {
                        //Hold the item data off until the other items are created and loaded
                        queue.put(obm, foreign_ids);
                    } else {
                        d.addItem(createItemFromMap(obm, d));
                        String id = obm.getRaw("id");
                        for (Map.Entry<OODP.ObjectiveMap, Set<String>> e : queue.entrySet()) {
                            Set<String> set = e.getValue();
                            //Remove id of new created item from each withheld item
                            set.remove(id);
                            //Check if all required items were created
                            if (set.isEmpty()) {
                                OODP.ObjectiveMap map = e.getKey();
                                d.addItem(createItemFromMap(map, d));
                            }
                        }
                        //Remove created items from the queue
                        queue.entrySet().removeIf(e -> e.getValue().isEmpty());
                    }
                }
                if (!queue.isEmpty()) {
                    try {
                        queue.forEach((qom, s) -> finalD.addItem(createItemFromMap(qom, finalD)));
                    } catch (RuntimeException e) {
                        throw new RuntimeException(e);
                    }
                }

                //Update items which use blocks or run constantly
                d.getItems().forEach(i -> {
                    if (i instanceof BlockAction ba) ba.updateBlockData();
                    if (i instanceof RunnableDA rda) rda.start();
                });

                //Creating triggers after items to ensure the items exist beforehand
                Set<Trigger> triggers = om.getSet("triggers", Trigger.class);
                triggers.forEach(t -> {
                    t.updateSec();
                    t.bindConditions();
                    finalD.addItem(t);
                });

                log("Loaded dungeon \"" + d.getName() + "\". Took " + (System.currentTimeMillis()-start) + "ms");
                return d;
            } catch (Exception e) {
                Dungeon.dungeons.remove(d);
                throw new RuntimeException(e);
            }
        });
    }

    private static @NotNull DAItem createItemFromMap(OODP.@NotNull ObjectiveMap obm, @NotNull Dungeon d) {
        Type ty = Type.get(obm.getString("type"));
        if (ty != null) {
            DAItem da = obm.as(ty.getDAClass(), true);
            Dungeon.Section s = d.getSection(obm.getUUID("section"));
            if (s != null) da.setSection(s);
            da.setDungeon(d);
            return da;
        } else {
            throw new RuntimeException("Unknown daitem type: " + obm.getString("type"));
        }
    }
    public static @NotNull Set<String> extractUUIDs(OODP.@NotNull ObjectiveMap om) { return extractUUIDs(om.entrySet()); }
    public static @NotNull Set<String> extractUUIDs(@NotNull Set<Map.Entry<String, String>> set) {
        Set<String> idset = new HashSet<>();
        set.stream().filter(entry -> {
            String v = entry.getValue();
            String k = entry.getKey();
            if (k.equals("id") || k.equals("section") || k.contains("world")) return false;
            try {
                UUID.fromString(v);
            } catch (Exception e) {
                if (!v.startsWith("{") && !v.startsWith("["))
                    return false;
            }
            return true;
        }).map(Map.Entry::getValue).collect(Collectors.toSet()).forEach(s -> {
            if (s.startsWith("[")) {
                for (String sid : OODP.smartSplit(s, ',')) {
                    try {
                        UUID.fromString(sid);
                        idset.add(sid);
                    } catch (IllegalArgumentException ignore) {}
                }
            } else if (s.startsWith("{")) {
                for (String sid : OODP.smartSplit(s, ','))
                    idset.addAll(extractUUIDs(dp.map(sid).entrySet()));
            } else {
                idset.add(s);
            }
        });
        return idset;
    }

    public static void log(String s) { logger.log(Level.INFO, s); }
    public static void log(Object o) { logger.log(Level.INFO, String.valueOf(o)); }
    public static void log(Level level, String s) { logger.log(level, s); }
    public static void log(Level level, Object o) { logger.log(level, String.valueOf(o)); }

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
        var dae = new DungeonAnarchyCommandExecutor();
        var dat = new DungeonAnarchyCommandExecutor.Tab();
        loadCommand("dungeon_anarchy", dae, dat);
        loadCommand("da", dae, dat);
        loadCommand("dc", new DungeonCreatorCommandExecutor(), new DungeonCreatorCommandExecutor.tab());
        var dge = new DungeonCommandExecutor();
        var dgt = new DungeonCommandExecutor.Tab();
        loadCommand("dungeon", dge, dgt);
        loadCommand("dg", dge, dgt);
    }

    @Override
    public void onEnable() {
        logger = getLogger();
        plugin = this;
        MPDL.bind(this);
        loadClasses();
        super.onEnable();
        registerEvents(new MainListener());
        loadCommands();

        load();
    }

    /**
     * This method confirms if all the classes which extend
     * {@link DAItem} have a default parameter-less constructor.
     * <br><br>
     * However this method also ensures that all the
     * static {@link DAItem#setTabComplete(Function)} methods
     * are called during initialization
     */
    public static void loadClasses() {
        for (Type t : Type.values()) {
            Class<? extends DAItem> dac = t.getDAClass();
            if (!Modifier.isAbstract(dac.getModifiers()) && t.isCreatable()) {
                try {
                    dac.getDeclaredConstructor().newInstance();
                } catch (NoSuchMethodException e) {
                    log(dac.getName() + " Does not have a default parameterless constructor.");
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
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
        if (save_data) {
            PluginThread.finish();
            FileManager.saveData();
        }
        PluginThread.finish();
        Dungeon.dungeons.clear();
        Trigger.instances.clear();
        Action.instances.clear();
        Condition.instances.clear();
        DAItem.instances.clear();
        System.gc();
    }
}
