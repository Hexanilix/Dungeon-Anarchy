package org.hexils.dnarch.items.actions;

import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.List;

public class DestroyBlock extends ReplaceBlock {
    public DestroyBlock(List<Block> blocks) {
        super(blocks, Material.AIR);
    }
}
