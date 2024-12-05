package org.hexils.dnarch.da;

import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.hetils.mpdl.listener.GeneralListener;
import org.hexils.dnarch.da.dungeon.DungeonMaster;
import org.hexils.dnarch.da.objects.Named;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public abstract class Managable extends Named {
    public static Collection<Managable> instances = new ArrayList<>();
    protected Inventory gui = null;

    public Managable() { instances.add(this); }

    public final Inventory getGUI() {
        return this.gui;
    }

    public final void manage(Player p) {
        if (this.gui == null)
            this.createGUI();
        this.updateGUI();
        if (p != null) p.openInventory(this.gui);
    }

    //TODO this only works for one layer and doesnt properly return
    public final void manage(Player p, Managable da) {
        this.manage(p);
        GeneralListener.runOnEvent(GeneralListener.EventType.INVENTORY_CLOSE, this.gui, () -> da.manage(p), EventPriority.LOW);
    }

    public final void doAction(DungeonMaster dm, String command) {
        if (command != null) {
            String[] v = command.split(" ", 1);
            action(dm, v[0], v.length > 1 ? v[1].split(" ") : new String[]{});
        }
    }

    @Override
    public final void rename(String name) {
        this.name = name;
        this.createGUI();
    }

    public abstract void createGUI();

    public abstract void updateGUI();

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
}
