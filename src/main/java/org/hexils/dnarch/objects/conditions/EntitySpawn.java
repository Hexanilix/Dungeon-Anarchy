package org.hexils.dnarch.objects.conditions;

import org.bukkit.entity.EntityType;

public class EntitySpawn {

    public final EntityType type;
    public final String name;
    public final double health;

    public EntitySpawn(EntityType type, String name, double health) {
        this.type = type;
        this.name = name;
        this.health = health;
    }

    public EntitySpawn(EntityType type, String name) {
        this(type, name, 0);
    }

    public EntitySpawn(EntityType type) {
        this(type, null, 0);
    }

}
