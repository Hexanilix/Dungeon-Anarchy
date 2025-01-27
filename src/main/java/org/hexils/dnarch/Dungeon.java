package org.hexils.dnarch;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.hetils.jgl17.Getter;
import org.hetils.jgl17.Pair;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.*;
import org.hetils.mpdl.location.BoundingBox;
import org.hetils.mpdl.location.LocationUtil;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.conditions.WithinBoundsCondition;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;


import static org.hetils.mpdl.item.ItemUtil.newItemStack;

public class Dungeon extends DAManageable implements Savable {

    public static class DuplicateNameException extends Exception {  public DuplicateNameException(String s) { super(s); } }
    public static class DungeonIntersectViolation extends Exception {  public DungeonIntersectViolation(String s) { super(s); } }

    public static final Collection<Dungeon> dungeons = new ArrayList<>();
    @Contract(pure = true)
    public static @Nullable Dungeon get(String name) {
        for (Dungeon d : dungeons)
            if (Objects.equals(d.getName(), name))
                return d;
        return null;
    }
    @Contract(pure = true)
    public static @Nullable Dungeon get(Location loc) {
        for (Dungeon d : dungeons)
            if (d.contains(loc))
                return d;
        return null;
    }

    @Contract(pure = true)
    public static @Nullable Dungeon get(Entity e) { return e != null ? get(e.getLocation()) : null; }

    public class Section extends DAManageable {
        private UUID id;
        private final BoundingBox bounds;
        private final WithinBoundsCondition section_enter;
        private Material displ_mat = Material.GREEN_CONCRETE;
        @OODPExclude
        private final ItemListGUI event_gui;
        @OODPExclude
        private final ItemListGUI item_gui;
        @OODPExclude
        private final ItemListGUI trigger_gui;
        @OODPExclude
        private final Getter<Set<DAItem>> get_items = () -> items.stream().filter(da -> da.section == this).collect(Collectors.toSet());

        public Section(Pair<Location, Location> selection) { this(selection, "Section" + sections.size()); }
        public Section(Pair<Location, Location> selection, String name) { this(UUID.randomUUID(), name, new BoundingBox(selection)); }
        Section(UUID id, String name, BoundingBox bounds) {
            super(name, true, 54);
            this.id = id;
            this.bounds = bounds;
            this.section_enter = new WithinBoundsCondition(this.bounds);
            this.section_enter.setRenameable(false);
            this.event_gui = new ItemListGUI(() -> getName() + " events", () -> newItemStack(
                    Material.BARREL,
                    ChatColor.GREEN + "Events",
                    List.of(ChatColor.GRAY + "Shows the list of events this section produces")),
                    section_enter
            );
            this.item_gui = new ItemListGUI(() -> getName() + " items", () -> newItemStack(
                    Material.ENDER_CHEST,
                    ChatColor.GREEN + "Items",
                    List.of(ChatColor.GRAY + "Shows the list of items this section contains")),
                    get_items
            );
            this.trigger_gui = new ItemListGUI(() -> getName() + " triggers", () -> newItemStack(
                    Material.COMPARATOR, ChatColor.YELLOW + "Triggers"),
                    () -> triggers.stream().filter(da -> da.section == this).collect(Collectors.toSet())
            );
            this.onRename(() -> {
                event_gui.setName(getName());
                item_gui.setName(getName());
            });
            sections.add(this);
        }

        public UUID getId() { return id; }

        public Set<DAItem> getItems() { return get_items.get(); }

        public boolean addItem(@NotNull DAItem i) {
            boolean r = Dungeon.this.addItem(i);
            if (r) i.setSection(this);
            return r;
        }

        public boolean removeItem(DAItem i) {
            boolean r = Dungeon.this.removeItem(i);
            if (r) i.setSection(null);
            return r;
        }

        public BoundingBox getBounds() { return bounds; }

        @Override
        protected void createGUI() {
            this.setSize(54);
            this.setNameSign(13);
            this.setItem(19, event_gui.toItem());
            this.setItem(20, item_gui.toItem());
            this.setItem(21, trigger_gui.toItem());
            this.setItem(22, newItemStack(
                    Material.ENDER_EYE,
                    ChatColor.AQUA + "Show/Hide Section",
                    List.of(ChatColor.GRAY + "Click to display or hide the bounding area of the section"),
                    Manageable.ITEM_ACTION_NSK, "displaySection")
            );
            this.setItem(24, newItemStack(
                    Material.TNT,
                    ChatColor.RED + "Delete Section",
                    List.of(ChatColor.DARK_RED + "Click to delete this section from \"" + dungeon_info.display_name + "\""),
                    Manageable.ITEM_ACTION_NSK, "deleteSection")
            );
        }

