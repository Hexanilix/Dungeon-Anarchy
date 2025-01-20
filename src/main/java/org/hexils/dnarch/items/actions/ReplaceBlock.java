package org.hexils.dnarch.items.actions;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.MaterialUtil;
import org.hexils.dnarch.BlockAction;
import org.hexils.dnarch.DAItem;
import org.hexils.dnarch.DungeonMaster;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;

public class ReplaceBlock extends BlockAction {
    @Override
    public DAItem create(DungeonMaster dm, String[] args) {
        Material mat = args.length >= 1 ? Material.getMaterial(args[0].toUpperCase()) : null;
        if (mat != null) {
            this.change_material = mat;
            setBlocks(dm.getSelectedBlocks());
            return this;
        } else {
            dm.sendMessage(ChatColor.RED + "Please select a valid material!");
            return null;
        }
    }

    public static class DestroyBlock extends ReplaceBlock {
        public DestroyBlock() { super(null, Material.AIR); }
        public DestroyBlock(List<Block> blocks) { super(blocks, Material.AIR); }
        @Override
        public DAItem create(@NotNull DungeonMaster dm, String[] args) {
            setBlocks(dm.getSelectedBlocks());
            return this;
        }
    }

    private boolean sound = true;
    private boolean particles = true;
    private Material change_material;
    @OODPExclude private World world;

    @Override
    protected void createGUI() {
        this.setSize(54);
        this.setItem(22, newItemStack(Material.OAK_SIGN, "Affected blocks:"));
    }

    public ReplaceBlock() { super(Type.REPLACE_BLOCK); }
    public ReplaceBlock(List<Block> blocks, Material mat) {
        super(Type.REPLACE_BLOCK);
        this.change_material = mat;
        setBlocks(blocks);
    }

    public void setBlocks(List<Block> blocks) {
        affected_blocks.clear();
        if (blocks != null && !blocks.isEmpty()) {
            world = blocks.get(0).getWorld();
            for (Block b : blocks)
                if (b.getWorld() == world) {
                    this.affected_blocks.add(b);
                    this.original_block_data.add(b.getBlockData());
                }
        }
    }

    @Override
    public void onTrigger() {
        if (!triggered && !affected_blocks.isEmpty()) {
            if (world == null) world = affected_blocks.get(0).getWorld();
            if (original_block_data.isEmpty()) setBlocks(affected_blocks.stream().toList());
            for (Block b : affected_blocks) {
                if (sound)
                    world.playSound(b.getLocation(), Sound.BLOCK_STONE_BREAK, .5f, .5f);
                if (particles)
                    world.spawnParticle(Particle.BLOCK_CRACK, b.getLocation().add(.5, .5, .5), 25, 1, 1, 1, 1, b.getBlockData());
                b.setType(change_material);
            }
            triggered = true;
        }
    }

    @Override
    protected void resetAction() {
        for (int i = 0; i < original_block_data.size(); i++)
            affected_blocks.get(i).setBlockData(original_block_data.get(i));
    }

    @Override
    protected void updateGUI() {
        ItemStack cm = newItemStack(MaterialUtil.validMetaSubstitution(change_material), "Change Material: " + change_material.name());
        changeField(cm, "material", change_material.name());
        this.setItem(13, cm);
        for (int i = 0; i < 27; i++)
            this.setItem(i + 27, i < original_block_data.size() ? org.hetils.mpdl.BlockUtil.b2i(affected_blocks.get(i), original_block_data.get(i)) : null);
    }

    @Override
    protected ItemStack genItemStack() {
        return new ItemStack(Material.DIAMOND_PICKAXE);
    }

    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, ClickType click) {
//        switch (field) {
//            case "material" -> {
//                Material m = Material.getMaterial(value);
//                if (m != null)
//                    this.change_material = m;
//            }
//        }
    }
}
