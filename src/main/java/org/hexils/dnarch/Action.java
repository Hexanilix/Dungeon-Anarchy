package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.hetils.mpdl.BlockUtil;
import org.hexils.dnarch.dungeon.Dungeon;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.objects.actions.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public abstract class Action extends DA_item implements Triggerable {

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
        super(toRedableFormat(type.name()));
        this.type = type;
        actions.add(this);
    }

    public static Action commandCreate(Type type, DungeonMaster dm, @NotNull String[] args) {
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
                a = new DestroyBlock(dm.getSelectedBlocks());
                dm.deselectBlocks();
            }
            case SPAWN -> {
                EntityType et = args.length >= 1 ? getEnum(EntityType.class, args[0]) : null;
                if (et != null) {
                    a = new Spawn(new EntitySpawn(et), org.hetils.mpdl.BlockUtil.toLocations(dm.getSelectedBlocks()));
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
        if (a == null) return null;
        Dungeon.Section s;
        Dungeon d = dm.getCurrentDungeon();
        if (a instanceof BlockAction b) {
            List<Dungeon.Section> list = new ArrayList<>();
            for (Block bl : b.getAffectedBlocks())
                list.add(d.getSection(bl));
            Map<Dungeon.Section, Integer> m = new HashMap<>();
            list.forEach(sc -> m.putIfAbsent(sc, 0));
            list.forEach(sc -> m.replace(sc, m.get(sc)+1));
            Map.Entry<Dungeon.Section, Integer> e = new Map.Entry<>() {
                @Override
                public Dungeon.Section getKey() {
                    return list.get(0);
                }

                @Override
                public Integer getValue() {
                    return 0;
                }

                @Override
                public Integer setValue(Integer value) {
                    return 0;
                }
            };
            for (Map.Entry<Dungeon.Section, Integer> en : m.entrySet())
                if (en.getValue() > e.getValue())
                    e = en;
            s = e.getKey();
        } else s = d.getSection(dm.p);
        s.addItem(a);
        return a;
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
