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
import org.hetils.mpdl.LocationUtil;
import org.hetils.mpdl.NSK;
import org.hetils.mpdl.VectorUtil;
import org.hetils.mpdl.GeneralListener;
import org.hexils.dnarch.*;
import org.hexils.dnarch.items.conditions.DungeonStart;
import org.hexils.dnarch.items.conditions.WithinBoundsCondition;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;


import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;
import static org.hexils.dnarch.commands.DungeonCommandExecutor.*;
import static org.hexils.dnarch.commands.DungeonCreatorCommandExecutor.W;

public class Dungeon extends Manageable implements Savable {
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
            if (d.isWithinDungeon(loc))
                return d;
        return null;
    }

    @Contract(pure = true)
    public static @Nullable Dungeon get(Entity e) { return e != null ? get(e.getLocation()) : null; }

    public class Section extends Manageable {
        private UUID id;
        private final List<DA_item> items = new ArrayList<>();
        private final LocationUtil.BoundingBox bounds;
        private final WithinBoundsCondition sectionEnter;
        private final ConditionGUI ev;
        private final ItemList it = new ItemList();

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
            this.sectionEnter.setRenameable(false);
            this.ev = new ConditionGUI(name + " events", List.of(sectionEnter));
            sections.add(this);
        }

        @Override
        public void rename(@NotNull DungeonMaster dm, Runnable onRename) { super.rename(dm, () -> { if (onRename != null) onRename.run(); ev.setName(getName()); it.setName(getName());}); }

        public UUID getId() {
            return id;
        }

        @Override
        protected void createGUI() {
            this.setSize(54);
            this.setNameSign(13);
            this.setItem(20, newItemStack(
                    Material.BARREL,
                    ChatColor.GREEN + "Events",
                    List.of(ChatColor.GRAY + "Shows the list of events this section produces"),
                    ITEM_ACTION, "showEvents")
            );
            this.setItem(21, newItemStack(
                    Material.ENDER_CHEST,
                    ChatColor.GREEN + "Items",
                    List.of(ChatColor.GRAY + "Shows the list of items this section contains"),
                    ITEM_ACTION, "showItems")
            );
            this.setItem(22, newItemStack(
                    Material.ENDER_EYE,
                    ChatColor.AQUA + "Show/Hide Section",
                    List.of(ChatColor.GRAY + "Click to display or hide the bounding area of the section"),
                    ITEM_ACTION, "displaySection")
            );
            this.setItem(24, newItemStack(
                    Material.TNT,
                    ChatColor.RED + "Delete Section",
                    List.of(ChatColor.DARK_RED + "Click to delete this section from \"" + dungeon_info.display_name + "\""),
                    ITEM_ACTION, "deleteSection")
            );
        }

        @Override
        public void updateGUI() {

        }

        public List<DA_item> getItems() { return items; }

        public void addItem(@NotNull DA_item i) { items.add(i); }

        public boolean removeItem(DA_item i) {
            if (items.remove(i)) {
                i.delete();
                return true;
            } else return false;
        }

        public @NotNull Location getCenter() { return LocationUtil.join(bounds.getWorld(), bounds.getCenter()); }

        private class ItemList extends Manageable {
            public ItemList() {}

            @Override
            protected void createGUI() {
                this.setSize(54);
//                this.setName(Dungeon.this.dungeon_info.display_name + " items");
            }

            @Override
            public void updateGUI() {
                for (int i = 9; i < Math.min(54, 9+items.size()); i++) {
                    ItemStack it = items.get(i-9).getItem();
                    Manageable.setGuiAction(it, "give", items.get(i-9).getId().toString());
                    this.setItem(i, it);
                }
            }

            @Override
            protected void action(DungeonMaster dm, @NotNull String action, String[] args, InventoryClickEvent event) {
                if (action.equalsIgnoreCase("give") && args.length > 0) {
                    log(args[0]);
                    DA_item da = DA_item.get(args[0]);
                    if (da != null) dm.giveItem(da);
                }
            }
        }

        @Override
        protected void action(DungeonMaster dm, @NotNull String action, String[] args, InventoryClickEvent event) {
            switch (action) {
                case "showEvents" -> ev.manage(dm, this);
                case "showItems" -> it.manage(dm, this);
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
                dm.closeInventory();
            } else m = null;
            GeneralListener.confirmWithPlayer(dm.p, W + "Are you sure you want to delete section \"" + getName() + W + "\"? (yes/no)", text -> {
                if (text.equalsIgnoreCase("yes")) {
                    if (mains == this) {
                        if (sections.size() != 1 || this != sections.get(0)) {
                            mains = sections.get(0);
                            dm.sendMessage(W + "Set the main dungeon sector to \"" + mains.getName() + "\"");
                        } else {
                            dm.sendMessage(ER + "You can't delete the main sector of a dungeon! Use \"/dc delete\" to delete the dungeon");
                            return true;
                        }
                    }
                    this.delete();
                    updateBoundingBox();
                    dm.sendMessage(IF + "Deleted section \"" + getName() + "\"!.");
                } else {
                    dm.sendMessage(IF + "Cancelled.");
                    if (m != null) m.manage(dm);
                }
                return true;
            }, () -> dm.sendMessage(IF + "Cancelled."));
        }

        @Override
        public void delete() {
            sections.remove(this);
            super.delete();
        }

        public Dungeon getDungeon() { return Dungeon.this; }

        private Material displ_mat = Material.GREEN_CONCRETE;

        public ItemStack toItem() {
            ItemStack i = newItemStack(displ_mat, getName());
            NSK.setNSK(i, ITEM_ACTION, id.toString());
            return i;
        }

        @Override
        public String toString() {
            return "Section{" +
                    "id=" + id +
                    ", bounds=" + bounds +
                    ", sectionEnter=" + sectionEnter +
                    ", ev=" + ev +
                    ", displ_mat=" + displ_mat +
                    '}';
        }
    }

    private LocationUtil.BoundingBox bounding_box;
    private World world;
