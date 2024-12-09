package org.hexils.dnarch.objects;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.Action;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.objects.actions.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Door extends Action {
    public enum Facing {
        N,
        S,
        E,
        W,
        NE,
        SE,
        NW,
        SW
    }

    private List<Block> blocks;
    private boolean isopen = false;
    private Facing facing;
    private boolean singleuse;
    private float angle = 90;
    private Location pivotp;
    private Location[][][] matrix;

    //TODO: Convert block list to matrix
    public Door(List<Block> blocks, Facing face) {
        super(Type.DOOR);
        this.blocks = blocks;
        this.name = "Door";
        this.facing = face;
    }

    @Override
    protected void createGUIInventory() {
    }

    @Override
    public void updateGUI() {

    }

    @Override
    protected ItemStack toItem() {
        return null;
    }

    @Override
    protected void changeField(DungeonMaster dm, String field, String value) {

    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args) {

    }

    private BukkitRunnable openr = null;
    private Collection<Block> mdblocks = new ArrayList<>();
    @Override
    public void trigger() {
        if (isopen) return;
        openr = new BukkitRunnable() {
            private float ca = 0;
            private float speed = 0.1f;
            private Location[] ll = new Location[blocks.size()];
            @Override
            public void run() {
                if (this.ca >= angle)
                    cancel();
                else {
                    pivotp.setYaw(pivotp.getYaw()+speed);
                    for (int i = 0; i < 3; i++) {
                        Location l = pivotp.clone().add(pivotp.getDirection().normalize().multiply(i));
                        if (ll[i] != l) {
                            ll[i] = l;

                        }
                    }
                }
            }
        };
        openr.runTaskTimer(Main.plugin, 0, 0);
    }

    public void halt() {
    }

    @Override
    protected void resetAction() {

    }
}
