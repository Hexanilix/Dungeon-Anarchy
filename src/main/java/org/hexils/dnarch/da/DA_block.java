package org.hexils.dnarch.da;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.hexils.dnarch.NSK;

public abstract class DA_block extends DA_item {
    public static final NSK BLOCK = new NSK(new NamespacedKey("dungeon_anarchy", "block"), PersistentDataType.STRING);
    public Block block;

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }
}
