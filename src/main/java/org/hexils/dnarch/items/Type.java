package org.hexils.dnarch.items;

import org.hexils.dnarch.Dungeon;
import org.hexils.dnarch.items.actions.ModifyBlock;
import org.hexils.dnarch.items.actions.ReplaceBlock;
import org.hexils.dnarch.items.actions.ResetAction;
import org.hexils.dnarch.items.actions.TimerAction;
import org.hexils.dnarch.items.actions.entity.EntitySpawnAction;
import org.hexils.dnarch.items.actions.entity.ModifyEntity;
import org.hexils.dnarch.items.conditions.NOTCondition;
import org.hexils.dnarch.items.conditions.WithinBoundsCondition;
import org.hexils.dnarch.items.conditions.WithinDistance;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.hexils.dnarch.Action.toReadableFormat;

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
    TIMER, TRIGGER, RESET_ACTION, ENTITY_SPAWN,  MULTIPLIER;

    public static @Nullable Type get(@NotNull String arg) {
        for (Type t : Type.values())
            if (t.name().equals(arg.toUpperCase()))
                return t;
        return null;
    }

    public String getName() { return toReadableFormat(this.name()); }

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

    @Contract(pure = true)
    public @NotNull Class<?> getDAClass() {
        return switch (this) {
            case DESTROY_BLOCK -> ReplaceBlock.DestroyBlock.class;
            case ENTITY_MOD -> ModifyEntity.class;
            case REPLACE_BLOCK -> ReplaceBlock.class;
            case DOOR -> Door.class;
            case ENTITY_SPAWN_ACTION -> EntitySpawnAction.class;
            case MODIFY_BLOCK -> ModifyBlock.class;
            case DUNGEON_START -> Dungeon.class;
            case WITHIN_DISTANCE -> WithinDistance.class;
            case WITHIN_BOUNDS -> WithinBoundsCondition.class;
            case NOT -> NOTCondition.class;
            case ENTITY_DEATH_EVENT -> EntitySpawnAction.EntityDeathCondition.class;
            case ENTITY_SPAWN_EVENT -> EntitySpawnAction.EntitySpawnCondition.class;
            case BLOCK_CHANGE_EVENT -> null;
            case TIMER -> TimerAction.class;
            case TRIGGER -> Trigger.class;
            case RESET_ACTION -> ResetAction.class;
            case MULTIPLIER -> Multiplier.class;
            case ENTITY_SPAWN -> EntitySpawn.class;
        };
    }
}
