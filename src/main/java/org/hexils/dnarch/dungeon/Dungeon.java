package org.hexils.dnarch.dungeon;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.hetils.jgl17.Pair;
import org.hetils.mpdl.InventoryUtil;
import org.hetils.mpdl.LocationUtil;
import org.hetils.mpdl.NSK;
import org.hetils.mpdl.VectorUtil;
import org.hetils.mpdl.GeneralListener;
import org.hexils.dnarch.*;
import org.hexils.dnarch.objects.conditions.DungeonStart;
import org.hexils.dnarch.objects.conditions.WithinBoundsCondition;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;



import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.commands.DungeonCommandExecutor.*;
import static org.hexils.dnarch.commands.DungeonCreatorCommandExecutor.W;

public class Dungeon extends Managable implements Savable {
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
        private final LocationUtil.BoundingBox bounds;
        private final WithinBoundsCondition sectionEnter;

        public Section(Pair<Location, Location> selection) {
            this(selection, "Section" + sections.size());
        }

        public Section(Pair<Location, Location> selection, String name) {
            super(name);
            int i = 0;
            do {
                id = UUID.randomUUID();
                i++;
            } while (i < 256);
            this.bounds = new LocationUtil.BoundingBox(selection);
            this.sectionEnter = new WithinBoundsCondition(this.bounds);
            sections.add(this);
        }

        public UUID getId() {
            return id;
        }

        @Override
        protected void createGUI() {
            this.setSize(54);
            this.setNameSign(13);
            this.gui.setItem(20, newItemStack(
                    Material.BARREL,
                    ChatColor.GREEN + "Events",
                    List.of(ChatColor.GRAY + "Shows the list of events this section produces"),
                    ITEM_ACTION, "showEvents")
            );
            this.gui.setItem(21, newItemStack(
                    Material.ENDER_CHEST,
                    ChatColor.GREEN + "Items",
                    List.of(ChatColor.GRAY + "Shows the list of items this section contains"),
                    ITEM_ACTION, "showItems")
            );
            this.gui.setItem(22, newItemStack(
                    Material.ENDER_EYE,
                    ChatColor.AQUA + "Show/Hide Section",
                    List.of(ChatColor.GRAY + "Click to display or hide the bounding area of the section"),
                    ITEM_ACTION, "displaySection")
            );
            this.gui.setItem(24, newItemStack(
                    Material.TNT,
                    ChatColor.AQUA + "Delete Section",
                    List.of(ChatColor.GRAY + "Click to delete this section from \"" + dungeon_info.display_name + "\""),
                    ITEM_ACTION, "deleteSection")
            );
        }

        @Override
        public void updateGUI() {

        }

        public void addItem(Action a) {
            items.add(a);
        }

        private class EventList extends Managable {
            public EventList() {}

            @Override
            protected void createGUI() {
                this.setSize(54);
                this.setName(Dungeon.this.dungeon_info.display_name + " events");
                this.gui.setItem(0, newItemStack(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, ChatColor.GOLD + "Area Condition", List.of(ChatColor.GRAY + "This condition triggers whenever a player enters", ChatColor.GRAY + "Click to get"), ITEM_ACTION, "getLocCond"));
                updateGUI();
            }

            @Override
            public void updateGUI() {
            }

            @Override
            protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {
                switch (action) {
                    case "getLocCond" -> dm.give(sectionEnter);
                }
            }
        }
        private class ItemList extends Managable {
            public ItemList() {}

            @Override
            protected void createGUI() {
                this.setSize(54);
                this.setName(Dungeon.this.dungeon_info.display_name + " items");

                updateGUI();
            }

            @Override
            public void updateGUI() {
            }

            @Override
            protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {
                switch (action) {
                    case "getLocCond" -> dm.give(sectionEnter);
                }
            }
        }

        private final EventList ev = new EventList();

        @Override
        protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {
            switch (action) {
                case "showEvents" -> ev.manage(dm, this);
                case "showItems" -> {}
                case "displaySection" -> {
                    viewers.add(dm);
                    if (!dm.hideSelection(this)) dm.select(this, bounds.toLocationPair(dm.p.getWorld()));
                }
                case "deleteSection" -> this.attemptRemove(dm, true);
            }
        }

