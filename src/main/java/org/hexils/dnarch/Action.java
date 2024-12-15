package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.hexils.dnarch.dungeon.Dungeon;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.items.EntitySpawn;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.actions.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.hexils.dnarch.Main.log;
import static org.hexils.dnarch.commands.DungeonCommandExecutor.ER;


public abstract class Action extends DA_item implements Triggerable {

    public final static Collection<Action> actions = new HashSet<>();

    public static String toReadableFormat(String in) {
        if (in == null) return null;
        String[] spl = in.toLowerCase().replace(" ", "_").split("_");
        StringBuilder s = new StringBuilder().append((char) (spl[0].charAt(0) - 32)).append(spl[0], 1, spl[0].length());
        for (int i = 1; i < spl.length; i++) {
            String st = spl[i];
            s.append(' ').append(((char) (st.charAt(0) - 32))).append(st, 1, st.length());
        }
        return s.toString();
    }

    protected boolean triggered;
    public final Type type;

    public Action(@NotNull Type type) {
        super(toReadableFormat(type.name()));
        this.type = type;
        actions.add(this);
    }

    public static <E extends Enum<E>> E getEnum(Class<E> enumList, String name) {
        if (name == null || enumList == null) return null;
        for (E e : enumList.getEnumConstants())
            if (e.name().equalsIgnoreCase(name))
                return e;
        return null;
    }

    protected abstract void resetAction();

    public final void reset() {
        triggered = false;
        resetAction();
    }

    public final boolean isTriggered() {
        return triggered;
    }
}
