package org.hexils.dnarch.dungeon;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.hetils.jgl17.Pair;
import org.hetils.mpdl.InventoryUtil;
import org.hetils.mpdl.LocationUtil;
import org.hetils.mpdl.NSK;
import org.hetils.mpdl.VectorUtil;
import org.hexils.dnarch.*;
import org.hexils.dnarch.objects.conditions.DungeonStart;
import org.hexils.dnarch.objects.conditions.WithinBoundsCondition;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.hetils.mpdl.GeneralUtil.log;

import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.GUI.ITEM_ACTION;

public class Dungeon extends Managable implements Savable {

    @Override
    public void save() {

    }

    public Section getSection(Player p) {
        for (Section s : sections)
            if (s.bounds.contains(p))
                return s;
        return null;
    }

    public static class DuplicateNameException extends Exception {
        public DuplicateNameException(String s) {
            super(s);
        }
    }

    public static final Collection<Dungeon> dungeons = new ArrayList<>();
    @Contract(pure = true)
    public static @Nullable Dungeon get(String name) {
        for (Dungeon d : dungeons)
            if (Objects.equals(d.name, name))
                return d;
        return null;
    }
    @Contract(pure = true)
    public static @Nullable Dungeon get(Location loc) {
        for (Dungeon d : dungeons)
            if (d.isWithinDungeon(loc))
                return d;
        return null;
    }

    @Contract(pure = true)
    public static @Nullable Dungeon get(Entity e) {
        return e != null ? get(e.getLocation()) : null;
    }

    public class Section extends Managable {
        private UUID id;
        private String name;
        private final LocationUtil.BoundingBox bounds;
        private final WithinBoundsCondition sectionEnter;

        public Section(Pair<Location, Location> selection) {
            this(selection, "Section" + sections.size());
        }

        public Section(Pair<Location, Location> selection, String name) {
            int i = 0;
            do {
                id = UUID.randomUUID();
                i++;
            } while (i < 256);
            this.bounds = new LocationUtil.BoundingBox(selection);
            this.sectionEnter = new WithinBoundsCondition(this.bounds);
            this.name = name;
            sections.add(this);
        }

        public static @NotNull String commandNew(@NotNull DungeonMaster dm, @NotNull String[] args) {
            if (dm.hasAreaSelected()) {
                if (dm.isEditing()) {
                    Dungeon d = dm.getCurrentDungeon();
                    if (args.length > 0) {
                        d.newSection(dm.getSelectedArea(), args[0]);
                        dm.clearSelection();
                        return ChatColor.GREEN + "Created '" + d.getName() + "' section " + args[0];
                    } else {
                        d.newSection(dm.getSelectedArea());
                        dm.clearSelection();
                        return ChatColor.GREEN + "Created '" + d.getName() + "' section";
                    }
                } else return ChatColor.RED + "You must be currently editing a dungeon to create sections!";
            } else return ChatColor.RED + "You need to first select an area to create a section!";
        }

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        public WithinBoundsCondition getSectionEnterCondition() { return sectionEnter; }

        @Override
        protected void createGUI() {
            this.setSize(54);
            this.gui.setItem(21, newItemStack(Material.BUCKET, "Get Location Condition", List.of(ChatColor.GRAY + "Click to get"), ITEM_ACTION, "getLocCond"));
        }

        @Override
        public void updateGUI() {

        }

        @Override
        protected void action(DungeonMaster dm, @NotNull String action, String[] args) {
            switch (action) {
                case "getLocCond" -> dm.give(this.sectionEnter);
            }
        }

        private Material displ_mat = Material.GREEN_CONCRETE;

        public ItemStack toItem() {
            ItemStack i = newItemStack(displ_mat, name);
            log(id);
            NSK.setNSK(i, ITEM_ACTION, id.toString());
            log(NSK.hasNSK(i, ITEM_ACTION));
            return i;
        }
    }

    private LocationUtil.BoundingBox bounding_box;
    private String name;
    private World world;
    private final List<DA_item> items = new ArrayList<>();
    private final List<Section> sections = new ArrayList<>();
    private Section mains;
    public final DungeonItems dungeon_items = new DungeonItems();
    private boolean running = false;
    private final DungeonInfo dungeon_info = new DungeonInfo();

    public static String getNewDGName() {
        String name;
        int i = 0;
        do {
            name = "Dungeon" + (dungeons.size()+i);
            i++;
        } while (get(name) != null);
        return name;
    }

    public Dungeon(UUID creator, Pair<Location, Location> sec) {
        this(creator, getNewDGName(), sec);
    }

