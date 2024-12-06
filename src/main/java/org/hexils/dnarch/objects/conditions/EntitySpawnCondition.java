package org.hexils.dnarch.objects.conditions;

import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Condition;
import org.hexils.dnarch.dungeon.DungeonMaster;
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
    public void onTrigger() {

    }
}
