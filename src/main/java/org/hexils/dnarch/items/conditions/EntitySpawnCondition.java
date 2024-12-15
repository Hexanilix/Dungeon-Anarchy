package org.hexils.dnarch.items.conditions;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Condition;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.actions.Spawn;
import org.jetbrains.annotations.NotNull;

public class EntitySpawnCondition extends Condition {
    private final Spawn spawn;
    public EntitySpawnCondition(Spawn spawn) {
        super(Type.ENTITY_SPAWN_EVENT);
        this.spawn = spawn;
    }

    @Override
    public boolean isSatisfied() {
        return this.spawn.isTriggered();
    }

    @Override
    protected void createGUI() {

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
    protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {

    }

    @Override
    protected void onTrigger() {

    }
}
