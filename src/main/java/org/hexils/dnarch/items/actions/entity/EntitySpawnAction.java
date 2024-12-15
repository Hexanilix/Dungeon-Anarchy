package org.hexils.dnarch.items.actions.entity;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hexils.dnarch.BlockAction;
import org.hexils.dnarch.DA_item;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.conditions.entity.EntityDeath;
import org.hexils.dnarch.items.conditions.entity.EntitySpawnCondition;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;


import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;

public class EntitySpawnAction extends BlockAction {
    public static class EntityCollection extends DA_item {
        private List<Entity> entities;

        public EntityCollection(List<Entity> entities) {
            super(Type.ENTITY_COLLECTION);
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
        protected ItemStack genItemStack() {
            ItemStack i = newItemStack(Material.SPAWNER, "Entity Collection");
            return i;
        }

        public void delete() {
            entities.forEach(Entity::remove);
            super.delete();
        }

        public List<Entity> getEntities() { return entities; }
    }

    private Collection<org.hexils.dnarch.items.EntitySpawn> entities = new ArrayList<>();
    private EntityCollection s_ent_c = null;
    private final EntitySpawnCondition ent_spaw_c = new EntitySpawnCondition(this);
    private final EntityDeath entity_death_event = new EntityDeath(this);

    public EntitySpawnAction(org.hexils.dnarch.items.EntitySpawn e, Block sp) { this(List.of(e), List.of(sp)); }
    public EntitySpawnAction(org.hexils.dnarch.items.EntitySpawn entity, List<Block> spawnp) { this(List.of(entity), spawnp); }
    public EntitySpawnAction(Collection<org.hexils.dnarch.items.EntitySpawn> entities, List<Block> spawnp) {
        super(Type.ENTITY_SPAWN_ACTION, spawnp);
        this.entities = entities;
        this.cgui = new ConditionGUI(getName(), List.of(ent_spaw_c, entity_death_event));
    }

    @Override
    public void trigger() {
        if (!triggered) {
            List<Entity> spawnede = new ArrayList<>();
            Random r = new Random();
            for (org.hexils.dnarch.items.EntitySpawn e : entities) {
                Location l = this.affected_blocks.get(r.nextInt(this.affected_blocks.size())).getLocation().add(.5, .5, .5);
                Entity ent = l.getWorld().spawnEntity(l, e.type);
                spawnede.add(ent);
                if (e.name != null) {
                    ent.setCustomNameVisible(true);
                    ent.setCustomName(e.name);
                } else
                    log(Level.SEVERE, "An error occurred when spawning " + e.type.name() + " entity at " + org.hetils.mpdl.LocationUtil.toReadableFormat(l));
            }
            this.s_ent_c = new EntityCollection(spawnede);
            this.triggered = true;
            ent_spaw_c.trigger();
        }
    }

    @Override
    protected void resetAction() {
        this.s_ent_c.delete();
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
        ItemMeta m = i .getItemMeta();
        return i;
    }

    public EntityCollection getColl() {
        return s_ent_c;
    }

    private final ConditionGUI cgui;
    @Override
    public void rename(@NotNull DungeonMaster dm, Runnable onRename) { super.rename(dm, () -> { onRename.run(); cgui.setName(getName()); }); }

    @Override
    protected void createGUI() {
        this.setSize(54);
        this.setNameSign(13);
        this.setItem(12, cgui.toItem());

        this.fillBox(27, 9, 3, (ItemStack) null);
        this.fillBox(27, 9, 3, entities.stream().map(DA_item::getItem).toList());
    }

    @Override
    protected void action(DungeonMaster dm, @NotNull String action, String[] args, InventoryClickEvent event) {
        switch (action) {
            case "getEntColl" -> dm.giveItem(s_ent_c);
            case "getEntCond" -> dm.giveItem(ent_spaw_c);
        }
    }

    public EntitySpawnCondition getEntitySpawnCondition() {
        return ent_spaw_c;
    }

    @Override
    public String toString() {
        return "Spawn{" +
                "entities=" + entities +
                ", spawnp=" + this.affected_blocks +
                ", ent_spaw_c=" + ent_spaw_c +
                ", spawnede=" + s_ent_c +
                ", triggered=" + triggered +
                ", type=" + type +
                ", name='" + getName() + '\'' +
                '}';
    }
}
