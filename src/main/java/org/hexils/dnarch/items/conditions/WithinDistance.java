package org.hexils.dnarch.items.conditions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.Condition;
import org.hexils.dnarch.RunnableDA;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;


import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;

public class WithinDistance extends Condition implements RunnableDA {
    public static boolean hasPlayers(Collection<Entity> c) {
        if (c == null) return false;
        for (Entity e : c)
            if (e instanceof Player)
                return true;
        return false;
    }

    @Override
    public void onTrigger() { this.satisfied = true; }

    @Override
    public boolean isSatisfied() {
        return this.satisfied;
    }

    public enum TriggerShape {
        SQUARE,
        SPHERE,
        CIRCLE
    }

    private Location loc;
    private TriggerShape shape;
    private double rad;
    @OODPExclude
    private BukkitRunnable runnable;
    @OODPExclude
    private boolean satisfied = false;

    public WithinDistance() { super(Type.WITHIN_DISTANCE); }
    public WithinDistance(@NotNull Location loc) {
        super(Type.WITHIN_DISTANCE);
        this.loc = loc;
        this.rad = 1;
        this.shape = TriggerShape.SPHERE;
        start();
    }

    @Override
    public void start() {
        this.runnable = new BukkitRunnable() {
            Collection<Entity> c;
            @Override
            public void run() {
                c = loc.getWorld().getNearbyEntities(loc, rad, rad, rad);
                if (hasPlayers(c)) {
                    if (!satisfied) {
                        satisfied = true;
                        WithinDistance.this.trigger();
                    }
                } else satisfied = false;
            }
        };
        this.runnable.runTaskTimer(Main.plugin, 0, 2);
    }

    public void setLoc(Location loc) {
        this.loc = loc;
    }

    public Location getLoc() {
        return loc;
    }

    public void setRad(double rad) {
        this.rad = rad;
    }

    public double getRad() { return rad; }

    public void setShape(TriggerShape shape) { this.shape = shape; }

    public TriggerShape getShape() { return shape; }

    @Override
    protected void createGUI() {

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
