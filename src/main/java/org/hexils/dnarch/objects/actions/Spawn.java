package org.hexils.dnarch.objects.actions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.hetils.mpdl.Inventory;
import org.hexils.dnarch.Action;
import org.hexils.dnarch.DA_item;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.EntitySpawn;
import org.hexils.dnarch.objects.conditions.EntitySpawnCondition;

import java.util.*;
import java.util.logging.Level;

import static org.hetils.mpdl.General.log;
import static org.hetils.mpdl.Item.newItemStack;

public class Spawn extends Action {
    public class EntityCollenction extends DA_item {
        private List<Entity> entities;

        public EntityCollenction(List<Entity> entities) {
            this.entities = entities;
        }

        @Override
        protected void createGUIInventory() {
            this.guiSize(54);
            updateGUI();
        }

        @Override
        public void updateGUI() {
            Inventory.fillBox(gui, 9, 9, 5);
            Inventory.fillBox(gui, 9, 9, 5, entities.stream().map(org.hetils.mpdl.Entity::toItem).toList());
            if (entities.size() > 45) {
                List<String> ents = new ArrayList<>();
                for (int i = 44; i < entities.size(); i++)
                    ents.add(ChatColor.GRAY + entities.get(i).getType().name());
                gui.setItem(53, newItemStack(Material.ENDER_CHEST, ChatColor.WHITE + "...and " + (entities.size()-44) + " others:", ents));
            }
        }

        @Override
        protected ItemStack toItem() {
            ItemStack i = newItemStack(Material.SPAWNER, "Entity Collection");
            return i;
        }
    }

    private Collection<EntitySpawn> entities = new ArrayList<>();
    private List<Location> spawnp = new ArrayList<>();
    private final EntitySpawnCondition ent_spaw_c = new EntitySpawnCondition(this);

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
            } else log(Level.SEVERE, "An error occurred when spawning " + e.type.name() + " entity at " + org.hetils.mpdl.Location.toReadableFormat(l));
        }
        this.triggered = true;
        ent_spaw_c.trigger();
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

    public EntitySpawnCondition getEntitySpawnCondition() {
        return ent_spaw_c;
    }

    @Override
    public String toString() {
        return "Spawn{" +
                "entities=" + entities +
                ", spawnp=" + spawnp +
                ", ent_spaw_c=" + ent_spaw_c +
                ", spawnede=" + spawnede +
                ", triggered=" + triggered +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", gui=" + gui +
                ", name='" + name + '\'' +
                '}';
    }
}
