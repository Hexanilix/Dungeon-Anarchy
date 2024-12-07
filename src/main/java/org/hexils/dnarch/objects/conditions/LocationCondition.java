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
import java.util.List;

public class LocationCondition extends Condition {
    private org.hetils.mpdl.Location.Box bounds;
    private BukkitRunnable runnable;
    private boolean satisfied = false;
    public LocationCondition(Pair<Location, Location> bounds) {
        super(Type.LOCATION);
        this.bounds = new org.hetils.mpdl.Location.Box(bounds);
        this.runnable = new BukkitRunnable() {
            Collection<Entity> c;
            final World w = LocationCondition.this.bounds.getWorld();
            final Location loc = org.hetils.mpdl.Location.getCenter();
            final double rad = loc.distance(bounds.key());
            @Override
            public void run() {
                c = w.getNearbyEntities(loc, rad, rad, rad);
                hasPlayers(c);
            }
        };
        this.runnable.runTaskTimer(Main.plugin, 0, 2);
    }

    public void hasPlayers(@NotNull Collection<Entity> ents) {
        satisfied = false;
        for (Entity e : ents) {
            if (e instanceof Player p && bounds.contains(p.getLocation())) {
                satisfied = true;
                break;
            }
        }
    }

    @Override
    public boolean isSatisfied() {
        return satisfied;
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
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args) {

    }

    @Override
    protected void onTrigger() {

    }
}
