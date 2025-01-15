package org.hexils.dnarch.items.actions;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.Action;
import org.hexils.dnarch.Booled;
import org.hexils.dnarch.DungeonMaster;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hetils.mpdl.GeneralUtil.msToTicks;
import static org.hetils.mpdl.ItemUtil.newItemStack;

public class TimerAction extends Action {
    private boolean t = false;
    private int timer = 0;
    private Set<? extends Action> actions = new HashSet<>();

    public TimerAction(int time) {
        super(Type.TIMER);
        this.timer = time;
    }

    @Override
    public void onTrigger() { t = true; new BukkitRunnable() { @Override public void run() {  } }.runTaskLater(Main.plugin, msToTicks(timer)); }

    @Override
    protected void resetAction() { t = false; actions.forEach(Action::reset); }

    @Override
    protected void createGUI() {

    }

    @Override
    protected void updateGUI() {

    }

    @Override
    protected ItemStack genItemStack() {
        return newItemStack(Material.CLOCK, getName(), List.of(String.valueOf(timer)));
    }

}
