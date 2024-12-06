package org.hexils.dnarch.objects.actions;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hetils.mpdl.Item.newItemStack;

public class ModifyBlock extends Action {
    private List<Block> modify;
    private List<BlockData> og_data;
    private BlockModification.ModType type;
    public ModifyBlock(List<Block> blocks, BlockModification.ModType type) {
        super(Type.MODIFY_BLOCK);
        this.modify = blocks;
        this.og_data = blocks.stream().map(Block::getBlockData).toList();
        this.type = type;
    }

    @Override
    public void execute() {
        modify.forEach(b -> BlockModification.mod.get(type).modify(b));
    }

    @Override
    protected void resetAction() {
        for (int i = 0; i < modify.size(); i++) {
            modify.get(i).setBlockData(og_data.get(i));
        }
    }

    @Override
    protected void createGUIInventory() {

    }

    @Override
    protected ItemStack toItem() {
        ItemStack i = newItemStack(Material.GOLDEN_SHOVEL, name);
        return i;
    }

    @Override
    public void updateGUI() {

    }


    private static final class BlockModification {
        public interface Modify { void modify(Block b); }
        public enum ModType {
            OPEN,
        }
        public static final Map<ModType, Modify> mod = new HashMap<>();
        static {
            mod.put(ModType.OPEN, b -> {
                if (b != null) {
                    BlockData bd = b.getBlockData();
                    if (bd instanceof Openable op) {
                        op.setOpen(true);
                        b.setBlockData(op);
                    }
                }
            });
        }
    }
}
