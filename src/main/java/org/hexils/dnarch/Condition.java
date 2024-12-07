package org.hexils.dnarch;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.objects.conditions.Distance;
import org.hexils.dnarch.objects.conditions.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Condition extends DA_item implements Booled, Triggerable {

    public final Map<DA_item, Runnable> runnables = new HashMap<>();

    protected Type type;

    public static Condition create(@Nullable Type type, DungeonMaster dm, @NotNull String[] args) {
        Condition c = null;
        if (type != null) switch (type) {
            case DISTANCE -> {
                Location l = null;
                if (dm.hasBlocksSelected())
                    l = org.hetils.mpdl.Location.getCenter(dm.getSelectedBlocks().stream().map(Block::getLocation).toList());
                if (l == null) l = dm.p.getLocation();
                c = new Distance(l);
            }
            case LOCATION -> {

            }
        }
        return c;
    }

    public final Type getType() {
        return type;
    }

    public Condition(Type type) {
        this.type = type;
    }

    public final void trigger() {
        onTrigger();
        runnables.values().forEach(Runnable::run);
    }

    protected abstract void onTrigger();
}
