package org.hexils.dnarch.items.conditions;

import org.bukkit.entity.Entity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.hexils.dnarch.Condition;
import org.hexils.dnarch.DA_item;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.actions.Spawn;

import java.util.ArrayList;
import java.util.List;

public class EntityDeath extends Condition {
    public static List<EntityDeath> instances = new ArrayList<>();
    private Spawn.EntityCollection ec = null;

    public Spawn.EntityCollection getEc() {
        return ec;
    }

    public EntityDeath() {
        super(Type.ENTITY_DEATH_EVENT);
    }

    @Override
    protected void onTrigger() {

    }

    @Override
    public boolean isSatisfied() {
        return ec != null && ec.getEntities().stream().allMatch(Entity::isDead);
    }

    @Override
    protected ItemStack toItem() {
        return null;
    }

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
                if (da instanceof Spawn.EntityCollection e) {
                    ec = e;
                }
            }
        }.runTaskLater(Main.plugin, 1);
        return true;
    }
}
