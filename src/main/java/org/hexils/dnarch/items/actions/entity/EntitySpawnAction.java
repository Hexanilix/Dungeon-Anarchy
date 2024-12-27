package org.hexils.dnarch.items.actions.entity;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.*;
import org.hexils.dnarch.items.Multiplier;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;


import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;

public class EntitySpawnAction extends BlockAction {

    private List<org.hexils.dnarch.items.EntitySpawn> entities;
    private final EntitySpawnCondition ent_spaw_c = new EntitySpawnCondition();
    private final EntityDeathCondition entity_death_event = new EntityDeathCondition();
    private List<Entity> spawned_entities = null;
    private Multiplier multiplier = null;

    public EntitySpawnAction(org.hexils.dnarch.items.EntitySpawn e, Block sp) { this(List.of(e), List.of(sp)); }
    public EntitySpawnAction(org.hexils.dnarch.items.EntitySpawn entity, List<Block> spawnp) { this(List.of(entity), spawnp); }
    public EntitySpawnAction(List<org.hexils.dnarch.items.EntitySpawn> entities, List<Block> spawnp) {
        super(Type.ENTITY_SPAWN_ACTION, spawnp);
        this.entities = entities;
        this.cgui = new ItemListGUI(getName(), List.of(ent_spaw_c, entity_death_event));
    }

    @Override
    public void trigger() {
        if (!triggered) {
            List<Entity> spawnede = new ArrayList<>();
            Random r = new Random();
            for (int i = 0; i < (multiplier == null ? 1 : multiplier.multiplier()); i++) for (org.hexils.dnarch.items.EntitySpawn e : entities) {
                Location l = this.affected_blocks.get(r.nextInt(this.affected_blocks.size())).getLocation().add(.5, .5, .5);
                Entity ent = l.getWorld().spawnEntity(l, e.type);
                spawnede.add(ent);
                if (e.name != null) {
                    ent.setCustomNameVisible(true);
                    ent.setCustomName(e.name);
                } else
                    log(Level.SEVERE, "An error occurred when spawning " + e.type.name() + " entity at " + org.hetils.mpdl.LocationUtil.toReadableFormat(l));
            }
            this.spawned_entities = spawnede;
            this.triggered = true;
            ent_spaw_c.trigger();
        }
    }

    @Override
    protected void resetAction() {
        this.spawned_entities.forEach(Entity::remove); spawned_entities.clear();
    }

    private @NotNull List<String> entsToString() {
        List<String> l = new ArrayList<>();
        for (org.hexils.dnarch.items.EntitySpawn e : entities)
            l.add(ChatColor.GRAY + e.type.name());
        return l;
    }

    @Override
    protected ItemStack genItemStack() {
        ItemStack i = newItemStack(Material.SPAWNER, getName(), entsToString());
        return i;
    }

    public List<Entity> getSpawnedEntities() { return spawned_entities; }

    private final ItemListGUI cgui;
    @Override
    public void rename(@NotNull DungeonMaster dm, Runnable onRename) { super.rename(dm, () -> { onRename.run(); cgui.setName(getName()); }); }

    @Override
    protected void createGUI() {
        this.setSize(54);
        this.setNameSign(13);
        this.setItem(12, cgui.toItem());

        this.fillBox(27, 9, 3, (ItemStack) null);
    }

    @Override
    protected void updateGUI() {
        this.setItem(23, (multiplier == null ? newItemStack(Material.RED_STAINED_GLASS, ChatColor.YELLOW + "No multiplier") : multiplier.getItem()));
        this.fillBox(27, 9, 3, entities.stream().map(DAItem::getItem).toList());
    }

    @Override
    protected void action(DungeonMaster dm, @NotNull String action, String[] args, InventoryClickEvent event) {
        switch (action) {

        }
    }

    public EntitySpawnCondition getEntitySpawnCondition() { return ent_spaw_c; }
    public EntityDeathCondition getEntityDeathCondition() { return entity_death_event; }

    @Override
    public String toString() {
        return "Spawn{" +
                "entities=" + entities +
                ", spawnp=" + this.affected_blocks +
                ", ent_spaw_c=" + ent_spaw_c +
                ", spawnede=" + spawned_entities +
                ", triggered=" + triggered +
                ", name='" + getName() + '\'' +
                '}';
    }

    public class EntitySpawnCondition extends Condition {
        public EntitySpawnCondition() {
            super(Type.ENTITY_SPAWN_EVENT);
        }

        @Override
        public boolean isSatisfied() { return EntitySpawnAction.this.isTriggered(); }

        @Override
        protected void createGUI() {

        }

        @Override
        protected ItemStack genItemStack() { return newItemStack(entities.get(0).getItem().getType(), "Entity Death Condition"); }

    }

    public class EntityDeathCondition extends Condition {
        public static List<EntityDeathCondition> instances = new ArrayList<>();

        public EntitySpawnAction getAction() { return EntitySpawnAction.this; }

        public EntityDeathCondition() {
            super(Type.ENTITY_DEATH_EVENT);
            instances.add(this);
        }

        @Override
        public boolean isSatisfied() { return EntitySpawnAction.this.isTriggered() && EntitySpawnAction.this.getSpawnedEntities().stream().allMatch(Entity::isDead); }

        @Override
        protected ItemStack genItemStack() { return newItemStack(Material.SKELETON_SKULL, getName()); }

        @Override
        protected void createGUI() {
            this.setSize(54);
            this.setItem(24, null);
        }
    }
}
