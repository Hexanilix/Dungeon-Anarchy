package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.hetils.mpdl.GeneralListener;
import org.hetils.mpdl.NSK;
import org.hetils.mpdl.listener.PlayerChatRunnable;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hetils.mpdl.ItemUtil.newItemStack;


public abstract class Managable implements Deletable {
    public static final NSK MODIFIABLE = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-modifiable"), PersistentDataType.BOOLEAN);
    public static final NSK ITEM_FIELD_VALUE = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-field_value"), PersistentDataType.STRING);
    public static final NSK ITEM_ACTION = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-action"), PersistentDataType.STRING);
    public static final NSK SIGN_CHANGEABLE = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-sign_changeable"), PersistentDataType.BOOLEAN);
    public static final NSK ITEM_RENAME = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-prompt_rename"), PersistentDataType.BOOLEAN);

    public static void setField(ItemStack i, String field, String value) {
        NSK.setNSK(i, ITEM_FIELD_VALUE, field + ":" + value);
    }

    public static void setGuiAction(ItemStack i, String action) {
        NSK.setNSK(i, ITEM_ACTION, action);
    }
    
    public static Collection<Managable> instances = new ArrayList<>();

    public static class ManagableListener implements Listener {
        @EventHandler
        public void onInvClose(@NotNull InventoryCloseEvent event) {
            Inventory inv = event.getInventory();
            for (Managable m : instances)
                if (m.gui == inv) {
                    if (m.underManagable == null && m.aboveManagable != null && !m.is_being_renamed) {
                        m.aboveManagable.underManagable = null;
                        Managable am = m.aboveManagable;
                        m.aboveManagable = null;
                        new BukkitRunnable() { @Override public void run() { am.manage((Player) event.getPlayer()); } }.runTaskLater(Main.plugin, 1);
                    }
                    break;
                }
        }
    }

    protected Inventory gui = null;
    protected Managable aboveManagable = null;
    protected Managable underManagable = null;
    private ItemStack name_sign = null;
    private String name = "Managable";

    public Managable() { instances.add(this); }

    public Managable(String name) {
        this.name = name;
        genNameSign();
        instances.add(this);
    }

    public ItemStack genNameSign() {
        this.name_sign = newItemStack(Material.OAK_HANGING_SIGN, name != null ? (ChatColor.RESET + ChatColor.WHITE.toString() + name) : "null", List.of(ChatColor.GRAY + "Click to rename"), ITEM_RENAME, true);
        return name_sign;
    }
    public void setNameSign(int index) {
        this.gui.setItem(index, name_sign == null ? genNameSign() : name_sign);
    }

    @Contract(pure = true)
    public static @Nullable Managable get(Inventory inventory) {
        for (Managable m : instances)
            if (m.gui == inventory)
                return m;
        return null;
    }

    public final Inventory getGUI() {
        return this.gui;
    }

    public final void manage(@NotNull DungeonMaster dm) {
        manage(dm.p);
    }

    public final void manage(Player p) {
        if (this.gui == null)
            this.createGUI();
        this.updateGUI();
        p.openInventory(this.gui);
    }

    //TODO this only works for one layer
    public final void manage(@NotNull DungeonMaster dm, Managable da) {
        manage(dm.p, da);
    }
    public final void manage(Player p, @NotNull Managable da) {
        da.underManagable = this;
        aboveManagable = da;
        this.manage(p);
    }

    public String getName() {
        return name;
    }

    public void setName(String s) {
        this.name = s;
        setSize(ls);
    }

    private boolean is_being_renamed = false;
    public void rename(@NotNull Player p) {
        rename(p, null);
    }

    public void rename(@NotNull Player p, Runnable onRename) {
        is_being_renamed = true;
        GeneralListener.confirmWithPlayer(p, ChatColor.AQUA + "Please type in the new name for \"" + this.name + "\" or 'cancel':", s -> {
            name = s.replace(" ", "_");
            genNameSign();
            createGUI();
            updateGUI();
            manage(p);
            is_being_renamed = false;
            if (onRename != null) onRename.run();
            return true;
        }, () -> is_being_renamed = false);
    }

    public final void doAction(DungeonMaster dm, String command, InventoryClickEvent event) {
        if (command != null) {
            String[] v = command.split(" ", 1);
            action(dm, v[0], v.length > 1 ? v[1].split(" ") : new String[]{}, event);
        }
    }

    protected abstract void createGUI();

    public void updateGUI() {}

    public final void setField(DungeonMaster dm, String value) {
        if (value != null) {
            String[] v = value.split(" ", 1);
            if (v.length > 1)
                changeField(dm, v[0], v[1]);
        }
    }

    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {}

    protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {}

    public boolean guiClickEvent(InventoryClickEvent event) {return true;}

    private int ls = 0;
    private String ln = "";
    protected void setSize(int size) {
        if (gui == null || !ln.equals(name))
            this.gui = org.hetils.mpdl.InventoryUtil.newInv(size, name);
        else if (ls != size) {
            Inventory i = org.hetils.mpdl.InventoryUtil.newInv(size, name);
            for (int j = 0; j < Math.min(size, gui.getSize()); j++)
                i.setItem(j, gui.getItem(j));
            this.gui = i;
        }
        ls = size;
        ln = name;
    }

    public void delete() {
        instances.remove(this);
        this.gui = null;
        System.gc();
    }
}
