package org.hexils.dnarch.da;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.MainListener;
import org.hexils.dnarch.NSK;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.hetils.mpdl.Item.newItemStack;
import static org.hexils.dnarch.Main.getNSK;
import static org.hexils.dnarch.Main.log;
import static org.hexils.dnarch.da.GUI.ITEM_RENAME;

public abstract class DA_item {
    public static final NSK ITEM_UUID = new NSK(new NamespacedKey("dungeon_anarchy", "item-uuid"), PersistentDataType.STRING);
    public static final Collection<DA_item> instances = new ArrayList<>();

    protected String name;
    private final UUID id;
    protected Inventory gui = null;

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

    //TODO FIX THIS MFFFFF
    public final void promptRename(Player p) {
//        new Renamer(this).rename(p);
    }

    public final Inventory getGUI() {
        return this.gui;
    }

    protected abstract Inventory createGUIInventory();

    private @NotNull ItemStack getNameSign() {
        ItemStack rename = newItemStack(Material.SPRUCE_SIGN, "Name: " + name, List.of(ChatColor.GRAY + "Click to rename"));
        Main.setNSK(rename, ITEM_RENAME, true);
        return rename;
    }

    private Inventory createGUI() {
        this.gui = this.createGUIInventory();
        this.gui.setItem(4, getNameSign());
        return this.gui;
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
        String s = (String) getNSK(it, ITEM_UUID);
        return s == null ? null : DA_item.get(UUID.fromString(s));
    }

    public static @Nullable DA_item get(Player p) {
        return p == null ? null : DA_item.get(p.getInventory().getItemInMainHand());
    }

    public final UUID getId() {
        return id;
    }

    public final void manage(Player p) {
        if (this.gui == null)
            this.createGUI();
        this.updateGUI();
        if (p != null) p.openInventory(this.gui);
    }

    //TODO this only works for one layer and doesnt properly return
    public final void manage(Player p, DA_item da) {
        this.manage(p);
        MainListener.onClose.put(this.gui, (event) -> {
            da.manage(p);
        });
    }

    public abstract void updateGUI();

    protected abstract ItemStack toItem();
    public final ItemStack getItem() {
        ItemStack i = this.toItem();
        Main.setNSK(i, ITEM_UUID, id.toString());
        return i;
    }

    protected abstract void changeField(DM dm, @NotNull String field, String value);

    protected abstract void action(DM dm, String action, String[] args);

    public boolean guiClickEvent(InventoryClickEvent event) {return true;}

    public final void setField(DM dm, String value) {
        if (value != null) {
            String[] v = value.split(" ", 1);
            if (v.length > 1)
                changeField(dm, v[0], v[1]);
        }
    }

    public final void doAction(DM dm, String command) {
        if (command != null) {
            String[] v = command.split(" ", 1);
            if (v.length > 1)
                action(dm, v[0], v[1].split(" "));
        }
    }

    public final void rename(String name) {
        this.name = name;
        this.createGUI();
    }
}
