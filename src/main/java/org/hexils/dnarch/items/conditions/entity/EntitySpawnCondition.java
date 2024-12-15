package org.hexils.dnarch.items.conditions.entity;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Condition;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.actions.entity.EntitySpawnAction;
import org.jetbrains.annotations.NotNull;

import static org.hetils.mpdl.ItemUtil.newItemStack;

public class EntitySpawnCondition extends Condition {
    private final EntitySpawnAction entitySpawnAction;
    public EntitySpawnCondition(EntitySpawnAction entitySpawnAction) {
        super(Type.ENTITY_SPAWN_EVENT);
        this.entitySpawnAction = entitySpawnAction;
    }

    @Override
    public boolean isSatisfied() {
        return this.entitySpawnAction.isTriggered();
    }

    @Override
    protected void createGUI() {

    }

    @Override
    public void updateGUI() {

    }

    @Override
    protected ItemStack genItemStack() {
        return newItemStack(Material.TURTLE_EGG, "");
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
