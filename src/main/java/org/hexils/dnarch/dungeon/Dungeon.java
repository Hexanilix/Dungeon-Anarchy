package org.hexils.dnarch.dungeon;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.hetils.jgl17.General;
import org.hetils.jgl17.Pair;
import org.hetils.mpdl.Inventory;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.*;
import org.hexils.dnarch.objects.conditions.DungeonStart;
import org.hexils.dnarch.objects.conditions.WithinBoundsCondition;
import org.hexils.dnarch.objects.conditions.Type;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.hetils.mpdl.General.log;
import static org.hetils.mpdl.Item.newItemStack;
import static org.hetils.mpdl.Location.toMaxMin;

public class Dungeon extends Managable implements Savable {

    @Override
    public void save() {

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
        private String name;
        private final org.hetils.mpdl.Location.Box bounds;
        private final WithinBoundsCondition sectionEnter;

        public Section(Pair<Location, Location> selection) {
            this(selection, "Section" + sections.size());
        }

        public Section(Pair<Location, Location> selection, String name) {
            this.bounds = new org.hetils.mpdl.Location.Box(selection);
            this.sectionEnter = new WithinBoundsCondition(this.bounds);
            this.name = name;
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

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        public WithinBoundsCondition getSectionEnterCondition() { return sectionEnter; }

        @Override
        public void createGUI() {
            this.guiSize(54);
            this.gui.setItem(21, newItemStack(Material.BUCKET, "Get Location Condition", List.of(ChatColor.GRAY + "Click to get"), GUI.ITEM_ACTION, "getLocCond"));
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
    }

    private org.hetils.mpdl.Location.Box bounding_box;
    private String name;
    private final List<DA_item> items = new ArrayList<>();
    private final List<Section> sections = new ArrayList<>();
    private Section mains;
    private DungeonStart dungeon_start;
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

    public Dungeon(UUID creator, String name, Pair<Location, Location> sec) {
        //TODO dupne
        if (get(name) != null) return;
        Player p = Bukkit.getPlayer(creator);
        this.dungeon_info.creator = creator;
        this.dungeon_info.creator_name = p == null ? "" : p.getName();
        this.name = name;
        this.dungeon_info.name = name;
        this.mains = new Section(sec, "Main_Sector");
        bounding_box = new org.hetils.mpdl.Location.Box(toMaxMin(sec));
        this.dungeon_start = new DungeonStart(this);
        dungeons.add(this);
    }

    public String getName() { return name; }

    public boolean isRunning() { return running; }

    public void start() {
        if (!this.running) {
            this.running = true;
            dungeon_start.trigger();
        }
    }

    public void stop() {
        if (this.running)
            this.running = false;
    }

    public Section getMains() { return mains; }

    public UUID getCreator() { return dungeon_info.creator; }

    public List<DA_item> getItems() { return items; }

    public String getCreatorName() { return dungeon_info.creator_name; }

    public DungeonStart getEventBlock(Type type) {
        if (type == null) return null;
        return switch (type) {
            case DUNGEON_START -> dungeon_start;
            default -> null;
        };
    }

    public void newSection(Pair<Location, Location> sec) {
        sections.add(new Section(sec));
        updateBoundingBox();
    }
    public void newSection(Pair<Location, Location> sec, String name) {
        sections.add(new Section(sec, name));
        updateBoundingBox();
    }

    public Pair<Location, Location> getBoundingBox() { return this.bounding_box; }

    private void updateBoundingBox() {
        List<Location> l = new ArrayList<>(sections.stream().map(s -> s.bounds.key()).toList());
        l.addAll(sections.stream().map(s -> s.bounds.value()).toList());
        l.addAll(List.of(mains.bounds.key(), mains.bounds.value()));
        bounding_box = new org.hetils.mpdl.Location.Box(toMaxMin(l));
    }


    public static final Particle[] selectParts;
    static {
        selectParts = new Particle[]{Particle.ENCHANTMENT_TABLE, Particle.COMPOSTER, Particle.SOUL_FIRE_FLAME, Particle.FLAME, Particle.END_ROD};
    }

    public void displayDungeon(Player p) {
        DungeonMaster dm = DungeonMaster.getOrNew(p);
        dm.select(bounding_box, Particle.GLOW);
        dm.select(mains.bounds, Particle.END_ROD);
        for (int i = 0; i < sections.size(); i++) {
            dm.select(sections.get(i).bounds, selectParts[i%selectParts.length]);
        }
    }

    public boolean isWithinDungeon(Location l) {
        if (!bounding_box.contains(l)) return false;
        for (Section s : sections)
            if (s.bounds.contains(l))
                return true;
        return mains.bounds.contains(l);
    }

    @Override
    public void createGUI() {
        this.gui = Inventory.newInv(54, dungeon_info.name);
        ItemStack getDS = newItemStack(Material.CLOCK, ChatColor.GREEN + "Get dungeon start block");
        NSK.setNSK(getDS, GUI.ITEM_ACTION, "giveDungeonStartBlock");
        this.gui.setItem(20, getDS);
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
                dm.give(this.dungeon_start);
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
}
