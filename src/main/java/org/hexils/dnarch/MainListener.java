package org.hexils.dnarch;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.dungeon.Dungeon;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static org.hetils.mpdl.General.log;
import static org.hexils.dnarch.Main.*;
import static org.hexils.dnarch.DA_item.ITEM_UUID;

public final class MainListener implements org.bukkit.event.Listener {
    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        if (item.isSimilar(wand)) {
            DungeonMaster dm = DungeonMaster.getOrNew(p);
            Block b = event.getClickedBlock();
            switch (event.getAction()) {
                case LEFT_CLICK_BLOCK -> {
                    if (p.isSneaking()) {
                        if (b == null) return;
                        if (!dm.setSelectionA(b))
                            dm.clearSelectionA();
                        event.setCancelled(true);
                    }
                }
                case RIGHT_CLICK_BLOCK -> {
                    if (p.isSneaking()) {
                        if (b == null) return;
                        if (!dm.setSelectionB(b))
                            dm.clearSelectionB();
                        event.setCancelled(true);
                    } else if (dm.isEditing() && !dm.selectBlock(b))
                            dm.deselectBlock(b);
                }
            }
        } else if (NSK.hasNSK(event.getItem(), ITEM_UUID)) {
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

//    @EventHandler(priority = EventPriority.HIGHEST)
//    public void onCommandPreprocess(@NotNull PlayerCommandPreprocessEvent event) {
//        // Get the full command (including the "/")
//        String command = event.getMessage().toLowerCase();
//
//        // Example: Run code before any command
//        event.getPlayer().sendMessage("You are about to trigger: " + command);
//
//        // Example: Cancel a specific command ("/example")
//        if (command.startsWith("/da")) {
//            event.getPlayer().sendMessage("Sorry, this command is blocked.");
//            event.setCancelled(true);  // Cancel command execution
//        }
//
//        // Example: Run code and block certain commands based on condition
//        if (event.getPlayer().getName().equalsIgnoreCase("NotAllowedPlayer")) {
//            event.getPlayer().sendMessage("You are not allowed to use commands!");
//            event.setCancelled(true);
//        }
//    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Inventory opi = event.getInventory();
        if (event.getWhoClicked() instanceof Player p) {
            for (Managable a : Managable.instances)
                if (a.getGUI() == opi) {
                    event.setCancelled(true);
                    ItemStack it = event.getCurrentItem();
                    if (DA_item.get(it) == a)
                        return;
                    if (a.guiClickEvent(event)) {
                        DungeonMaster dm = DungeonMaster.getOrNew(p);
                        if (event.getAction() == InventoryAction.CLONE_STACK) {
                            DA_item da = DA_item.get(it);
                            if (da != null)
                                da.manage(p, a);
                        } else if (NSK.hasNSK(it, GUI.ITEM_RENAME)) {
                            a.rename(p);
                        } else if (NSK.hasNSK(it, GUI.ITEM_FIELD_VALUE)) {
                            a.setField(dm, (String) NSK.getNSK(event.getCurrentItem(), GUI.ITEM_FIELD_VALUE));
                        } else if (NSK.hasNSK(it, GUI.ITEM_ACTION)) {
                            a.doAction(dm, (String) NSK.getNSK(it, GUI.ITEM_ACTION));
                        }
                    }
                }
        }
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
//        ItemStack item = event.getItemInHand();
//        if (NSK.hasNSK(item, DA_block.BLOCK, true)) {
//            if (NSK.hasNSK(item, GUI.MODIFIABLE, true)) {
//                String s = (String) NSK.getNSK(item, ITEM_UUID);
//                if (s != null) {
//                    UUID id = UUID.fromString(s);
//                    DA_item dai = DA_item.get(id);
//                    if (dai != null)
//                        dai.manage(event.getPlayer());
//                }
//                event.getBlockPlaced().getBlockData();
//            }
//        }
        Dungeon d = Dungeon.get(event.getBlock().getLocation());
        if (d != null) {
            DungeonMaster dm = DungeonMaster.getOrNew(event.getPlayer());
            if (!dm.inBuildMode() || d != dm.getCurrentDungeon())
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Dungeon d = Dungeon.get(event.getBlock().getLocation());
        if (d != null) {
            DungeonMaster dm = DungeonMaster.getOrNew(event.getPlayer());
            if (!dm.inBuildMode() || d != dm.getCurrentDungeon())
                event.setCancelled(true);
        }
    }
}