package org.hexils.dnarch.items.actions;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.BlockAction;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static org.hetils.mpdl.ItemUtil.newItemStack;

public class ModifyBlock extends BlockAction {
    public interface Modify { void modify(Block b); }
    public enum ModType {
        OPEN, CLOSE;

        public static @Nullable ModType get(@NotNull String arg) {
            for (ModType t : ModType.values())
                if (t.name().equalsIgnoreCase(arg))
                    return t;
            return null;
        }
    }
    public static final Map<ModType, Modify> mod = new HashMap<>();

    private List<Block> modify;
    private List<BlockData> og_data;
    private ModType type;
    public ModifyBlock(@NotNull List<Block> blocks, ModType type) {
        super(Type.MODIFY_BLOCK, blocks);
        this.modify = blocks;
        this.og_data = blocks.stream().map(Block::getBlockData).toList();
        this.type = type;
    }

    @Override
    public void trigger() {
        modify.forEach(b -> mod.get(type).modify(b));
    }

    @Override
    protected void resetAction() {
        for (int i = 0; i < modify.size(); i++) {
            modify.get(i).setBlockData(og_data.get(i));
        }
    }

    @Override
    protected void createGUI() {
        this.setSize(54);
        updateGUI();
    }

    @Override
    protected ItemStack genItemStack() {
        ItemStack i = newItemStack(Material.GOLDEN_SHOVEL, getName());
        return i;
    }

    @Override
    protected void updateGUI() {
        for (int i = 0; i < 27; i++)
            this.setItem(i+27, i < og_data.size() ? org.hetils.mpdl.BlockUtil.b2i(modify.get(i), og_data.get(i)) : null);
    }


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
        mod.put(ModType.CLOSE, b -> {
            if (b != null) {
                BlockData bd = b.getBlockData();
                if (bd instanceof Openable op) {
                    op.setOpen(false);
                    b.setBlockData(op);
                }
            }
        });
    }
}
