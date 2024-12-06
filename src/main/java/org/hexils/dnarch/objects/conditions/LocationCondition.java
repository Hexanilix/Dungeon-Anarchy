package org.hexils.dnarch.objects.conditions;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.hetils.jgl17.Pair;
import org.hexils.dnarch.Condition;
import org.hexils.dnarch.dungeon.DungeonMaster;
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
    protected void createGUIInventory() {

    }

    @Override
    public void updateGUI() {

    }

    @Override
    protected ItemStack toItem() {
        return null;
    }

    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args) {

    }

    @Override
    protected void onTrigger() {

    }
}
