package org.hexils.dnarch.items.actions.entity;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hexils.dnarch.*;
import org.hexils.dnarch.items.EntitySpawn;
import org.hexils.dnarch.items.NumberHolder;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;


import static org.hetils.mpdl.item.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;

public class EntitySpawnAction extends BlockAction {

    static {
        setTabComplete(args -> {
            if (args.length == 1) return Arrays.stream(EntityType.values()).map(e -> e.name().toLowerCase()).toList();
            else return null;
        });
    }

    private final List<org.hexils.dnarch.items.EntitySpawn> entities = new ArrayList<>();
    @OODPExclude
    private final EntitySpawnCondition ent_spaw_c = new EntitySpawnCondition();
    @OODPExclude
    private final EntityDeathCondition entity_death_event = new EntityDeathCondition();
    @OODPExclude
    private List<Entity> spawned_entities = null;
    private NumberHolder multiplier = null;

    public EntitySpawnAction() { super(Type.ENTITY_SPAWN_ACTION); }
    public EntitySpawnAction(org.hexils.dnarch.items.EntitySpawn e, Block sp) { this(List.of(e), List.of(sp)); }
    public EntitySpawnAction(org.hexils.dnarch.items.EntitySpawn entity, List<Block> spawnp) { this(List.of(entity), spawnp); }
    public EntitySpawnAction(List<org.hexils.dnarch.items.EntitySpawn> entities, List<Block> spawnp) {
        super(Type.ENTITY_SPAWN_ACTION, spawnp);
        if (spawnp.isEmpty()) throw new IllegalArgumentException("Spawn locations cannot be null");
        this.entities.addAll(entities);
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
                    log(Level.SEVERE, "An error occurred when spawning " + e.type.name() + " entity at " + org.hetils.mpdl.location.LocationUtil.toReadableFormat(l));
            }
            this.spawned_entities = spawnede;
            this.triggered = true;
            ent_spaw_c.trigger();
        }
    }

    @Override
    protected void resetAction() {
        if (spawned_entities != null) {
            this.spawned_entities.forEach(Entity::remove);
            spawned_entities.clear();
        }
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

    @OODPExclude
    private final ItemListGUI cgui = new ItemListGUI(getName(), ent_spaw_c, entity_death_event);;

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

    @Override
    public DAItem create(@NotNull DungeonMaster dm, String[] args) {
        if (dm.hasBlocksSelected())
            affected_blocks.addAll(dm.getSelectedBlocks());
        if (args.length > 0) {
            this.entities.add(new EntitySpawn(EntityType.valueOf(args[0].toUpperCase())));
        }
        return this;
    }

    public class EntitySpawnCondition extends Condition {
        public EntitySpawnCondition() { super(Type.ENTITY_SPAWN_EVENT); }

        @Override
        protected void createGUI() {}

        @Override
        public boolean isSatisfied() { return EntitySpawnAction.this.isTriggered(); }

        @Override
        protected ItemStack genItemStack() { return newItemStack(entities.get(0).getItem().getType(), "Entity Death Condition"); }

        @Override
        public DAItem create(DungeonMaster dm, String[] args) { return null; }
    }

    public class EntityDeathCondition extends Condition {
        public static Set<EntityDeathCondition> instances = new HashSet<>();

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

        @Override
        public DAItem create(@NotNull DungeonMaster dm, String[] args) { return this; }
    }
}
