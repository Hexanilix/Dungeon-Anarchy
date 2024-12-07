package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.objects.actions.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

import static org.hetils.mpdl.General.log;

public abstract class Action extends DA_item {

    public final static Collection<Action> actions = new HashSet<>();

    public static String toRedableFormat(String in) {
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

    public Action(Type type) {
        this.type = type;
        this.name = toRedableFormat(type.name());
        log(toRedableFormat(type.name()));
        log(this.name);
        actions.add(this);
    }

    public static Action create(Type type, DungeonMaster dm, @NotNull String[] args) {
        Action a = null;
        switch (type) {
            case REPLACE_BLOCK -> {
                Material mat = args.length >= 1 ? Material.getMaterial(args[0].toUpperCase()) : null;
                if (mat != null) {
                    a = new ReplaceBlock(dm.getSelectedBlocks(), mat);
                    dm.deselectBlocks();
                } else dm.p.sendMessage(ChatColor.RED + "Please select a valid material!");
            }
            case DESTROY_BLOCK -> {
                log(dm.getSelectedBlocks());
                a = new DestroyBlock(dm.getSelectedBlocks());
                dm.deselectBlocks();
            }
            case SPAWN -> {
                EntityType et = args.length >= 1 ? getEnum(EntityType.class, args[0]) : null;
                if (et != null) {
                    a = new Spawn(new EntitySpawn(et), org.hetils.mpdl.Block.toLocations(dm.getSelectedBlocks()));
                    dm.deselectBlocks();
                } else dm.p.sendMessage(ChatColor.RED + "Please select a valid entity type");
            }
            case MODIFY_BLOCK -> {
                ModifyBlock.ModType mt = args.length >= 1 ? ModifyBlock.ModType.get(args[0]) : null;
                if (mt != null) {
                    a = new ModifyBlock(dm.getSelectedBlocks(), mt);
                    dm.deselectBlocks();
                } else dm.p.sendMessage(ChatColor.RED + "Please select a valid mod type!");
            }
            default -> {

            }
        }
        return a;
    }

    public static <E extends Enum<E>> E getEnum(Class<E> enumList, String name) {
        if (name == null || enumList == null) return null;
        for (E e : enumList.getEnumConstants())
            if (e.name().equalsIgnoreCase(name))
                return e;
        return null;
    }

    public abstract void execute();
    protected abstract void resetAction();

    public final void reset() {
        triggered = false;
        resetAction();
    }

    public final boolean isTriggered() {
        return triggered;
    }
}
