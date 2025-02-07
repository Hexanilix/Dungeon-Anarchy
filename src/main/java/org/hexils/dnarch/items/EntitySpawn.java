package org.hexils.dnarch.items;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.DAItem;
import org.hexils.dnarch.DungeonMaster;
import org.jetbrains.annotations.NotNull;

import static org.hetils.jgl17.StringUtil.readableEnum;
import static org.hetils.mpdl.item.ItemUtil.newItemStack;

public class EntitySpawn extends DAItem {

    private EntityType type;
    private final String name;
    private final double health;

    public EntitySpawn() { this(null, null, 0); }
    public EntitySpawn(EntityType type) { this(type, readableEnum(type), 20); }
    public EntitySpawn(EntityType type, String name) { this(type, name, 10); }
    public EntitySpawn(EntityType type, String name, double health) {
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

    public EntityType getEntType() { return type; }
    public String getEntName() { return name; }
    public double getEntHealth() { return health; }

    @Override
    protected void updateGUI() { this.setItem(11, newItemStack(Material.getMaterial(type.name()+"_SPAWN_EGG"), readableEnum(type))); }

    @Override
    protected ItemStack genItemStack() { return newItemStack(Material.getMaterial(type.name()+"_SPAWN_EGG"), getName()); }

    @Override
    public DAItem create(DungeonMaster dm, String @NotNull [] args) {
        if (args.length > 0)
            type = EntityType.valueOf(args[0].toUpperCase());
        if (type == null) type = EntityType.ZOMBIE;
        return this;
    }
}
