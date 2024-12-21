package org.hexils.dnarch.items.actions;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.hetils.mpdl.MaterialUtil;
import org.hexils.dnarch.BlockAction;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.hetils.mpdl.ItemUtil.newItemStack;

public class ReplaceBlock extends BlockAction {
    public static class DestroyBlock extends ReplaceBlock { public DestroyBlock(List<Block> blocks) { super(blocks, Material.AIR); } }

    private boolean sound = true;
    private boolean particles = true;
    private List<BlockData> ogbd = new ArrayList<>();
    private List<Block> blocks = new ArrayList<>();
    private Material change_material;
    private World world;

    @Override
    protected void createGUI() {
        this.setSize(54);
        this.setItem(22, newItemStack(Material.OAK_SIGN, "Affected blocks:"));
    }

    public ReplaceBlock(List<Block> blocks, Material change_material) {
        super(Type.REPLACE_BLOCK, blocks);
        this.change_material = change_material;
        if (blocks != null && !blocks.isEmpty()) {
            world = blocks.get(0).getWorld();
            for (Block b : blocks)
                if (b.getWorld() == world) {
                    this.blocks.add(b);
                    this.ogbd.add(b.getBlockData());
                }
        }
    }


    @Override
    public void trigger() {
        if (!triggered) {
            for (Block b : blocks) {
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
        for (int i = 0; i < blocks.size(); i++)
            blocks.get(i).setBlockData(ogbd.get(i));
    }

    @Override
    protected void updateGUI() {
        ItemStack cm = newItemStack(MaterialUtil.validMetaSubstitution(change_material), "Change Material: " + change_material.name());
        setField(cm, "material", change_material.name());
        this.setItem(13, cm);
        for (int i = 0; i < 27; i++)
            this.setItem(i+27, i < ogbd.size() ? org.hetils.mpdl.BlockUtil.b2i(blocks.get(i), ogbd.get(i)) : null);
    }

    @Override
    protected ItemStack genItemStack() {
        return new ItemStack(Material.DIAMOND_PICKAXE);
    }

    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {
        switch (field) {
            case "material" -> {
                Material m = Material.getMaterial(value);
                if (m != null)
                    this.change_material = m;
            }
            case "blocks" -> {
                if (dm.getSelectedBlocks().isEmpty())
                    dm.sendMessage( ChatColor.RED + "You have no blocks currently selected!");
                else {
                    this.blocks = dm.getSelectedBlocks();
                    dm.deselectBlocks();
                }
            }
        }
    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {

    }
}
