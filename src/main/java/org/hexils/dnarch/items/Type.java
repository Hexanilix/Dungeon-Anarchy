package org.hexils.dnarch.items;

import org.hexils.dnarch.*;
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
    TIMER(TimerAction.class),
    DESTROY_BLOCK(ReplaceBlock.DestroyBlock.class),
    ENTITY_MOD(ModifyEntity.class),
    REPLACE_BLOCK(ReplaceBlock.class),
    DOOR(Door.class),
    ENTITY_SPAWN_ACTION(EntitySpawnAction.class),
    MODIFY_BLOCK(ModifyBlock.class),

    //Conditions
    NOT(NOTCondition.class),
    DUNGEON_START(Condition.class),
    WITHIN_DISTANCE((WithinDistance.class)),
    WITHIN_BOUNDS(WithinBoundsCondition.class),
    ENTITY_DEATH_EVENT(EntitySpawnAction.EntityDeathCondition.class),
    ENTITY_SPAWN_EVENT(EntitySpawnAction.class),

    //Other
    TRIGGER(Trigger.class),
    RESET_ACTION(ResetAction.class),
    ENTITY_SPAWN(EntitySpawn.class),
    MULTIPLIER(Multiplier.class);

    public static @Nullable Type get(@NotNull String arg) {
        for (Type t : Type.values())
            if (t.name().equals(arg.toUpperCase()))
                return t;
        return null;
    }

    public boolean isCreatable() {
        return switch (this) {
            case DUNGEON_START,
                 ENTITY_DEATH_EVENT,
                 ENTITY_SPAWN_ACTION -> false;
            default -> true;
        };
    }

    public boolean isAction() { return Action.class.isAssignableFrom(da_class); }

    public boolean isCondition() { return Condition.class.isAssignableFrom(da_class); }

    private final Class<? extends DAItem> da_class;

    Type(Class<? extends DAItem> da_class) { this.da_class = da_class; }

    public DAItem create(DungeonMaster dm, String[] args) {
        if (args == null) args = new String[0];
        try {
            return da_class.getDeclaredConstructor().newInstance().create(dm, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DAItem for " + name(), e);
        }
    }

    @Contract(pure = true)
    public @NotNull Class<? extends DAItem> getDAClass() { return da_class; }

    public @NotNull String readableName() { return toReadableFormat(this.name()); }
}