    public Dungeon(UUID creator, String name, @NotNull Pair<Location, Location> sec) {
        //TODO dupne
//        if (get(name) != null) return;
        Player p = Bukkit.getPlayer(creator);
        this.dungeon_info.creator = creator;
        this.dungeon_info.creator_name = p == null ? "" : p.getName();
        this.name = name;
        this.world = sec.key().getWorld();
        this.dungeon_info.name = name;
        log(dungeon_info.name);
        this.mains = new Section(sec, "Main_Sector");
        bounding_box = new LocationUtil.BoundingBox(sec);
        dungeons.add(this);
    }

    public String getName() { return name; }

    public boolean isRunning() { return running; }

    public void start() {
        if (!this.running) {
            this.running = true;
            dungeon_items.start.trigger();
        }
    }

    public void stop() {
        if (this.running)
            this.running = false;
    }

    public Section getMains() { return mains; }

    public UUID getCreator() { return dungeon_info.creator; }

    public List<DA_item> getItems() { return items; }

    public void addItem(@NotNull DA_item i) { items.add(i); }

    public boolean removeItem(DA_item i) {
        if (items.remove(i)) {
            i.delete();
            return true;
        } else return false;
    }

    public String getCreatorName() { return dungeon_info.creator_name; }

    public void newSection(Pair<Location, Location> sec) {
        new Section(sec);
        updateBoundingBox();
    }
    public void newSection(Pair<Location, Location> sec, String name) {
        new Section(sec, name);
        updateBoundingBox();
    }

    public BoundingBox getBoundingBox() { return this.bounding_box; }

    private void updateBoundingBox() {
        List<Vector> l = new ArrayList<>(sections.stream().map(s -> s.bounds.getMax()).toList());
        l.addAll(sections.stream().map(s -> s.bounds.getMin()).toList());
        l.add(mains.bounds.getMax());
        l.add(mains.bounds.getMin());
        bounding_box.resize(VectorUtil.toMaxMin(l));
    }


    public static final Particle[] selectParts;
    static {
        selectParts = new Particle[]{Particle.ENCHANTMENT_TABLE, Particle.COMPOSTER, Particle.SOUL_FIRE_FLAME, Particle.FLAME, Particle.END_ROD};
    }

    public void displayDungeon(Player p) {
        DungeonMaster dm = DungeonMaster.getOrNew(p);
        dm.select(bounding_box.toLocationPair(world), Particle.GLOW);
        dm.select(mains.bounds.toLocationPair(world), Particle.END_ROD);
        for (int i = 0; i < sections.size(); i++) {
            dm.select(sections.get(i).bounds.toLocationPair(world), selectParts[i%selectParts.length]);
        }
    }

    public boolean isWithinDungeon(Location l) {
        if (!bounding_box.contains(l)) return false;
        for (Section s : sections)
            if (s.bounds.contains(l))
                return true;
        return mains.bounds.contains(l);
    }
    
    private class SectorGUIList extends Managable {
        public SectorGUIList() {}

        @Override
        protected void createGUI() {
            this.name = Dungeon.this.dungeon_info.name + " sections";
            this.setSize(54);
            updateGUI();
        }

        @Override
        public void updateGUI() {
            InventoryUtil.fillBox(gui, 0, 9, 6, sections.stream().map(Section::toItem).toList());
        }

        @Override
        protected void action(DungeonMaster dm, String action, String[] args) {
            log(action);
            Section s = null;
            UUID id = UUID.fromString(action);
            for (Section sc : sections)
                if (sc.getId().equals(id)) {
                    s = sc;
                    break;
                }
            if (s != null) {
                s.manage(dm, this);
            }
        }
    }
    
    private final SectorGUIList sector_gui_list = new SectorGUIList();

    @Override
    protected void createGUI() {
        this.gui = InventoryUtil.newInv(54, dungeon_info.name);
        this.gui.setItem(20, newItemStack(Material.CLOCK, ChatColor.GREEN + "Get dungeon start block", ITEM_ACTION, "giveDungeonStartBlock"));
        this.gui.setItem(21, newItemStack(Material.WRITABLE_BOOK, ChatColor.GREEN + "Sections", sections.stream().map(s -> ChatColor.GRAY + s.getName()).toList(), ITEM_ACTION, "dungeonSectorList"));
    }
    @Override
    public void updateGUI() {

    }
    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {

    }
    @Override
    protected void action(DungeonMaster dm, @NotNull String action, String[] args) {
        switch (action) {
            case "giveDungeonStartBlock" -> {
                dm.give(this.dungeon_items.start);
            }
            case "dungeonSectorList" -> {
                sector_gui_list.manage(dm, this);
            }
        }
    }

    public DungeonInfo getDungeonInfo() { return dungeon_info; }

    public static class DungeonInfo {

        public String name;
        public UUID creator;
        public String creator_name;
        public String difficulty;
        public String description;

    }
    
    public class DungeonItems {
        public final DungeonStart start = new DungeonStart(Dungeon.this);
    }

    @Override
    public void delete() {
        dungeons.remove(this);
        super.delete();
    }
}
