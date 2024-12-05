package org.hexils.dnarch.da.objects.conditions;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.da.Condition;
import org.hexils.dnarch.da.dungeon.DungeonMaster;
import org.hexils.dnarch.da.dungeon.Dungeon;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.hetils.mpdl.Item.newItemStack;

public class DungeonStart extends Condition {
    private Dungeon d;
     public DungeonStart(Dungeon d) {
         super(Type.DUNGEON_START);
         this.d = d;
     }

    @Override
    public boolean isSatisfied() {
        return d.isRunning();
    }

    @Override
    protected void createGUIInventory() {

    }

    @Override
    public void updateGUI() {

    }

    @Override
    protected ItemStack toItem() {
         ItemStack i = newItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, "Dungeon start");
         return i;
    }

    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args) {

    }

    @Override
    public void onTrigger() {

    }
}
