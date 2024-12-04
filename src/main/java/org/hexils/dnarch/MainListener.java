package org.hexils.dnarch;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.da.DA_block;
import org.hexils.dnarch.da.DA_item;
import org.hexils.dnarch.da.DM;
import org.hexils.dnarch.da.GUI;
import org.hexils.dnarch.events.runnables.InventoryClickRunnable;
import org.hexils.dnarch.events.runnables.InventoryCloseRunnable;
import org.hexils.dnarch.events.runnables.InventoryOpenRunnable;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hexils.dnarch.Main.*;
import static org.hexils.dnarch.da.DA_item.ITEM_UUID;

public final class MainListener implements org.bukkit.event.Listener {
    @EventHandler
    public void onSelct(@NotNull PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        if (item.isSimilar(wand)) {
            DM dm = DM.getOrNew(p);
            Block b = event.getClickedBlock();
            switch (event.getAction()) {
                case RIGHT_CLICK_BLOCK -> {
                    if (p.isSneaking()) {
                        if (b == null) return;
                        if (!dm.setSelectionA(b.getLocation()))
                            dm.clearSelectionA();
                        event.setCancelled(true);
                    } else {
                        if (!dm.selectBlock(b))
                            dm.deselectBlock(b);
                    }
                }
                case LEFT_CLICK_BLOCK -> {
                    if (p.isSneaking()) {
                        if (b == null) return;
                        if (!dm.setSelectionB(b.getLocation()))
                            dm.clearSelectionB();
                        event.setCancelled(true);
                    }
                }
            }
        } else if (NSK.hasNSK(event.getItem(), GUI.MODIFIABLE, true)) {
            String s = (String) NSK.getNSK(event.getItem(), ITEM_UUID);
            if (s != null) {
                UUID id = UUID.fromString(s);
                DA_item di = DA_item.get(id);
                if (di != null) {
                    di.manage(event.getPlayer());
                    event.setCancelled(true);
                }
            }
        }
    }

    public static final Map<Inventory, InventoryCloseRunnable> onClose = new HashMap<>();
    public static final Map<Inventory, InventoryOpenRunnable> onOpen = new HashMap<>();
    public static final Map<Inventory, InventoryClickRunnable> onClick = new HashMap<>();
    private static final Map<Player, Map<Object, Runnable>> onLeave = new HashMap<>();
    public static void runOnPlayerLeave(Object o, Player p, Runnable r) {
        onLeave.putIfAbsent(p, new HashMap<>());
        onLeave.get(p).put(o, r);
    }

    @EventHandler
    public void onInvOpen(@NotNull InventoryOpenEvent event) {
        Inventory i = event.getInventory();
        for (Map.Entry<Inventory, InventoryOpenRunnable> r : onOpen.entrySet())
            if (r.getKey() == i) {
                onOpen.remove(r.getKey());
                r.getValue().run(event);
            }
    }

    @EventHandler
    public void onInvClose(@NotNull InventoryCloseEvent event) {
        Inventory i = event.getInventory();
        for (Map.Entry<Inventory, InventoryCloseRunnable> r : onClose.entrySet())
            if (r.getKey() == i) {
                onClose.remove(r.getKey());
                r.getValue().run(event);
            }
    }

    @EventHandler
    public void onInvClick(@NotNull InventoryClickEvent event) {
        Inventory clicked_inv = event.getClickedInventory();
        Inventory opi = event.getInventory();
        for (Map.Entry<Inventory, InventoryClickRunnable> r : onClick.entrySet())
            if (r.getKey() == clicked_inv) {
                if (r.getValue().run(event))
                    onClick.remove(r.getKey());
            }
        if (event.getWhoClicked() instanceof Player p) {
            for (DA_item a : DA_item.instances)
                if (a.getGUI() == opi) {
                    event.setCancelled(true);
                    ItemStack it = event.getCurrentItem();
                    if (DA_item.get(it) == a)
                        return;
                    if (a.guiClickEvent(event)) {
                        if (event.getAction() == InventoryAction.CLONE_STACK) {
                            DA_item da = DA_item.get(it);
                            if (da != null)
                                da.manage(p, a);
                        } else if (NSK.hasNSK(it, GUI.ITEM_RENAME)) {
                            a.promptRename(p);
                        } else if (NSK.hasNSK(it, GUI.FIELD_VALUE))
                            a.setField(DM.getOrNew(p), (String) NSK.getNSK(event.getCurrentItem(), GUI.FIELD_VALUE));
                    }
                }
        }
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (NSK.hasNSK(item, DA_block.BLOCK, true)) {
            if (NSK.hasNSK(item, GUI.MODIFIABLE, true)) {
                String s = (String) NSK.getNSK(item, ITEM_UUID);
                if (s != null) {
                    UUID id = UUID.fromString(s);
                    DA_item dai = DA_item.get(id);
                    if (dai != null)
                        dai.manage(event.getPlayer());
                }
                event.getBlockPlaced().getBlockData();
            }
        }
    }
}