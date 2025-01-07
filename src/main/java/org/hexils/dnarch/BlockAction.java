package org.hexils.dnarch;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.actions.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public abstract class BlockAction extends Action {
    protected final List<Block> affected_blocks = new ArrayList<>();
    @OODPExclude
    protected final List<BlockData> original_block_data = new ArrayList<>();

    public BlockAction(Type type) { super(type); }
    public BlockAction(Type type, List<Block> b) {
        super(type);
        this.affected_blocks.addAll(b);
        this.original_block_data.addAll(b.stream().map(Block::getBlockData).toList());
    }

    public void updateBlockData() { original_block_data.clear(); original_block_data.addAll(affected_blocks.stream().map(Block::getBlockData).toList()); }

    public final @NotNull List<Block> getAffectedBlocks() { return affected_blocks; }
}
