package org.hexils.dnarch.da.conditions;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.hetils.jgl17.Pair;
import org.hexils.dnarch.da.Condition;
import org.hexils.dnarch.da.DM;
import org.jetbrains.annotations.NotNull;

public class LocationCondition extends Condition {
    private Pair<Location, Location> bounds;
    public LocationCondition(Pair<Location, Location> bounds) {
        super(Type.LOCATION);
        this.bounds = bounds;
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
    public void trigger() {

    }
}
