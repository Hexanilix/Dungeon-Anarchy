package org.hexils.dnarch.da.objects.actions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.da.Action;
import org.hexils.dnarch.da.dungeon.DungeonMaster;
import org.hexils.dnarch.da.objects.conditions.EntitySpawn;

import java.util.*;

import static org.hetils.mpdl.Item.newItemStack;

public class Spawn extends Action {

    private Collection<EntitySpawn> entities = new ArrayList<>();
    private List<Location> spawnp = new ArrayList<>();
    public Spawn(EntitySpawn e, Location sp) {
        super(Type.SPAWN);
        this.entities.add(e);
        this.spawnp.add(sp);
    }

    public Spawn(Collection<EntitySpawn> entities, List<Location> spawnp) {
        super(Type.SPAWN);
        this.entities = entities;
        this.spawnp = spawnp;
    }

    public Spawn(EntitySpawn entity, List<Location> spawnp) {
        super(Type.SPAWN);
        this.entities.add(entity);
        this.spawnp = spawnp;
    }

    private Collection<Entity> spawnede = new HashSet<>();
    @Override
    public void execute() {
        Random r = new Random();
        for (EntitySpawn e : entities) {
            Location l = spawnp.get(r.nextInt(spawnp.size()));
            Entity ent = l.getWorld().spawnEntity(l, e.type);
            spawnede.add(ent);
            if (e.name != null) {
                ent.setCustomNameVisible(true);
                ent.setCustomName(e.name);
            }
        }
    }

    @Override
    protected void resetAction() {
        spawnede.forEach(Entity::remove);
    }

    @Override
    protected void createGUIInventory() {

    }

    @Override
    public void updateGUI() {

    }

    private List<String> entsToString() {
        List<String> l = new ArrayList<>();
        for (EntitySpawn e : entities)
            l.add(ChatColor.GRAY + e.type.name());
        return l;
    }

    @Override
    protected ItemStack toItem() {
        ItemStack i = newItemStack(Material.SPAWNER, name, entsToString());
        return i;
    }

    @Override
    protected void changeField(DungeonMaster dm, String field, String value) {

    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args) {

    }
}
