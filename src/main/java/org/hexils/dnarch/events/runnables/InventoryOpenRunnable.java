package org.hexils.dnarch.events.runnables;

import org.bukkit.event.inventory.InventoryCloseEvent;

public interface InventoryCloseRunnable {

    boolean run(InventoryCloseEvent event);
}
