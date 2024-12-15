package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.hetils.mpdl.ItemUtil;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.dungeon.Dungeon;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.items.EntitySpawn;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.actions.DestroyBlock;
import org.hexils.dnarch.items.actions.ModifyBlock;
import org.hexils.dnarch.items.actions.ReplaceBlock;
import org.hexils.dnarch.items.actions.Spawn;
import org.hexils.dnarch.items.conditions.WithinDistance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Action.getEnum;
import static org.hexils.dnarch.Action.toReadableFormat;
import static org.hexils.dnarch.Main.log;
import static org.hexils.dnarch.commands.DungeonCommandExecutor.ER;

public abstract class DA_item extends Manageable {
    public static final NSK ITEM_UUID = new NSK(new NamespacedKey("dungeon_anarchy", "item-uuid"), PersistentDataType.STRING);
    public static final Collection<DA_item> instances = new ArrayList<>();
    public static @Nullable DA_item get(String id) { return get(UUID.fromString(id)); }
    public static @Nullable DA_item get(ItemStack it) {
        String s = (String) NSK.getNSK(it, ITEM_UUID);
        return s == null ? null : DA_item.get(UUID.fromString(s));
    }
    public static @Nullable DA_item get(UUID id) {
        for (DA_item d : instances)
            if (d.getId().equals(id))
                return d;
        return null;
    }

    private final UUID id;

    public static @Nullable DA_item commandNew(@NotNull Type t, DungeonMaster dm, @NotNull String[] args) {
        DA_item da = null;
        log(Arrays.toString(args));
        switch (t) {
            case REPLACE_BLOCK -> {
                Material mat = args.length >= 1 ? Material.getMaterial(args[0].toUpperCase()) : null;
                if (mat != null) {
                    da = new ReplaceBlock(dm.getSelectedBlocks(), mat);
                } else dm.sendMessage(ChatColor.RED + "Please select a valid material!");
            }
            case DESTROY_BLOCK -> {
                da = new DestroyBlock(dm.getSelectedBlocks());
            }
            case ENTITY_SPAWN_ACTION -> {
                EntityType et = args.length >= 1 ? getEnum(EntityType.class, args[0]) : null;
                log(et);
                if (et != null) {
                    da = new Spawn(new EntitySpawn(et), org.hetils.mpdl.BlockUtil.toLocations(dm.getSelectedBlocks()));
                } else dm.sendMessage(ChatColor.RED + "Please select a valid entity type");
            }
            case MODIFY_BLOCK -> {
                ModifyBlock.ModType mt = args.length >= 1 ? ModifyBlock.ModType.get(args[0]) : null;
                if (mt != null) {
                    da = new ModifyBlock(dm.getSelectedBlocks(), mt);
                } else dm.sendMessage(ChatColor.RED + "Please select a valid mod type!");
            }



            case WITHIN_DISTANCE -> {
                Location l = null;
                if (dm.hasBlocksSelected())
                    l = org.hetils.mpdl.LocationUtil.getCenter(dm.getSelectedBlocks().stream().map(Block::getLocation).toList());
                if (l == null) l = dm.getLocation();
                da = new WithinDistance(l);
            }
            case WITHIN_BOUNDS -> {

            }
            default -> {

            }
        }
        if (da == null) {
            dm.sendMessage(ER + "Couldn't create " + toReadableFormat(t.name()));
            return null;
        }
        Dungeon d = dm.getCurrentDungeon();
        Dungeon.Section s = d.getSection(dm.p);
        if (da instanceof Action a) {
            if (a instanceof BlockAction b) {
                dm.deselectBlocks();
                Map<Dungeon.Section, Integer> sectionCounts = new HashMap<>();
                log(b.getAffectedBlocks().size());
                for (Block block : b.getAffectedBlocks()) {
                    Dungeon.Section section = d.getSection(block);
                    log(section);
                    sectionCounts.put(section, sectionCounts.getOrDefault(section, 0) + 1);
                }
                s = sectionCounts.entrySet()
                        .stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
            }
            if (s != null) {
                s.addItem(a);
                return a;
            } else {
                dm.sendMessage(ER + "Error occurred when getting section");
                return null;
            }
        } else if (da instanceof Condition c) {
            s.addItem(c);
        }
        return da;
    }

    public static List<String> getTabCompleteFor(@NotNull Type t, String[] args) {
        log(Arrays.toString(args));
        List<String> s = new ArrayList<>();
        switch (t) {
            case ENTITY_SPAWN_ACTION -> { if (args.length == 1) s = Arrays.stream(EntityType.values()).map(e -> e.name().toLowerCase()).toList(); }
            case MODIFY_BLOCK -> { if (args.length == 1) s = Arrays.stream(ModifyBlock.ModType.values()).map(e -> e.name().toLowerCase()).toList(); }
        }
        return s;
    }

//    private final class Renamer {
//        private final DA_item da;
//        public Renamer(DA_item da) {
//            this.da = da;
//        }
//
//        private void rename(Player p) {
//            Inventory inv = Bukkit.createInventory(null, InventoryType.ANVIL, "Input new name:");
//            inv.setItem(0, getNameSign());
//            MainListener.onOpen.put(inv, (event) -> {
//                new Thread() {
//                    private Inventory inve = event.getInventory();
//                    private String text;
//                    private boolean run = true;
//                    public void run() {
//                        MainListener.onClick.put(inve, (event) -> {
//                            if (event.getSlotType() == InventoryType.SlotType.RESULT) {
//                                run = false;
//                                DA_item.this.rename(text);
//                                DA_item.this.manage(p);
//                                return true;
//                            }
//                            return false;
//                        });
//
//                        ItemStack st;
//                        while (run) {
//                            st = inve.getItem(2);
//                            log(Arrays.toString(inve.getContents()));
//                            if (st != null && st.hasItemMeta())
//                                text = st.getItemMeta().getDisplayName();
//                            try {
//                                Thread.sleep(100);
//                            } catch (InterruptedException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }
//                    }
//                }.start();
//                return true;
//            });
//            p.openInventory(inv);
//        }
//    }

    @Override
    public void rename(@NotNull DungeonMaster dm) {
        super.rename(dm, () -> items.forEach(i -> ItemUtil.setName(i, getName())));
    }

    public DA_item() {
        this.id = UUID.randomUUID();
        instances.add(this);
    }

    public DA_item(String name) {
        super(name);
        this.id = UUID.randomUUID();
        instances.add(this);
    }

    public final UUID getId() {
        return id;
    }

    private final List<ItemStack> items = new ArrayList<>();

    protected abstract ItemStack toItem();
    public final ItemStack getItem() {
        ItemStack i = this.toItem();
        NSK.setNSK(i, ITEM_UUID, id.toString());
        items.add(i);
        return i;
    }
}
