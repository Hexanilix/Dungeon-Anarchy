package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.hetils.mpdl.listener.GeneralListener;
import org.hetils.mpdl.listener.PlayerChatRunnable;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

import static org.hetils.mpdl.GeneralUtil.log;

public abstract class Managable extends Named implements Deletable {
    public static Collection<Managable> instances = new ArrayList<>();


    public static class ManagableListener implements Listener {
        @EventHandler
        public void onInvClose(@NotNull InventoryCloseEvent event) {
            Inventory inv = event.getInventory();
            for (Managable m : instances)
                if (m.gui == inv) {
                    if (m.underManagable == null && m.aboveManagable != null) {
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

    public Managable() { instances.add(this); }

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

    public final void manage(Player p) {
        if (this.gui == null)
            this.createGUI();
        this.updateGUI();
        if (p != null) p.openInventory(this.gui);
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

    public void rename(@NotNull Player p) {
        rename(p, null);
    }

    public void rename(@NotNull Player p, Runnable onRename) {
        GeneralListener.confirmWithPlayer(p, ChatColor.AQUA + "Please type in the new name for" + this.name, s -> {
            name = s;
            createGUI();
            updateGUI();
            manage(p);
            if (onRename != null) onRename.run();
            return true;
        });
    }

    public final void doAction(DungeonMaster dm, String command) {
        if (command != null) {
            String[] v = command.split(" ", 1);
            action(dm, v[0], v.length > 1 ? v[1].split(" ") : new String[]{});
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

    protected void action(DungeonMaster dm, String action, String[] args) {}

    public boolean guiClickEvent(InventoryClickEvent event) {return true;}

    private int ls = 0;
    protected void setSize(int size) {
        if (gui == null)
            this.gui = org.hetils.mpdl.InventoryUtil.newInv(size, name);
        else if (ls != size) {
            Inventory i = org.hetils.mpdl.InventoryUtil.newInv(size, name);
            for (int j = 0; j < Math.min(size, gui.getSize()); j++)
                i.setItem(j, gui.getItem(j));
            this.gui = i;
        }
        ls = size;
    }

    public void delete() {
        instances.remove(this);
        this.gui = null;
        System.gc();
    }
}
