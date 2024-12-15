package org.hexils.dnarch;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.conditions.WithinDistance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static org.hexils.dnarch.Action.toReadableFormat;

public abstract class Condition extends DA_item implements Booled, Triggerable {

    public final Map<DA_item, Runnable> runnables = new HashMap<>();

    protected Type type;

    public final Type getType() {
        return type;
    }

    public Condition(Type type) { this(type, true); }
    public Condition(Type type, boolean renamealbe) {
        super(type, renamealbe);
        this.type = type;
    }

    public final void trigger() {
        onTrigger();
        runnables.values().forEach(Runnable::run);
    }

    protected abstract void onTrigger();
}
