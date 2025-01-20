package org.hexils.dnarch.items;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.DAItem;
import org.hexils.dnarch.DungeonMaster;
import org.jetbrains.annotations.NotNull;

import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Action.toReadableFormat;

public class EntitySpawn extends DAItem {

    public final EntityType type;
    public final String name;
    public final double health;

    public EntitySpawn(EntityType type) { this(type, toReadableFormat(type.name()), 20); }
    public EntitySpawn(EntityType type, String name) { this(type, name, 10); }
    public EntitySpawn(@NotNull EntityType type, String name, double health) {
        super(Type.ENTITY_SPAWN);
        this.type = type;
        this.name = name;
        this.health = health;
    }

    @Override
    protected void createGUI() {
        this.setSize(54);
        this.setNameSign(13);
//        this.setItem(12, newItemStack(Material.REPEATER, "Spawn Amount"));
    }

    @Override
    protected void updateGUI() {
        this.setItem(11, newItemStack(Material.getMaterial(type.name()+"_SPAWN_EGG"), toReadableFormat(type.name())));
    }

    @Override
    protected ItemStack genItemStack() {
        return newItemStack(Material.getMaterial(type.name()+"_SPAWN_EGG"), getName());
    }

    @Override
    public DAItem create(DungeonMaster dm, String[] args) {
        return new EntitySpawn(EntityType.ZOMBIE);
    }
}
