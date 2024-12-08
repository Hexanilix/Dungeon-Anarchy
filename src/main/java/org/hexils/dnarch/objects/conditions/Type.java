package org.hexils.dnarch.objects.conditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum Type {
    DUNGEON_START, DISTANCE, WITHIN_BOUNDS, ENTITY_SPAWN, NOT;

    public static @Nullable Type get(@NotNull String arg) {
        for (Type t : Type.values())
            if (t.name().equalsIgnoreCase(arg))
                return t;
        return null;
    }
}