        public WithinBoundsCondition getWhithinBoundCondition() { return section_enter; }

        public @NotNull Location getCenter() { return LocationUtil.join(bounds.getWorld(), bounds.getCenter()); }

        @Override
        protected void action(DungeonMaster dm, @NotNull String action, String[] args, ClickType click) {
            switch (action) {
                case "showEvents" -> dm.manage(event_gui);
                case "showItems" -> dm.manage(item_gui);
                case "displaySection" -> {
                    if (!dm.hideSelection(this)) {
                        if (viewers.contains(dm)) viewers.add(dm);
                        dm.select(this, bounds.toLocationPair(dm.getWorld()));
                    }
                }
                case "deleteSection" -> this.attemptRemove(dm);
            }
        }

        public void attemptRemove(@NotNull DungeonMaster dm) {
            Manageable m;
            if (dm.isManaging()) {
                m = dm.getCurrentManageable();
                dm.holdManagement(true);
            } else m = null;
            GeneralListener.promptPlayer(dm, W + "Are you sure you want to delete section \"" + getName() + W + "\"? (yes/no)", text -> {
                if (text.equalsIgnoreCase("yes")) {
                    if (sections.size() == 1) {
                        dm.sendMessage(ER + "You can't delete the main sector of a dungeon! Use \"/dc delete\" to delete the dungeon");
                    } else {
                        this.delete();
                        if (mains == this) {
                            mains = sections.get(0);
                            dm.sendMessage(W + "Set the main dungeon sector to \"" + mains.getName() + "\"");
                        }
                        updateBoundingBox();
                        viewers.forEach(v -> v.hideSelection(this));
                        dm.sendMessage(DungeonMaster.Sender.CREATOR, IF + "Deleted section \"" + getName() + "\"!.");
                    }
                } else {
                    dm.sendMessage(DungeonMaster.Sender.CREATOR, IF + "Cancelled.");
                    dm.manage(m);
                }
            }, () -> {
                dm.sendMessage(DungeonMaster.Sender.CREATOR, IF + "Cancelled.");
                dm.holdManagement(false);
            });
        }

        @Override
        public void onDelete() {
            sections.remove(this);
            this.get_items.get().forEach(DAItem::delete);
            super.delete();
        }

        public Dungeon getDungeon() { return Dungeon.this; }

        public ItemStack toItem() {
            ItemStack i = newItemStack(displ_mat, getName());
            DAManageable.setAction(i, "open", id.toString());
            return i;
        }

        @Override
        public String toString() {
            return "Section{" +
                    "id=" + id +
                    ", bounds=" + bounds +
                    ", sectionEnter=" + section_enter +
                    ", ev=" + event_gui +
                    ", displ_mat=" + displ_mat +
                    '}';
        }
    }

    private final BoundingBox bounding_box;
    private final World world;
    private List<Section> sections = new ArrayList<>();
    private Section mains;
    private Location entrance_location;
    private boolean open = false;
    @OODPExclude
    private boolean running = false;
    private DungeonInfo dungeon_info = new DungeonInfo();
    private final Set<DAItem> items = new HashSet<>();
    private final Set<Trigger> triggers = new HashSet<>();

    @OODPExclude
    private final List<DungeonMaster> viewers = new ArrayList<>();
    @OODPExclude
    private Set<DungeonMaster> editors = new HashSet<>();
    public final Condition dungeon_start = new Condition(Type.DUNGEON_START, false) {

        @Override
        protected void createGUI() {}

        @Contract(pure = true)
        @Override
        public @Nullable DAItem create(DungeonMaster dm, String[] args) { return null; }

        { this.setName(Dungeon.this::getName); }
        @Override
        public boolean isSatisfied() { return Dungeon.this.isRunning(); }

        @Override
        protected @NotNull ItemStack genItemStack() {
            ItemStack i = new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
            return i;
        }
    };

