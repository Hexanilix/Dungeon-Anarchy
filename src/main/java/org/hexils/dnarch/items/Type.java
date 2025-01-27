package org.hexils.dnarch.items;

import org.hexils.dnarch.*;
import org.hexils.dnarch.items.actions.*;
import org.hexils.dnarch.items.actions.entity.EntitySpawnAction;
import org.hexils.dnarch.items.actions.entity.ModifyEntity;
import org.hexils.dnarch.items.conditions.NOTCondition;
import org.hexils.dnarch.items.conditions.WithinBoundsCondition;
import org.hexils.dnarch.items.conditions.WithinDistance;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.hetils.jgl17.StringUtil.readableEnum;


public enum Type {
    //Actions
    TIMER(TimerAction.class),
    DESTROY_BLOCK(ReplaceBlock.DestroyBlock.class),
    ENTITY_MOD(ModifyEntity.class),
    REPLACE_BLOCK(ReplaceBlock.class),
    DOOR(Door.class),
    ENTITY_SPAWN_ACTION(EntitySpawnAction.class),
    MODIFY_BLOCK(ModifyBlock.class),
    SPAWN_PARTICLE(SpawnParticle.class),

    //Conditions
    NOT(NOTCondition.class),
    DUNGEON_START(Condition.class),
    WITHIN_DISTANCE((WithinDistance.class)),
    WITHIN_BOUNDS(WithinBoundsCondition.class),
    ENTITY_DEATH_EVENT(EntitySpawnAction.EntityDeathCondition.class),
    ENTITY_SPAWN_EVENT(EntitySpawnAction.EntitySpawnCondition.class),

    //Other
    TRIGGER(Trigger.class),
    RESET(ResetAction.class),
    ENTITY_SPAWN(EntitySpawn.class),
    NUMBER_HOLDER(NumberHolder.class);

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface NotCreatable { }

    private final Class<? extends DAItem> da_class;
    Type(Class<? extends DAItem> da_class) { this.da_class = da_class; }

    public static @Nullable Type get(@NotNull String arg) {
        for (Type t : Type.values())
            if (t.name().equals(arg.toUpperCase()))
                return t;
        return null;
    }

    public boolean isCreatable() {
        return da_class.isAnnotationPresent(NotCreatable.class) || switch (this) {
            case DUNGEON_START,
                 ENTITY_DEATH_EVENT,
                 ENTITY_SPAWN_EVENT,
                 ENTITY_SPAWN -> false;
            default -> true;
        };
    }

    public boolean isAction() { return Action.class.isAssignableFrom(da_class); }

    public boolean isCondition() { return Condition.class.isAssignableFrom(da_class); }

    public @Nullable DAItem create(DungeonMaster dm, String[] args) {
        if (isCreatable()) {
            if (args == null) args = new String[0];
            try {
                return da_class.getDeclaredConstructor().newInstance().create(dm, args);
            } catch (Exception e) {}
        }
        return null;
    }

    @Contract(pure = true)
    public @NotNull Class<? extends DAItem> getDAClass() { return da_class; }

    public @NotNull String readableName() { return readableEnum(this); }
}
