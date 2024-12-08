package org.hexils.dnarch.objects.conditions;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.hetils.jgl17.Pair;
import org.hexils.dnarch.Condition;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class WithinBoundsCondition extends Condition {
    private org.hetils.mpdl.Location.Box bounds;
    private BukkitRunnable runnable;
    private boolean satisfied = false;
    public WithinBoundsCondition(Pair<Location, Location> bounds) {
        super(Type.WITHIN_BOUNDS);
        this.bounds = new org.hetils.mpdl.Location.Box(bounds);
        this.runnable = new BukkitRunnable() {
            Collection<Entity> c;
            final World w = WithinBoundsCondition.this.bounds.getWorld();
            final Location loc = org.hetils.mpdl.Location.getCenter(bounds.key(), bounds.value());
            final double rad = loc.distance(bounds.key());
            @Override
            public void run() {
                c = w.getNearbyEntities(loc, rad, rad, rad);
                satisfied = false;
                for (Entity e : c)
                    if (e instanceof Player p && WithinBoundsCondition.this.bounds.contains(p.getLocation())) {
                        satisfied = true;
                        trigger();
                        break;
                    }
            }
        };
        this.runnable.runTaskTimer(Main.plugin, 0, 2);
    }

    @Override
    public boolean isSatisfied() {
        return satisfied;
    }

    @Override
    protected void createGUIInventory() {

    }

    @Override
    protected ItemStack toItem() {
        return null;
    }

    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args) {

    }

    @Override
    protected void onTrigger() {

    }
}
