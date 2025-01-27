package org.hexils.dnarch.items.actions;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.GeneralListener;
import org.hexils.dnarch.*;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hetils.mpdl.GeneralUtil.msToTicks;
import static org.hetils.mpdl.item.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;

public class TimerAction extends Action {
    @OODPExclude
    private boolean trigged = false;
    private int timer_time;
    private final Set<Action> actions = new HashSet<>();
    @OODPExclude
    private BukkitTask active_timer;

    public TimerAction() { this(1000); }
    public TimerAction(int time) {
        super(Type.TIMER);
        this.timer_time = time;
        this.allowClassesForGui(Action.class);
    }

    @Override
    public void trigger() {
        trigged = true;
        active_timer = new BukkitRunnable() {
            @Override
            public void run() {
                actions.forEach(Triggerable::trigger);
            }
        }.runTaskLater(Main.plugin(),msToTicks(timer_time));
    }

    @Override
    protected void resetAction() {
        trigged = false;
        if (active_timer != null) active_timer.cancel();
        active_timer = null;
        actions.forEach(Action::reset);
    }

    @Override
    protected void createGUI() {
        this.fillBox(27, 9, 3);
    }

    @Override
    protected void updateGUI() {
        updateTimer();
        this.fillBox(27, 9, 3, actions);
    }

    private void updateTimer() {
//        ItemUtil.setLore(this.getItem(13), List.of("Timer: " + timer_time));
        this.setAction(13, newItemStack(Material.CLOCK, "Timer: ", List.of(String.valueOf(timer_time))), "time");
    }

    @Override
    protected void action(DungeonMaster dm, @NotNull String action, String[] args, ClickType click) {
        switch (action) {
            case "time" -> {
                if (click.name().contains("LEFT")) {
                    timer_time -= click.isShiftClick() ? 50 : 250;
                    updateTimer();
                } else if (click.name().contains("RIGHT")) {
                    timer_time += click.isShiftClick() ? 50 : 250;
                    updateTimer();
                } else if (click == ClickType.MIDDLE) {
                    GeneralListener.promptPlayer(dm, dm.getMessage(ChatColor.RESET, DungeonMaster.Sender.CREATOR.toString(), "Enter new timer:"), (t) -> {
                        try {
                            timer_time = Integer.parseInt(t);
                            updateTimer();
                        } catch (NumberFormatException ignore) {}
                    });
                }
            }
        }
    }

    @Override
    public void onInvClose() {
        actions.clear();
        ItemStack[] items = this.getBox(27, 9, 3);
        for (ItemStack i : items) {
            DAItem da = DAItem.get(i);
            if (da instanceof Action a) {
                actions.add(a);
            }
        }
    }

    @Override
    protected ItemStack genItemStack() {
        return newItemStack(Material.CLOCK, getName(), List.of(String.valueOf(timer_time)));
    }



    @Override
    public DAItem create(DungeonMaster dm, String @NotNull [] args) {
        if (args.length > 0) {
            try {
                timer_time = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignore) {}
        }
        return this;
    }
}
