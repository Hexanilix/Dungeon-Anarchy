package org.hexils.dnarch.items.actions;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.GeneralListener;
import org.hetils.mpdl.ItemUtil;
import org.hexils.dnarch.*;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hetils.mpdl.GeneralUtil.msToTicks;
import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;

public class TimerAction extends Action {
    @OODPExclude
    private boolean trigged = false;
    private int timer;
    private final Set<DAItem> actions = new HashSet<>();

    public TimerAction() { this(1000); }
    public TimerAction(int time) {
        super(Type.TIMER);
        this.timer = time;
    }

    @Override
    public void onTrigger() { trigged = true;
        new BukkitRunnable() {
            @Override public void run() {
                actions.forEach(da -> {
                    if (da instanceof Triggerable t)
                        t.trigger();
                });
            }
        }.runTaskLater(Main.plugin, msToTicks(timer));
    }

    @Override
    protected void resetAction() {
        trigged = false;
        actions.forEach(t -> {
            if (t instanceof Resetable r) r.reset();
        });
    }

    @Override
    protected void createGUI() {
        this.fillBox(27, 9, 3);
        this.setField(13, newItemStack(Material.CLOCK, "Timer:", List.of(String.valueOf(timer))), "time");
    }

    private void updateTimer() {
        this.fillBox(27, 9, 3, actions.stream().map(DAItem::getItem).toList());
        ItemUtil.setLore(this.getItem(13), List.of("Timer: " + timer));
    }

    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, ClickType click) {
        switch (field) {
            case "time" -> {
                if (click.name().contains("LEFT")) {
                    timer -= 250;
                    updateTimer();
                } else if (click.name().contains("RIGHT")) {
                    timer += 250;
                    updateTimer();
                } else if (click == ClickType.MIDDLE) {
                    GeneralListener.confirmWithPlayer(dm.p, DungeonMaster.getMessage(DungeonMaster.Sender.CREATOR, "Enter new timer:"), (t) -> {
                        try {
                            timer = Integer.parseInt(t);
                            updateTimer();
                        } catch (NumberFormatException ignore) {}
                        return false;
                    });
                }
            }
        }
    }

    @Override
    public boolean guiClickEvent(@NotNull InventoryClickEvent event) {
        ItemStack ci = event.getCurrentItem();
        DAItem a = DAItem.get(ci);
        if (a instanceof Triggerable) {
            event.setCancelled(false);
        }
        return false;
    }

    @Override
    public void onInvClose() {
        actions.clear();
        ItemStack[] items = this.getBox(27, 9, 3);
        for (ItemStack i : items) {
            DAItem a = DAItem.get(i);
            if (a instanceof Triggerable) {
                actions.add(a);
            }
        }
        log(actions);
    }

    @Override
    protected ItemStack genItemStack() {
        return newItemStack(Material.CLOCK, getName(), List.of(String.valueOf(timer)));
    }



    @Override
    public DAItem create(DungeonMaster dm, String[] args) {
        return this;
    }
}
