package org.hexils.dnarch.items.conditions;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Condition;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.dungeon.Dungeon;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;


import static org.hetils.mpdl.ItemUtil.newItemStack;

public class DungeonStart extends Condition {
    private Dungeon d;
     public DungeonStart(Dungeon d) {
         super(Type.DUNGEON_START, false);
         this.d = d;
     }

    @Override
    public boolean isSatisfied() {
        return d.isRunning();
    }

    @Override
    protected void createGUI() {

    }

    @Override
    protected void updateGUI() {

    }

    @Override
    protected ItemStack genItemStack() {
         ItemStack i = newItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, "Dungeon start");
         return i;
    }

    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {

    }

    @Override
    protected void onTrigger() {
    }
}
