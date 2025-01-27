package org.hexils.dnarch;

import org.bukkit.inventory.ItemStack;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.item.NSK;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public abstract class Action extends DAItem implements Triggerable, Resetable {

    public final static Set<Action> actions = new HashSet<>();
    public static @Nullable Action get(String id) { return get(UUID.fromString(id)); }
    public static @Nullable Action get(ItemStack it) {
        String s = NSK.getNSK(it, ITEM_UUID);
        return s == null ? null : get(UUID.fromString(s));
    }
    public static @Nullable Action get(UUID id) {
        for (Action d : actions)
            if (d.getId().equals(id))
                return d;
        return null;
    }

    @OODPExclude
    protected boolean triggered;

    public Action(@NotNull Type type) {
        super(type);
        actions.add(this);
    }

    public static <E extends Enum<E>> E getEnum(Class<E> eclazz, String name) {
        if (name == null || eclazz == null) return null;
        for (E e : eclazz.getEnumConstants())
            if (e.name().equalsIgnoreCase(name))
                return e;
        return null;
    }

    protected abstract void resetAction();

    @Override
    public final void reset() {
        triggered = false;
        resetAction();
    }

    public final boolean isTriggered() { return triggered; }
}
