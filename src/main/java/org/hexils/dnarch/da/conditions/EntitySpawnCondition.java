package org.hexils.dnarch.da.conditions;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.da.Condition;
import org.hexils.dnarch.da.DM;
import org.jetbrains.annotations.NotNull;

public class EntitySpawnCondition extends Condition {
    public EntitySpawnCondition(Type type) {
        super(type);
    }

    @Override
    public boolean isSatisfied() {
        return false;
    }

    @Override
    protected Inventory createGUIInventory() {
        return null;
    }

    @Override
    public void updateGUI() {

    }

    @Override
    protected ItemStack toItem() {
        return null;
    }

    @Override
    protected void changeField(DM dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DM dm, String action, String[] args) {

    }

    @Override
    public void trigger() {}
}
