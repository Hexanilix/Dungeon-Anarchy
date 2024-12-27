package org.hexils.dnarch.items.actions;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.Action;
import org.hexils.dnarch.Booled;
import org.hexils.dnarch.DungeonMaster;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

public class TimerAction extends Action implements Booled {
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
    public void trigger() {
        this.cd.runTaskTimer(Main.plugin, 0, 0);
    }

    @Override
    protected void resetAction() {
        timer = start;
    }

    @Override
    protected void createGUI() {
    }

    @Override
    protected void updateGUI() {

    }

    @Override
    protected ItemStack genItemStack() {
        return null;
    }

    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {

    }

    @Override
    public boolean isSatisfied() {
        return timer <= 0;
    }
}
