package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
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

import static org.hetils.mpdl.General.log;

public abstract class Managable extends Named {
    public static Collection<Managable> instances = new ArrayList<>();
    protected Inventory gui = null;

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
    public final void manage(Player p, Managable da) {
        this.manage(p);
        GeneralListener.runOnEvent(GeneralListener.EventType.INVENTORY_CLOSE, this.gui, () -> new BukkitRunnable() { @Override public void run() { da.manage(p); } }.run(), EventPriority.LOW);
    }

    public void rename(@NotNull Player p) {
        rename(p, null);
    }

    public void rename(@NotNull Player p, Runnable onRename) {
        p.sendMessage(ChatColor.AQUA + "Please type in the new name for " + this.name);
        GeneralListener.runOnEvent(GeneralListener.EventType.PLAYER_CHAT, p, new PlayerChatRunnable() {
            @Override
            public void run() {}

            @Override
            public boolean run(String s) {
                name = s;
                createGUI();
                manage(p);
                if (onRename != null) onRename.run();
                return true;
            }
        }, EventPriority.LOW);
    }

    public final void doAction(DungeonMaster dm, String command) {
        if (command != null) {
            String[] v = command.split(" ", 1);
            action(dm, v[0], v.length > 1 ? v[1].split(" ") : new String[]{});
        }
    }

    public abstract void createGUI();

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

    protected void guiSize(int size) {
        if (gui == null)
            this.gui = org.hetils.mpdl.Inventory.newInv(size, name);
        else {
            Inventory i = org.hetils.mpdl.Inventory.newInv(size, name);
            for (int j = 0; j < Math.min(size, gui.getSize()); j++)
                i.setItem(j, gui.getItem(j));
            this.gui = i;
        }
    }

    protected void delete() {
        instances.remove(this);
        this.gui = null;
        System.gc();
    }
}
