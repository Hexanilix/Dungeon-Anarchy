package org.hexils.dnarch.items.conditions.entity;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.hexils.dnarch.Condition;
import org.hexils.dnarch.DA_item;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.actions.entity.EntitySpawnAction;

import java.util.ArrayList;
import java.util.List;

import static org.hetils.mpdl.ItemUtil.newItemStack;

public class EntityDeath extends Condition {
    public static List<EntityDeath> instances = new ArrayList<>();
    private EntitySpawnAction.EntityCollection ec = null;

    public EntitySpawnAction.EntityCollection getEc() { return spawn.getColl(); }

    private final EntitySpawnAction spawn;
    public EntityDeath(EntitySpawnAction spanwn) {
        super(Type.ENTITY_DEATH_EVENT);
        this.spawn = spanwn;
        instances.add(this);
    }

    @Override
    protected void onTrigger() {

    }

    @Override
    public boolean isSatisfied() { return spawn.isTriggered() && spawn.getColl().getEntities().stream().allMatch(Entity::isDead); }

    @Override
    protected ItemStack genItemStack() { return newItemStack(Material.IRON_SWORD, getName()); }

    @Override
    protected void createGUI() {
        this.setSize(54);
        this.setItem(24, null);
    }

    @Override
    public boolean guiClickEvent(InventoryClickEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                DA_item da = DA_item.get(getItem(24));
                if (da instanceof EntitySpawnAction.EntityCollection e) {
                    ec = e;
                }
            }
        }.runTaskLater(Main.plugin, 1);
        return true;
    }
}