        public void attemptRemove(DungeonMaster dm, boolean has_gui_open) {
            if (has_gui_open) dm.p.closeInventory();
            GeneralListener.confirmWithPlayer(dm.p, W + "Are you sure you want to delete section \"" + name + W + "\"? (yes/no)", text -> {
                if (text.equalsIgnoreCase("yes")) {
                    if (mains == this) {
                        if (sections.size() != 1 || this != sections.get(0)) {
                            mains = sections.get(0);
                            dm.p.sendMessage(W + "Set the main dungeon sector to \"" + mains.getName() + "\"");
                        } else {
                            dm.p.sendMessage(ER + "You can't delete the main sector of a dungeon! Use \"/dc delete\" to delete the dungeon");
                            return true;
                        }
                    }
                    this.delete();
                    updateBoundingBox();
                    dm.p.sendMessage(IF + "Deleted section \"" + name + "\"!.");
                } else {
                    dm.p.sendMessage(IF + "Cancelled.");
                    if (has_gui_open) this.manage(dm);
                }
                return true;
            }, () -> dm.p.sendMessage(IF + "Cancelled."));
        }

        @Override
        public void delete() {
            sections.remove(this);
            super.delete();
        }

        public Dungeon getDungeon() { return Dungeon.this; }

        private Material displ_mat = Material.GREEN_CONCRETE;

