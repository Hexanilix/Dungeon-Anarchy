package org.hexils.dnarch.items;

import org.jetbrains.annotations.NotNull;

public enum Type {
    //Actions
    DESTROY_BLOCK,
    ENTITY_MOD,
    REPLACE_BLOCK,
    DOOR,
    ENTITY_SPAWN_ACTION,
    MODIFY_BLOCK,

    //Conditions
    DUNGEON_START,
    WITHIN_DISTANCE,
    WITHIN_BOUNDS,
    NOT,
    ENTITY_DEATH_EVENT,
    ENTITY_SPAWN_EVENT,
    BLOCK_CHANGE_EVENT,

    //Other
    TIMER;


    public static Type get(@NotNull String arg) {
        for (Type t : Type.values())
            if (t.name().equals(arg.toUpperCase()))
                return t;
        return null;
    }

    public boolean isAction() {
        return switch (this) {
            case DESTROY_BLOCK,
                 ENTITY_MOD,
                 REPLACE_BLOCK,
                 DOOR,
                 ENTITY_SPAWN_ACTION,
                 MODIFY_BLOCK -> true;
            default -> false;
        };
    }

    public boolean isCondition() {
        return switch (this) {
            case DUNGEON_START,
                 WITHIN_DISTANCE,
                 WITHIN_BOUNDS,
                 NOT,
                 ENTITY_DEATH_EVENT,
                 ENTITY_SPAWN_EVENT -> true;
            default -> false;
        };
    }
}
