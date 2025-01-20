package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.ItemUtil;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.actions.ModifyBlock;
import org.hexils.dnarch.items.actions.ReplaceBlock;
import org.hexils.dnarch.items.actions.entity.EntitySpawnAction;
import org.hexils.dnarch.items.conditions.WithinDistance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Action.getEnum;
import static org.hexils.dnarch.Action.toReadableFormat;
import static org.hexils.dnarch.Main.log;
import static org.hexils.dnarch.commands.DungeonCommandExecutor.ER;

public abstract class DAItem extends Manageable implements Idable, Deletable, DAItemFactory {
    public static final NSK<String, String> ITEM_UUID = new NSK<>(new NamespacedKey(Main.plugin, "item-uuid"), PersistentDataType.STRING);
    public static final Collection<DAItem> instances = new ArrayList<>();
    public static @Nullable DAItem get(String id) { return get(UUID.fromString(id)); }
    public static @Nullable DAItem get(ItemStack it) {
        String s = (String) NSK.getNSK(it, ITEM_UUID);
        return s == null ? null : DAItem.get(UUID.fromString(s));
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
            dm.sendError(ER + "Couldn't create " + toReadableFormat(t.name()));
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
        if (s == null) s = d.getSection(dm.p);
        log("Section@DAItem: " + s);
        if (s != null) da.setSection(s);
        d.addItem(da);
        return da;
    }

    public static List<String> getTabCompleteFor(@NotNull Type t, String[] args) {
        List<String> s = new ArrayList<>();
        switch (t) {
            case ENTITY_SPAWN_ACTION -> { if (args.length == 1) s = Arrays.stream(EntityType.values()).map(e -> e.name().toLowerCase()).toList(); }
            case MODIFY_BLOCK -> { if (args.length == 1) s = Arrays.stream(ModifyBlock.ModType.values()).map(e -> e.name().toLowerCase()).toList(); }
        }
        return s;
    }

    private UUID id;
    private final Type type;
    @OODPExclude
    private ItemStack item;
    @OODPExclude
    private final List<ItemStack> items = new ArrayList<>();
    Dungeon.Section section = null;

    public DAItem(Type type) { this(type, type.readableName()); }
    public DAItem(Type type, String name) { this(type, name, true); }
    public DAItem(Type type, boolean renameable) { this(type, type.readableName(), renameable); }
    public DAItem(Type type, String name, boolean renameable) {
        super(name, renameable);
        this.type = type;
        this.id = UUID.randomUUID();
        instances.add(this);
    }

    public void setSection(Dungeon.Section s) {
        if (s == section)
            if (s.getItems().contains(this)) return;
        else if (section != null) section.removeItem(this);
        section = s;
        if (section != null) section.addItem(this);
    }

    @Override
    public void rename(@NotNull DungeonMaster dm) { rename(dm, null); }
    @Override
    public void rename(@NotNull DungeonMaster dm, Runnable onRename) {
        super.rename(dm, () -> {
            if (onRename != null) onRename.run();
            items.forEach(i -> ItemUtil.setName(i, getName()));
            item = this.genItemStack();
        });
    }

    void setId(UUID id) { this.id = id; }

    public Type getType() { return type; }

    public final UUID getId() { return id; }

    public Dungeon.Section getSection() { return section; }

    protected abstract ItemStack genItemStack();
    public final ItemStack getItem() {
        if (item == null) this.item = this.genItemStack();
        ItemUtil.setName(item, getName());
        NSK.setNSK(item, ITEM_UUID, id.toString());
        items.add(item);
        return item;
    }

    @Override
    public void onDelete() {
        instances.remove(this);
    }

    @Override
    public String toString() {
        return "DA_item{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }
}