//    private final List<DA_item> items = new ArrayList<>();
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
        } catch (DuplicateNameException | DungeonIntersectViolation e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public Dungeon(UUID creator, String name, @NotNull Pair<Location, Location> sec) throws DuplicateNameException, DungeonIntersectViolation {
        super(name);
        if (get(name) != null) throw new DuplicateNameException("Dungeon " + name + "already exists");
        Section si = getIntersectedSection(sec);
        if (si != null) throw new DungeonIntersectViolation("Couldn't create dungeon \"" + name + "\" due to section intersection with section \"" + si.getName() + "\" of dungeon \"" + si.getDungeon().getDungeonInfo().display_name + "\"");
        Player p = Bukkit.getPlayer(creator);
        this.dungeon_info.creator = creator;
        this.dungeon_info.creator_name = p == null ? "" : p.getName();
        this.world = sec.key().getWorld();
        this.dungeon_info.display_name = name;
        this.mains = new Section(sec, "Main_Sector");
        bounding_box = new LocationUtil.BoundingBox(sec);
        dungeons.add(this);
    }

    public static @Nullable Section getIntersectedSection(Pair<Location, Location> bb) {
        for (Dungeon d : dungeons)
            if (d.bounding_box.intersects(bb))
                for (Section s : d.sections)
                    if (s.bounds.intersects(bb))
                        return s;
        return null;
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

    public List<DA_item> getItems() { return sections.stream().map(Section::getItems).flatMap(List::stream).collect(Collectors.toList()); }

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

    public void attemptRemove(@NotNull DungeonMaster dm) {
        String name = this.getDungeonInfo().display_name;
        GeneralListener.confirmWithPlayer(dm.p, W + "Are you sure you want to delete dungeon \"" + name + "\"? (yes/no)", text -> {
            if (text.equalsIgnoreCase("yes")) {
                this.delete();
                this.removeViewer(dm);
                dm.setCurrentDungeon(null);
                dm.sendMessage(IF + "Deleted dungeon \"" + name + "\"!");
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
    public Section getSection(@NotNull Block b) {
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
    
    private class SectorGUIList extends Manageable {
        public SectorGUIList() {}

        @Override
        protected void createGUI() {
            this.setSize(54);
            this.setName(Dungeon.this.dungeon_info.display_name + " sections");
            updateGUI();
        }

        @Override
        public void updateGUI() {
            this.fillBox(0, 9, 6, sections.stream().map(Section::toItem).toList());
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

    private class DungeonEventList extends Manageable {
        public DungeonEventList() {}

        @Override
        protected void createGUI() {
            this.setSize(54);
            this.setName(Dungeon.this.dungeon_info.display_name + " events");
            List<DA_item> l = dungeon_items.getItems();
            for (int i = 0; i < Math.min(54, l.size()); i++) {
                ItemStack it = l.get(i).getItem();
                setGuiAction(it, "give " + l.get(i).getId().toString());
                this.setItem(i, it);
            }
            updateGUI();
        }

        @Override
        protected void action(DungeonMaster dm, @NotNull String action, String[] args, InventoryClickEvent event) {
            if (action.equalsIgnoreCase("give") && args.length > 0) {
                DA_item da = DA_item.get(args[0]);
                if (da != null) dm.giveItem(da);
                else dm.sendMessage(ER + "This item does not exist!");
            }
        }
    }
    
    private final SectorGUIList sector_gui_list = new SectorGUIList();
    private final DungeonEventList dungeon_event_list = new DungeonEventList();

    @Override
    protected void createGUI() {
        this.setSize(54);
        this.setNameSign(13);
    }
    
    @Override
    public void updateGUI() {
        if (sections != null) {
            this.setItem(20, newItemStack(Material.REDSTONE_LAMP, ChatColor.GREEN + "Events", sections.stream().map(s -> ChatColor.GRAY + s.getName()).toList(), ITEM_ACTION, "showDungeonEvents"));
            this.setItem(21, newItemStack(Material.SHULKER_BOX, ChatColor.GREEN + "Sections", sections.stream().map(s -> ChatColor.GRAY + s.getName()).toList(), ITEM_ACTION, "showDungeonSections"));
        }
    }

    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DungeonMaster dm, @NotNull String action, String[] args, InventoryClickEvent event) {
        switch (action) {
            case "showDungeonEvents" -> dungeon_event_list.manage(dm, this);
            case "showDungeonSections" -> sector_gui_list.manage(dm, this);
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
        public List<DA_item> getItems() { return List.of(start); }
        
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
