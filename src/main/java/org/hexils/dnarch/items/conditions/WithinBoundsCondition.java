package org.hexils.dnarch.items.conditions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.location.BoundingBox;
import org.hetils.mpdl.location.LocationUtil;
import org.hetils.mpdl.VectorUtil;
import org.hexils.dnarch.*;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;


import static org.hetils.mpdl.item.ItemUtil.newItemStack;

public class WithinBoundsCondition extends Condition implements RunnableDA {
    private BoundingBox bounds;
    @OODPExclude
    private BukkitRunnable runnable;
    @OODPExclude
    private boolean satisfied = false;

    public WithinBoundsCondition() { this(null); }
    public WithinBoundsCondition(BoundingBox bounds) {
        super(Type.WITHIN_BOUNDS);
        this.bounds = bounds == null ? null : bounds.clone();
        if (bounds != null) start();
    }

    private boolean check(@NotNull Collection<Entity> c) {
        for (Entity e : c)
            if (this.bounds.contains(e.getLocation()))
                return true;
        return false;
    }

    @Override
    public boolean isSatisfied() { return satisfied; }

    @Override
    protected ItemStack genItemStack() {
        return newItemStack(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, ChatColor.GOLD + "Area Condition", List.of(ChatColor.GRAY + "This condition triggers whenever a player enters"));
    }


    @Override
    public DAItem create(@NotNull DungeonMaster dm, String[] args) {
        if (dm.hasAreaSelected()) this.bounds = new BoundingBox(dm.getSelectedArea());
        dm.clearSelection();
        return this;
    }

    @Override
    public void start() {
        runnable = new BukkitRunnable() {
            final World w = WithinBoundsCondition.this.bounds.getWorld();
            final @NotNull Location loc = LocationUtil.join(w, bounds.getCenter());
            final double rad = bounds.getMax().distance(VectorUtil.from(loc));

            @Override
            public void run() {
                Collection<Entity> c = w.getNearbyEntities(loc, rad, rad, rad).stream().filter(e -> e instanceof Player).toList();
                if (check(c)) {
                    boolean h = satisfied;
                    satisfied = true;
                    if (!h) WithinBoundsCondition.this.onTrigger();
                } else satisfied = false;
            }
        };
        runnable.runTaskTimer(Main.plugin(), 0, 2);
    }

    @Override
    public void stop() {
        runnable.cancel();
    }

    @Override
    public void onTrigger() {}
}
