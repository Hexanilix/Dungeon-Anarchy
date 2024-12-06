package org.hexils.dnarch.objects.actions;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.hexils.dnarch.Action;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.GUI;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.hetils.mpdl.Inventory.newInv;
import static org.hetils.mpdl.Item.newItemStack;

public class ReplaceBlock extends Action {
    private boolean sound = true;
    private boolean particles = true;
    private List<BlockData> ogbd = new ArrayList<>();
    private List<Block> blocks = new ArrayList<>();
    private Material change_material;
    private World world;

    @Override
    protected void createGUIInventory() {
        this.gui = newInv(54, name);
        ItemStack cm = newItemStack(hasMeta(change_material), "Change Material: " + change_material.name());
        GUI.setField(cm, "material", change_material.name());
        this.gui.setItem(13, cm);
        this.gui.setItem(22, newItemStack(Material.OAK_SIGN, "Affected blocks:"));
    }

    @Contract(pure = true)
    private static @Nullable Material hasMeta(Material mat) {
        if (mat == null) return null;
        return switch (mat) {
            case AIR -> Material.FEATHER;
            case WATER -> Material.WATER_BUCKET;
            case LAVA -> Material.LAVA_BUCKET;
            case WATER_CAULDRON, LAVA_CAULDRON -> Material.CAULDRON;
            default -> mat;
        };
    }

    public ReplaceBlock(String name, List<Block> blocks, Material change_material) {
        super(Type.REPLACE_BLOCK);
        this.name = name;
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

    public ReplaceBlock(List<Block> blocks, Material change_material) {
        this("Replace Block Action", blocks, change_material);
    }


    @Override
    public void execute() {
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
    public void updateGUI() {
        for (int i = 0; i < 27; i++)
            this.gui.setItem(i+27, i < ogbd.size() ? org.hetils.mpdl.Block.b2i(blocks.get(i), ogbd.get(i)) : null);
    }

    @Override
    protected ItemStack toItem() {
        ItemStack i = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta m = i.getItemMeta();
        assert m != null;
        m.setDisplayName(name);
        m.getPersistentDataContainer().set(GUI.MODIFIABLE.key(), PersistentDataType.BOOLEAN, true);
        i.setItemMeta(m);
        return i;
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
                    dm.p.sendMessage( ChatColor.RED + "You have no blocks currently selected!");
                else {
                    this.blocks = dm.getSelectedBlocks();
                    dm.deselectBlocks();
                }
            }
        }
    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args) {

    }
}
