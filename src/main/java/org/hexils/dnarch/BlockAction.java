package org.hexils.dnarch;

import org.bukkit.block.Block;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.actions.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public abstract class BlockAction extends Action {
    protected final List<Block> affected_blocks = new ArrayList<>();

    public BlockAction(Type type) { super(type); }
    public BlockAction(Type type, List<Block> b) {
        super(type);
        this.affected_blocks.addAll(b);
    }

    public final @NotNull List<Block> getAffectedBlocks() { return affected_blocks; }
}
