package org.hexils.dnarch.items.actions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Action;
import org.hexils.dnarch.DA_item;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.items.EntitySpawn;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.conditions.EntitySpawnCondition;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;


import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;

public class Spawn extends Action {
    public static class EntityCollection extends DA_item {
        private List<Entity> entities;

        public EntityCollection(List<Entity> entities) {
            this.entities = entities;
        }

        @Override
        protected void createGUI() {
            this.setSize(54);
            updateGUI();
        }

        @Override
        public void updateGUI() {
            this.fillBox(9, 9, 5);
            this.fillBox(9, 9, 5, entities.stream().map(org.hetils.mpdl.EntityUtil::toItem).toList());
            if (entities.size() > 45) {
                List<String> ents = new ArrayList<>();
                for (int i = 44; i < entities.size(); i++)
                    ents.add(ChatColor.GRAY + entities.get(i).getType().name());
                this.setItem(53, newItemStack(Material.ENDER_CHEST, ChatColor.WHITE + "...and " + (entities.size()-44) + " others:", ents));
            }
        }

        @Override
        protected ItemStack toItem() {
            ItemStack i = newItemStack(Material.SPAWNER, "Entity Collection");
            return i;
        }

        public void delete() {
            entities.forEach(Entity::remove);
            super.delete();
        }

        public List<Entity> getEntities() { return entities; }
    }

    private Collection<EntitySpawn> entities = new ArrayList<>();
    private EntityCollection s_ent_c = null;
    private List<Location> spawnp = new ArrayList<>();
    private final EntitySpawnCondition ent_spaw_c = new EntitySpawnCondition(this);

    public Spawn(EntitySpawn e, Location sp) {
        super(Type.ENTITY_SPAWN_ACTION);
        this.entities.add(e);
        this.spawnp.add(sp);
    }

    public Spawn(Collection<EntitySpawn> entities, List<Location> spawnp) {
        super(Type.ENTITY_SPAWN_ACTION);
        this.entities = entities;
        this.spawnp = spawnp;
    }

    public Spawn(EntitySpawn entity, List<Location> spawnp) {
        super(Type.ENTITY_SPAWN_ACTION);
        this.entities.add(entity);
        this.spawnp = spawnp;
    }

    @Override
    public void trigger() {
        List<Entity> spawnede = new ArrayList<>();
        Random r = new Random();
        for (EntitySpawn e : entities) {
            Location l = spawnp.get(r.nextInt(spawnp.size()));
            Entity ent = l.getWorld().spawnEntity(l, e.type);
            spawnede.add(ent);
            if (e.name != null) {
                ent.setCustomNameVisible(true);
                ent.setCustomName(e.name);
            } else log(Level.SEVERE, "An error occurred when spawning " + e.type.name() + " entity at " + org.hetils.mpdl.LocationUtil.toReadableFormat(l));
        }
        this.s_ent_c = new EntityCollection(spawnede);
        this.triggered = true;
        ent_spaw_c.trigger();
    }

    @Override
    protected void resetAction() {
        this.s_ent_c.delete();
    }

    @Override
    protected void createGUI() {
        this.setSize(54);
        this.setNameSign(13);
        this.fillBox(27, 9, 3, entities.stream().map(DA_item::getItem).toList());
    }

    private @NotNull List<String> entsToString() {
        List<String> l = new ArrayList<>();
        for (EntitySpawn e : entities)
            l.add(ChatColor.GRAY + e.type.name());
        return l;
    }

    @Override
    protected ItemStack toItem() {
        ItemStack i = newItemStack(Material.SPAWNER, getName(), entsToString());
        return i;
    }

    public EntityCollection getColl() {
        return s_ent_c;
    }

    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {
        switch (action) {
            case "getEntColl" -> dm.give(s_ent_c);
            case "getEntCond" -> dm.give(ent_spaw_c);
        }
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
                ", spawnede=" + s_ent_c +
                ", triggered=" + triggered +
                ", type=" + type +
                ", name='" + getName() + '\'' +
                '}';
    }
}
