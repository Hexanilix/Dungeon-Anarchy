package org.hexils.dnarch.da.actions;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.da.Action;
import org.hexils.dnarch.da.DM;
import org.hexils.dnarch.da.EntitySpawn;

import java.util.*;

public class Spawn extends Action {

    private Collection<EntitySpawn> entities;
    private List<Location> spawnp;
    public Spawn(EntitySpawn e, Location sp) {
        this(new HashSet<>(), new ArrayList<>());
        this.entities.add(e);
        this.spawnp.add(sp);
    }

    public Spawn(Collection<EntitySpawn> entities, List<Location> spawnp) {
        this.entities = entities;
        this.spawnp = spawnp;
    }

    private Collection<Entity> spawnede = new HashSet<>();
    @Override
    public void execute() {
        Random r = new Random();
        for (EntitySpawn e : entities) {
            Location l = spawnp.get(r.nextInt(spawnp.size()));
            Entity ent = l.getWorld().spawnEntity(l, e.type());
            spawnede.add(ent);
            if (e.name() != null) {
                ent.setCustomNameVisible(true);
                ent.setCustomName(e.name());
            }
        }
    }

    @Override
    protected void resetAction() {
        spawnede.forEach(Entity::remove);
    }

    @Override
    protected Inventory createGUIInventory() {
        return null;
    }

    @Override
    public void updateGUI() {

    }

    @Override
    protected ItemStack toItem() {
        return null;
    }

    @Override
    protected void changeField(DM dm, String field, String value) {

    }

    @Override
    protected void action(DM dm, String action, String[] args) {

    }
}
