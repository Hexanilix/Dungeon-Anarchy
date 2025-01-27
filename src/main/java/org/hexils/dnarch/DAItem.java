package org.hexils.dnarch;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.item.ItemObtainable;
import org.hetils.mpdl.item.ItemUtil;
import org.hetils.mpdl.item.NSK;
import org.hexils.dnarch.commands.DungeonCreatorCommandExecutor;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

import static org.hetils.jgl17.StringUtil.readableEnum;

public abstract class DAItem extends DAManageable implements Idable, ItemObtainable, DAItemFactory {
    public static final NSK<String, String> ITEM_UUID = new NSK<>(new NamespacedKey(Main.plugin(), "item-uuid"), PersistentDataType.STRING);
    public static final Collection<DAItem> instances = new ArrayList<>();
    public static @Nullable DAItem get(String id) { return get(UUID.fromString(id)); }
    public static @Nullable DAItem get(ItemStack it) {
        String s;
        return (s = NSK.getNSK(it, ITEM_UUID)) == null ? null : DAItem.get(UUID.fromString(s));
    }
    public static @NotNull List<DAItem> get(ItemStack @NotNull [] it) { return get(List.of(it)); }
    public static @NotNull List<DAItem> get(@NotNull Iterable<ItemStack> it) {
        List<DAItem> l = new ArrayList<>();
        for (ItemStack i : it)
            l.add(get(i));
        return l;
    }
    public static @Nullable DAItem get(UUID id) {
        for (DAItem d : instances)
            if (d.getId().equals(id))
                return d;
        return null;
    }

    public static @Nullable DAItem commandNew(@NotNull Type t, DungeonMaster dm, @NotNull String[] args) {
        if (!t.isCreatable()) {
            dm.sendError(DungeonMaster.Sender.CREATOR, t.readableName() + " is not a creatable class");
            return null;
        }
        DAItem da = t.create(dm, args);
        if (da == null) {
            dm.sendError("Couldn't create " + readableEnum(t));
            return null;
        }
        Dungeon d = dm.getCurrentDungeon();
        Dungeon.Section s = null;
        if (da instanceof Action a) {
            if (a instanceof BlockAction b) {
                dm.deselectBlocks();
                Map<Dungeon.Section, Integer> sectionCounts = new HashMap<>();
                for (Block block : b.getAffectedBlocks()) {
                    Dungeon.Section section = d.getSection(block);
                    sectionCounts.put(section, sectionCounts.getOrDefault(section, 0) + 1);
                }
                s = sectionCounts.entrySet()
                        .stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
            }
        } else if (da instanceof Condition c) {

        }
        if (s == null) s = d.getSection(dm);
        if (s != null) da.setSection(s);
        da.setDungeon(d);
        return da;
    }

    @SuppressWarnings("unchecked")
    public static void setTabComplete(Function<String[], List<String>> r) {
        Class<?> c = getCallerClass();
        if (c != null && DAItem.class.isAssignableFrom(c)) {
            DungeonCreatorCommandExecutor.tabCompletions.put((Class<? extends DAItem>) c, r);
        }
    }

    public static @Nullable Class<?> getCallerClass() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stackTrace.length; i++) {
            try {
                Class<?> c = Class.forName(stackTrace[i].getClassName());
                if (c != DAItem.class && DAItem.class.isAssignableFrom(c))
                    return c;
            } catch (ClassNotFoundException ignore) {}
        }
        return null;
    }

    public static void clearOfDeletedDAItems(@NotNull Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            String id = NSK.getNSK(inv.getItem(i), DAItem.ITEM_UUID);
            if (id != null && DAItem.get(id) == null)
                inv.setItem(i, null);
        }
    }

    private UUID id;
    private final Type type;
    @OODPExclude
    private ItemStack item;
    @OODPExclude
    private final List<ItemStack> items = new ArrayList<>();
    Dungeon.Section section = null;
    @OODPExclude
    private Dungeon dungeon = null;

    public DAItem(Type type) { this(type, type.readableName()); }
    public DAItem(Type type, String name) { this(type, name, true); }
    public DAItem(Type type, boolean renameable) { this(type, type.readableName(), renameable); }
    public DAItem(Type type, String name, boolean renameable) {
        super(name, renameable);
        super.onRename(() -> {
            items.forEach(i -> ItemUtil.setName(i, getName()));
            item = this.genItemStack();
        });
        this.type = type;
        this.id = UUID.randomUUID();
        instances.add(this);
    }

    public final void setSection(Dungeon.Section s) {
        if (s != null && s == section) {
            if (s.getItems().contains(this)) return;
        } else if (section != null && section.getItems().contains(this)) section.removeItem(this);
        section = s;
        if (section != null) section.addItem(this);
    }

    public final Dungeon getDungeon() { return dungeon; }
    final void setDungeon(@NotNull Dungeon d) {
        this.dungeon = d;
        d.addItem(this);
    }

    public final UUID getId() { return id; }
    final void setId(UUID id) { this.id = id; }

    public final Type getType() { return type; }

    public final Dungeon.Section getSection() { return section; }

    public final ItemStack getItem() {
        if (item == null) this.item = this.genItemStack();
        ItemUtil.setName(item, getName());
        NSK.setNSK(item, ITEM_UUID, id.toString());
        items.add(item);
        return item;
    }
    protected abstract ItemStack genItemStack();

    @Override
    public final void onDelete() {
        onDeletion();
        instances.remove(this);
        dungeon.removeItem(this);
    }

    public void onDeletion() {}

    @Override
    public String toString() {
        return "DA_item{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }
}
