package org.hexils.dnarch.items.conditions;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.GeneralListener;
import org.hetils.mpdl.location.LocationUtil;
import org.hexils.dnarch.*;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Random;


import static org.hetils.mpdl.item.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;

public class WithinDistance extends Condition implements RunnableDA {

    @Override
    public boolean isSatisfied() {
        return this.satisfied;
    }

    public enum TriggerShape {
        SQUARE,
        SPHERE,
        CIRCLE
    }

    private Location center;
    private TriggerShape shape;
    private double rad;
    @OODPExclude
    private boolean satisfied = false;
    @OODPExclude
    private BukkitRunnable runnable;
    @OODPExclude
    private boolean relocating = false;

    @Override
    public DAItem create(@NotNull DungeonMaster dm, String[] args) {
        Location l = null;
        if (dm.hasBlocksSelected())
            l = org.hetils.mpdl.location.LocationUtil.getCenter(dm.getSelectedBlocks().stream().map(Block::getLocation).toList());
        if (l == null) l = dm.getLocation();
        return new WithinDistance(l);
    }

    public WithinDistance() { super(Type.WITHIN_DISTANCE); }
    public WithinDistance(@NotNull Location center) {
        super(Type.WITHIN_DISTANCE);
        this.center = center;
        this.rad = 1;
        this.shape = TriggerShape.SPHERE;
        start();
    }

    private void assignRunnable() {
        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (check(center.getWorld().getNearbyEntities(center, rad, rad, rad))) {
                    if (!satisfied) {
                        satisfied = true;
                        trigger();
                    }
                } else {
                    if (satisfied) {
                        satisfied = false;
                        trigger();
                    }
                }
            }
        };
    }

    @Override
    public void start() {
        assignRunnable();
        runnable.runTaskTimer(Main.plugin(), 0, 2);
    }

    public boolean check(Collection<Entity> c) {
        if (c == null) return false;
        for (Entity e : c)
            if (e instanceof Player p && p.getGameMode() != GameMode.SPECTATOR)
                return true;
        return false;
    }

    @Override
    public void stop() {
        runnable.cancel();
    }

    public void setCenter(Location center) {
        this.center = center;
    }

    public Location getCenter() {
        return center;
    }

    public void setRad(double rad) {
        this.rad = rad;
    }

    public double getRad() { return rad; }

    public void setShape(TriggerShape shape) { this.shape = shape; }

    public TriggerShape getShape() { return shape; }

    @Override
    protected void createGUI() {
        this.setAction(21, newItemStack(Material.ARROW, "Location", List.of(LocationUtil.toReadableFormat(this.center))), "change_center");
    }

    @Override
    protected void action(DungeonMaster dm, @NotNull String action, String[] args, ClickType click) {
        switch (action) {
            case "change_center" -> {
                dm.holdManagement(true);
                runnable.cancel();
                GeneralListener.selectLocation(dm, "Select a new location", l -> {
                    spawnParticlesInSphere(l, rad, (int) Math.pow(4, rad), Particle.COMPOSTER);
                } ,l -> {
                    center = l;
                    start();
                    dm.holdManagement(false);
                });
            }
        }
    }

    public void spawnParticlesInSphere(@NotNull Location center, double radius, int particleCount, Particle particleType) {
        World world = center.getWorld();
        if (world == null) return;

        Random random = new Random();
        for (int i = 0; i < particleCount; i++) {
            double phi = random.nextDouble() * 2 * Math.PI;
            double theta = random.nextDouble() * Math.PI;

            double x = radius * Math.sin(theta) * Math.cos(phi);
            double y = radius * Math.cos(theta);
            double z = radius * Math.sin(theta) * Math.sin(phi);

            Location particleLocation = center.clone().add(x, y, z);

            world.spawnParticle(particleType, particleLocation, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public ItemStack genItemStack() {
        ItemStack i = newItemStack(Material.SCULK_SENSOR, ChatColor.RESET + getName());
        ItemMeta m = i.getItemMeta();
        m.setLore(List.of(ChatColor.LIGHT_PURPLE + "Type: " + this.getType()));
        i.setItemMeta(m);
        return i;
    }
}