        public ItemStack toItem() {
            ItemStack i = newItemStack(displ_mat, name);
            NSK.setNSK(i, ITEM_ACTION, id.toString());
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
    private final List<DungeonMaster> viewers = new ArrayList<>();

    public static String getNewDGName() {
        String name;
        int i = 0;
        do {
            name = "Dungeon" + (dungeons.size()+i);
            i++;
        } while (get(name) != null);
        return name;
    }

    @Contract("_, _ -> new")
    public static @NotNull Dungeon create(UUID creator, Pair<Location, Location> sec) {
        try {
            return new Dungeon(creator, getNewDGName(), sec);
        } catch (Dungeon.DuplicateNameException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public Dungeon(UUID creator, String name, @NotNull Pair<Location, Location> sec) throws DuplicateNameException {
        //TODO dupne
        if (get(name) != null) throw new DuplicateNameException("Dungeon " + name + "already exists");
        Player p = Bukkit.getPlayer(creator);
        this.dungeon_info.creator = creator;
        this.dungeon_info.creator_name = p == null ? "" : p.getName();
        this.name = name;
        this.world = sec.key().getWorld();
        this.dungeon_info.display_name = name;
        this.mains = new Section(sec, "Main_Sector");
        bounding_box = new LocationUtil.BoundingBox(sec);
        dungeons.add(this);
    }
    
    public static @Nullable Section getIntersectedSection(LocationUtil.BoundingBox bb) {
        for (Dungeon d : dungeons)
            if (d.bounding_box.intersects(bb))
                for (Section s : d.sections)
                    if (s.bounds.intersects(bb))
                        return s;
        return null;
    }

    public @NotNull String commandNewSection(@NotNull DungeonMaster dm, @NotNull String[] args) {
        if (dm.hasAreaSelected()) {
            if (dm.isEditing()) {
                Section is = getIntersectedSection(new LocationUtil.BoundingBox(dm.getSelectedArea()));
                if (is == null) {
                    Dungeon d = dm.getCurrentDungeon();
                    Section s;
                    if (args.length > 0) s = newSection(dm.getSelectedArea(), args[0]);  
                    else s = newSection(dm.getSelectedArea());
                    dm.clearSelection();
                    return OK + "Created \"" + d.getName() + "\" section \"" + s.getName() + "\"";
                } else return ER + "Cannot create section ";
            } else return ER + "You must be currently editing a dungeon to create sections!";
        } else return ER + "You need to first select an area to create a section!";
    }

    public String getName() { return name; }

    public boolean isRunning() { return running; }

    public void start() {
        if (!this.running) {
            this.running = true;
            dungeon_items.start.trigger();
        }
    }

    public void stop() { if (this.running) this.running = false; }

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

    public Section newSection(Pair<Location, Location> sec) {
        Section s = new Section(sec);
        updateBoundingBox();
        return s;
    }
    public Section newSection(Pair<Location, Location> sec, String name) {
        Section s = new Section(sec, name);
        updateBoundingBox();
        return s;
    }

    public BoundingBox getBoundingBox() { return this.bounding_box; }

    private void updateBoundingBox() {
        List<Vector> l = new ArrayList<>(sections.stream().map(s -> s.bounds.getMax()).toList());
        l.addAll(sections.stream().map(s -> s.bounds.getMin()).toList());
        l.add(mains.bounds.getMax());
        l.add(mains.bounds.getMin());
        bounding_box.resize(VectorUtil.toMaxMin(l));
        viewers.forEach(this::showDungeonFor);
    }


    public static final Particle[] selectParts;
    static {
        selectParts = new Particle[]{Particle.ENCHANTMENT_TABLE, Particle.COMPOSTER, Particle.SOUL_FIRE_FLAME, Particle.FLAME, Particle.END_ROD};
    }

    public void addViewer(DungeonMaster dm) { if (!viewers.contains(dm)) viewers.add(dm); }

    public void removeViewer(DungeonMaster dm) {
        viewers.remove(dm);
        hideDungeonFrom(dm);
    }

    public void showDungeonFor(DungeonMaster dm) {
        addViewer(dm);
        dm.select(this, bounding_box.toLocationPair(world), Particle.GLOW);
        dm.select(mains, mains.bounds.toLocationPair(world), Particle.END_ROD);
        for (int i = 0; i < sections.size(); i++)
            dm.select(sections.get(i), sections.get(i).bounds.toLocationPair(world), selectParts[i%selectParts.length]);
    }
    public void hideDungeonFrom(@NotNull DungeonMaster dm) {
        dm.hideSelection(this);
        sections.forEach(dm::hideSelection);
        if (dm.isEditing() && dm.getCurrentDungeon() == this)
            dm.setCurrentDungeon(this);
    }

    public boolean isWithinDungeon(Location l) {
        if (!bounding_box.contains(l)) return false;
        for (Section s : sections)
            if (s.bounds.contains(l))
                return true;
        return mains.bounds.contains(l);
    }

    public void attemptRemove(Player p) {
        String name = this.getDungeonInfo().display_name;
        GeneralListener.confirmWithPlayer(p, W + "Are you sure you want to delete dungeon \"" + name + "\"? (yes/no)", text -> {
            if (text.equalsIgnoreCase("yes")) {
                this.delete();
                p.sendMessage(IF + "Deleted dungeon \"" + name + "\"!");
            }
            return true;
        });
    }

    public Section getSection(Player p) {
        if (mains.bounds.contains(p)) return mains;
        for (Section s : sections)
            if (s.bounds.contains(p))
                return s;
        return null;
    }
    public Section getSection(String s) {
        if (mains.getName().equals(s)) return mains;
        for (Section sc : sections)
            if (sc.getName().equals(s))
                return sc;
        return null;
    }
    public Section getSection(Block b) {
        return getSection(b.getLocation());
    }
    public Section getSection(Location l) {
        if (mains.bounds.contains(l)) return mains;
        for (Section sc : sections)
            if (sc.bounds.contains(l))
                return sc;
        return null;
    }

    public @NotNull List<Section> getSections() {
        List<Section> secs = new ArrayList<>(sections);
        secs.add(mains);
        return secs;
    }
    
    private class SectorGUIList extends Managable {
        public SectorGUIList() {}

        @Override
        protected void createGUI() {
            this.setSize(54);
            this.setName(Dungeon.this.dungeon_info.display_name + " sections");
            updateGUI();
        }

        @Override
        public void updateGUI() {
            InventoryUtil.fillBox(gui, 0, 9, 6, sections.stream().map(Section::toItem).toList());
        }

        @Override
        protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {
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
        this.gui = InventoryUtil.newInv(54, dungeon_info.display_name);
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
    protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {
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

        public String display_name;
        public UUID creator;
        public String creator_name;
        public String difficulty;
        public String description;

    }
    
    public class DungeonItems {
        public final DungeonStart start = new DungeonStart(Dungeon.this);
    }

    @Override
    public void save() {
        
    }

    @Override
    public void delete() {
        viewers.forEach(dm -> {
            dm.hideSelection(this);
            sections.forEach(dm::hideSelection);
            if (dm.isEditing() && dm.getCurrentDungeon() == this)
                dm.setCurrentDungeon(this);
        });
        dungeons.remove(this);
        super.delete();
    }
}
