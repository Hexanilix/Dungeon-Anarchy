package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.objects.actions.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


public abstract class BlockAction extends Action {
    protected final List<Block> affected_blocks = new ArrayList<>();

    public BlockAction(Type type) {
        super(type);
    }

    public BlockAction(Type type, List<Block> b) {
        super(type);
        this.affected_blocks.addAll(b);
    }

    public final List<Block> getAffectedBlocks() { return affected_blocks; }
}
