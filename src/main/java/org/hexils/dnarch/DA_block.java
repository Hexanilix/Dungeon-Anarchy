package org.hexils.dnarch;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.hetils.mpdl.item.NSK;
import org.hexils.dnarch.items.Type;

public abstract class DA_block extends DAItem {
    public static final NSK BLOCK = new NSK(new NamespacedKey(Main.plugin(), "block"), PersistentDataType.STRING);
    public Block block;

    public DA_block(Type type) { super(type); }

    public Block getBlock() { return block; }

    public void setBlock(Block block) { this.block = block; }
}
