package org.hexils.dnarch.objects.conditions;

import org.hexils.dnarch.dungeon.DungeonMaster;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum Type {
    DUNGEON_START, DISTANCE, LOCATION, ENTITY_SPAWN;

    public static @Nullable Type get(@NotNull String arg) {
        for (Type t : Type.values())
            if (t.name().equalsIgnoreCase(arg))
                return t;
        return null;
    }
}