    @OODPExclude
    private final List<Player> players = new ArrayList<>();

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
        } catch (DuplicateNameException | DungeonIntersectViolation e) {
            throw new RuntimeException(e.getCause());
        }
    }
    public Dungeon(String name, World w, BoundingBox bounds, DungeonInfo dg_info) throws DuplicateNameException, DungeonIntersectViolation {
        super(name);
        if (get(name) != null) throw new DuplicateNameException("Dungeon " + name + "already exists");
        Section si = getIntersectedSection(bounds);
        if (si != null) throw new DungeonIntersectViolation(
                "Couldn't create dungeon \"" + name + "\" due to section intersection with section \"" + si.getName() + "\" of dungeon \"" + si.getDungeon().getDungeonInfo().display_name + "\""
        );
        this.world = w;
        this.bounding_box = bounds;
        this.dungeon_info = dg_info;
        dungeons.add(this);
    }

    public Dungeon(UUID creator, String name, @NotNull Pair<Location, Location> sec) throws DuplicateNameException, DungeonIntersectViolation {
        super(name, true,  54);
        if (get(name) != null) throw new DuplicateNameException("Dungeon " + name + "already exists");
        Section si = getIntersectedSection(sec);
        if (si != null) throw new DungeonIntersectViolation("Couldn't create dungeon \"" + name + "\" due to section intersection with section \"" + si.getName() + "\" of dungeon \"" + si.getDungeon().getDungeonInfo().display_name + "\"");
        Player p = Bukkit.getPlayer(creator);
        this.dungeon_info.creator = creator;
        this.dungeon_info.creator_name = p == null ? "" : p.getName();
        this.world = sec.key().getWorld();
        this.dungeon_info.display_name = name;
        this.mains = new Section(sec, "Main_Sector");
        this.entrance_location = mains.getCenter();
        this.entrance_location.setY(mains.bounds.getMinY());
        while (entrance_location.getBlock().getType() != Material.AIR)
            entrance_location.add(0, 1, 0);
        bounding_box = new BoundingBox(sec);
        dungeons.add(this);
    }

    public World getWorld() { return this.world; }

    public void setMains(Section s) { if (sections.contains(s)) mains = s; }

    public Condition getDungeonStart() { return dungeon_start; }

    public static @Nullable Section getIntersectedSection(Pair<Location, Location> bb) {
        for (Dungeon d : dungeons)
            if (d.bounding_box.intersects(bb))
                for (Section s : d.sections)
                    if (s.bounds.intersects(bb))
                        return s;
        return null;
    }

    public static @Nullable Section getIntersectedSection(BoundingBox bb) {
        for (Dungeon d : dungeons)
            if (d.bounding_box.intersects(bb))
                for (Section s : d.sections)
                    if (s.bounds.intersects(bb))
                        return s;
        return null;
    }

    Section newSection(UUID id, String name, BoundingBox bounds) { return new Section(id, name, bounds); }
    public @NotNull String commandNewSection(@NotNull DungeonMaster dm, @NotNull String[] args) {
        if (dm.hasAreaSelected()) {
            if (dm.isEditing()) {
                Section is = getIntersectedSection(dm.getSelectedArea());
                if (is == null || dm.getCurrentDungeon().getSections().contains(is)) {
                    Dungeon d = dm.getCurrentDungeon();
                    Section s;
                    if (args.length > 0) s = newSection(dm.getSelectedArea(), args[0]);
                    else s = newSection(dm.getSelectedArea());
                    dm.clearSelection();
                    return OK + "Created \"" + d.getName() + "\" section \"" + s.getName() + "\"";
                } else return ER + "Cannot create section, selection intersects sector \"" + is.getName() + "\" in dungeon \"" + is.getDungeon().getDungeonInfo().display_name;
            } else return ER + "You must be currently editing a dungeon to create sections!";
        } else return ER + "You need to first select an area to create a section!";
    }

    public boolean addItem(@NotNull DAItem i) {
        if (i instanceof Trigger t) return triggers.add(t);
        else return items.add(i);
    }

    public boolean removeItem(DAItem i) {
        if (i instanceof Trigger t) return triggers.remove(t);
        else return items.remove(i);
    }

    public DAItem newTrigger() {
        Trigger t = new Trigger();
        triggers.add(t);
        return t;
    }

    public Set<Trigger> getTriggers() { return triggers; }

    public Location getEntranceLocation() { return entrance_location; }

    public void addPlayer(Player p) { this.players.add(p); }
    public void removePlayer(Player p) { this.players.remove(p); }

    public void addEditor(DungeonMaster dm) { this.editors.add(dm); }
    public void removeEditor(DungeonMaster dm) { this.editors.remove(dm); }
    public Set<DungeonMaster> getEditors() { return editors; }

    public void setOpen(boolean b) { this.open = b && editors.isEmpty(); }
    public boolean isOpen() { return this.open; }
    public boolean isClosed() { return !this.open; }

    public boolean isRunning() { return running; }

    public void start() {
        if (!this.running) {
            this.running = true;
            dungeon_start.onTrigger();
        }
    }

    public void stop() { if (this.running) this.running = false; }

    public Section getMains() { return mains; }

    public UUID getCreator() { return dungeon_info.creator; }

    public Set<DAItem> getItems() { return items; }

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
    public boolean contains(Location l) {
        if (!bounding_box.contains(l)) return false;
        for (Section s : sections)
            if (s.bounds.contains(l))
                return true;
        return mains.bounds.contains(l);
    }
    public boolean contains(Entity e) {
        if (!bounding_box.contains(e)) return false;
        for (Section s : sections)
            if (s.bounds.contains(e))
                return true;
        return mains.bounds.contains(e);
    }
    public boolean intersects(Entity e) {
        if (!bounding_box.intersects(e)) return false;
        for (Section s : sections)
            if (s.bounds.intersects(e))
                return true;
        return mains.bounds.intersects(e);
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
            dm.editDungeon(this);
    }

    public void attemptRemove(@NotNull DungeonMaster dm) {
        String name = this.getDungeonInfo().display_name;
        GeneralListener.promptPlayer(dm, W + "Are you sure you want to delete dungeon \"" + name + "\"? (yes/no)", text -> {
            if (text.equalsIgnoreCase("yes")) {
                this.delete();
                this.removeViewer(dm);
                dm.editDungeon(null);
                dm.clearOfDeletedDAItems();
                dm.sendMessage(DungeonMaster.Sender.CREATOR, IF + "Deleted dungeon \"" + name + "\"!");
            } else {
                dm.sendInfo(DungeonMaster.Sender.CREATOR, "Cancelled");
            }
        }, () -> dm.sendInfo(DungeonMaster.Sender.CREATOR, "Cancelled"));
    }

    public Section getSection(UUID id) {
        for (Section s : sections)
            if (s.getId().equals(id))
                return s;
        return null;
    }
    public Section getSection(Player p) {
        for (Section s : sections)
            if (s.bounds.contains(p))
                return s;
        return null;
    }
    public Section getSection(String s) {
        for (Section sc : sections)
            if (sc.getName().equals(s))
                return sc;
        return null;
    }
    public Section getSection(@NotNull Block b) { return getSection(b.getLocation()); }
    public Section getSection(Location l) {
        if (mains.bounds.contains(l)) return mains;
        for (Section sc : sections)
            if (sc.bounds.contains(l))
                return sc;
        return null;
    }

    public @NotNull List<Section> getSections() { return sections; }


    //Manageable
    @OODPExclude
    private final DAManageable sector_gui_list = new DAManageable(()->Dungeon.this.getName() + " sections", false, 54) {

        @Override
        protected void updateGUI() { this.fillBox(10, 7, 4, sections.stream().map(Section::toItem).toList()); }

        @Override
        protected void action(DungeonMaster dm, @NotNull String action, String[] args, ClickType click) {
            if (action.equalsIgnoreCase("open") && args.length > 0) {
                Section s = null;
                UUID id = UUID.fromString(args[0]);
                for (Section sc : sections)
                    if (sc.getId().equals(id)) {
                        s = sc;
                        break;
                    }
                if (s != null) dm.manage(s);
            }
        }
    };
    @OODPExclude
    private final ItemListGUI dungeon_event_list = new ItemListGUI("Events", dungeon_start);

    @Override
    protected void createGUI() {
        this.setNameSign(13);
    }
    
    @Override
    protected void updateGUI() {
        this.setItem(20, dungeon_event_list.toItem());
        this.setAction(21, newItemStack(Material.SHULKER_BOX, ChatColor.GREEN + "Sections", sections.stream().map(s -> ChatColor.GRAY + s.getName()).toList()), "showDungeonSections");
    }

    @Override
    protected void action(DungeonMaster dm, @NotNull String action, String[] args, ClickType click) {
        switch (action) {
            case "showDungeonSections" -> dm.manage(sector_gui_list);
        }
    }

    public DungeonInfo getDungeonInfo() { return dungeon_info; }
    public void setDungeonInfo(DungeonInfo info) { this.dungeon_info = info; }

    public static class DungeonInfo {

        public String display_name;
        public UUID creator;
        public String creator_name;
        public String difficulty;
        public String description;

    }

    @Override
    public void save() {
        this.items.forEach(da -> { if (da instanceof Action a) a.reset(); });
        FileManager.saveDungeon(this);
    }

    @Override
    public void onDelete() {
        viewers.forEach(dm -> {
            dm.hideSelection(this);
            sections.forEach(dm::hideSelection);
            if (dm.isEditing() && dm.getCurrentDungeon() == this)
                dm.editDungeon(this);
        });
        items.forEach(DAItem::delete);
        triggers.forEach(Trigger::delete);
        dungeons.remove(this);
        FileManager.deleteFile(this);
    }
}
