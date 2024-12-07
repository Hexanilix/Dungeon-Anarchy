package org.hexils.dnarch.dungeon;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.hetils.jgl17.General;
import org.hetils.jgl17.Pair;
import org.hetils.mpdl.Inventory;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.*;
import org.hexils.dnarch.objects.conditions.DungeonStart;
import org.hexils.dnarch.objects.conditions.LocationCondition;
import org.hexils.dnarch.objects.conditions.Type;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.hetils.mpdl.General.log;
import static org.hetils.mpdl.Item.newItemStack;
import static org.hetils.mpdl.Location.toMaxMin;

public class Dungeon extends Managable {
    public static @Nullable String commandNew(DungeonMaster dm, @NotNull String[] args) {
        if (dm.hasAreaSelected()) {
            Dungeon d;
            if (args.length > 0) {
                try {
                    d = new Dungeon(dm.p.getUniqueId(), args[0], dm.getSelectedArea());
                } catch (Dungeon.DuplicateNameException ignore) {
                    return ChatColor.RED + "Dungeon " + args[0] + " already exists.";
                }
            } else d = new Dungeon(dm.p.getUniqueId(), dm.getSelectedArea());
            dm.clearSelection();
            dm.setCurrent_dungeon(d);
            return ChatColor.GREEN + "Created new dungeon " + d.getName();
        } else return ChatColor.RED + "You must select a section to create a dungeon";
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

    public class Section extends Managable {
        private String name;
        private final org.hetils.mpdl.Location.Box bounds;
        private final LocationCondition sectionEnter;

        public Section(Pair<Location, Location> selection) {
            this(selection, "Section" + sections.size());
        }

        public Section(Pair<Location, Location> selection, String name) {
            this.bounds = new org.hetils.mpdl.Location.Box(selection);
            this.sectionEnter = new LocationCondition(this.bounds);
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

        public LocationCondition getSectionEnterCondition() { return sectionEnter; }

        @Override
        public void createGUI() {
            this.guiSize(54);
            this.gui.setItem(4, newItemStack(Material.BUCKET, "Get Location Condition", List.of(ChatColor.GRAY + "Click to get"), GUI.ITEM_ACTION, "getLocCond"));
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
    private String display_name;
    private final List<DA_item> items = new ArrayList<>();
    private final List<Section> sections = new ArrayList<>();
    private Section mains;
    private final UUID creator;
    private String creator_name;
    private DungeonStart dungeon_start;
    private boolean running = false;

    public Dungeon(UUID creator, Pair<Location, Location> sec) {
        Player p = Bukkit.getPlayer(creator);
        this.creator = creator;
        this.creator_name = p == null ? "" : p.getName();
        do {
            this.name = "Dungeon" + dungeons.size();
            this.display_name = this.name;
        } while (get(name) != null);
        this.mains = new Section(sec, "Main_Sector");
        bounding_box = new org.hetils.mpdl.Location.Box(toMaxMin(sec));
        dungeons.add(this);
    }
    public Dungeon(UUID creator, String name, Pair<Location, Location> sec) throws DuplicateNameException {
        Player p = Bukkit.getPlayer(creator);
        this.creator = creator;
        this.creator_name = p == null ? "" : p.getName();
        if (get(name) != null) throw new DuplicateNameException("");
        this.name = name;
        this.display_name = this.name;
        this.mains = new Section(sec, "Main_Sector");
        bounding_box = new org.hetils.mpdl.Location.Box(toMaxMin(sec));
        this.dungeon_start = new DungeonStart(this);
        dungeons.add(this);
    }

    public String getName() { return name; }

    public String getDisplayName() { return display_name; }

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

    public UUID getCreator() { return creator; }

    public List<DA_item> getItems() { return items; }

    public String getCreatorName() { return creator_name; }

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
        General.Stopwatch t = new General.Stopwatch();
        t.start();
        if (!bounding_box.contains(l)) return false;
        for (Section s : sections)
            if (s.bounds.contains(l))
                return true;
        log(t.getTime());
        return mains.bounds.contains(l);
    }

    @Override
    public void createGUI() {
        this.gui = Inventory.newInv(54, display_name);
        ItemStack getDS = newItemStack(Material.CLOCK, ChatColor.GREEN + "Get dungeon start block");
        NSK.setNSK(getDS, GUI.ITEM_ACTION, "giveDungeonStartBlock");
        this.gui.setItem(5, getDS);
    }
    @Override
    public void updateGUI() {

    }
    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {

    }
    @Override
    protected void action(DungeonMaster dm, String action, String[] args) {
        log("gdfjklhjk: " + action);
        switch (action) {
            case "giveDungeonStartBlock" -> dm.give(this.dungeon_start);
        }
    }
}
