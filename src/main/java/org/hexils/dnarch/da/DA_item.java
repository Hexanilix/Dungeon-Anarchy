package org.hexils.dnarch.da;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.da.dungeon.DungeonMaster;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

import static org.hetils.mpdl.General.log;
import static org.hetils.mpdl.Item.newItemStack;
import static org.hexils.dnarch.da.GUI.ITEM_RENAME;

public abstract class DA_item extends Managable {
    public static final NSK ITEM_UUID = new NSK(new NamespacedKey("dungeon_anarchy", "item-uuid"), PersistentDataType.STRING);
    public static final Collection<DA_item> instances = new ArrayList<>();

    protected String name;
    private final UUID id;

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

    protected abstract void createGUIInventory();

    @Override
    public void createGUI() {
        this.createGUIInventory();
        if (this.gui != null) this.gui.setItem(4, getNameSign());
        else log(Level.WARNING + "Attempted to create GUI is null from item " + this.id.toString() +". This probably shouldn't happen!");
    }

    private @NotNull ItemStack getNameSign() {
        ItemStack rename = newItemStack(Material.SPRUCE_SIGN, "Name: " + name, List.of(ChatColor.GRAY + "Click to rename"));
        NSK.setNSK(rename, ITEM_RENAME, true);
        return rename;
    }

    public DA_item() {
        this.id = UUID.randomUUID();
        instances.add(this);
    }

    public static @Nullable DA_item get(UUID id) {
        for (DA_item d : instances)
            if (d.getId().equals(id))
                return d;
        return null;
    }

    public static @Nullable DA_item get(ItemStack it) {
        String s = (String) NSK.getNSK(it, ITEM_UUID);
        return s == null ? null : DA_item.get(UUID.fromString(s));
    }

    public static @Nullable DA_item get(Player p) {
        return p == null ? null : DA_item.get(p.getInventory().getItemInMainHand());
    }

    public final UUID getId() {
        return id;
    }

    protected abstract ItemStack toItem();
    public final ItemStack getItem() {
        ItemStack i = this.toItem();
        NSK.setNSK(i, ITEM_UUID, id.toString());
        return i;
    }
}
