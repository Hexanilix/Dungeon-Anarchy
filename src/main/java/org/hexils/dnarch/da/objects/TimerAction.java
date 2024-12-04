package org.hexils.dnarch.da.objects;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.da.Action;
import org.hexils.dnarch.da.Booled;
import org.hexils.dnarch.da.DM;
import org.hexils.dnarch.da.Triggerable;
import org.hexils.dnarch.da.actions.Type;
import org.jetbrains.annotations.NotNull;

public class TimerAction extends Action implements Triggerable, Booled {
    private int timer = 0;
    private int start = 0;
    private final BukkitRunnable cd = new BukkitRunnable() {
        @Override
        public void run() {
            timer-=50;
            if (timer <= 0) {
                trigger();
                cancel();
            }
        }
    };
    public TimerAction(int time) {
        super(Type.TIMER);
        this.start = time;
        this.timer = time;
        this.cd.runTaskTimer(Main.plugin, 0, 0);
    }

    @Override
    public void execute() {
        this.cd.runTaskTimer(Main.plugin, 0, 0);
    }

    @Override
    protected void resetAction() {
        timer = start;
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
    protected void changeField(DM dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DM dm, String action, String[] args) {

    }

    @Override
    public void trigger() {

    }

    @Override
    public boolean isSatisfied() {
        return timer <= 0;
    }
}
