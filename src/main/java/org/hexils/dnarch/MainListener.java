package org.hexils.dnarch;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.dungeon.Dungeon;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.items.conditions.EntityDeath;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


import static org.hexils.dnarch.Main.*;
import static org.hexils.dnarch.DA_item.ITEM_UUID;
import static org.hexils.dnarch.Manageable.*;

public final class MainListener implements org.bukkit.event.Listener {
    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        DungeonMaster dm = DungeonMaster.getOrNew(p);
        if (item.isSimilar(wand)) {
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
                    di.manage(dm);
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
            DungeonMaster dm = DungeonMaster.getOrNew(p);
            for (Manageable mg : Manageable.instances)
                if (mg.isThisGUI(opi)) {
                    event.setCancelled(true);
                    ItemStack it = event.getCurrentItem();
                    if (DA_item.get(it) == mg)
                        return;
                    if (mg.guiClickEvent(event)) {
                        if (event.getAction() == InventoryAction.CLONE_STACK) {
                            DA_item da = DA_item.get(it);
                            if (da != null)
                                da.manage(dm, mg);
                        } else if (NSK.hasNSK(it, ITEM_RENAME)) {
                            Manageable m = Manageable.get(opi);
                            if (m == null) mg.rename(dm, () -> dm.openInventory(opi));
                            else mg.rename(dm, () -> mg.manage(dm));
                        } else if (NSK.hasNSK(it, ITEM_FIELD_VALUE)) {
                            mg.setField(dm, (String) NSK.getNSK(event.getCurrentItem(), ITEM_FIELD_VALUE));
                        } else if (NSK.hasNSK(it, ITEM_ACTION)) {
                            mg.doAction(dm, (String) NSK.getNSK(it, ITEM_ACTION), event);
                        }
                    }
                }
        }
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
//        ItemStack item = event.getItemInHand();
//        if (NSK.hasNSK(item, DA_block.BLOCK, true)) {
//            if (NSK.hasNSK(item, MODIFIABLE, true)) {
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

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        for (EntityDeath ed : EntityDeath.instances)
            if (ed.getEc().getEntities().contains(event.getEntity()))
                ed.trigger();
    }
}