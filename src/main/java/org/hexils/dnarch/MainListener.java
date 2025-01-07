package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.hetils.mpdl.LocationUtil;
import org.hetils.mpdl.NSK;
import org.hetils.mpdl.VectorUtil;
import org.hexils.dnarch.items.actions.entity.EntitySpawnAction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


import static org.hexils.dnarch.Main.*;
import static org.hexils.dnarch.DAItem.ITEM_UUID;
import static org.hexils.dnarch.Manageable.*;
import static org.hexils.dnarch.Manageable.ItemListGUI.ITEM_LIST_GUI;

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
                DAItem di = DAItem.get(id);
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
                    if (DAItem.get(it) == mg)
                        return;
                    if (event.getAction() == InventoryAction.CLONE_STACK) {
                        DAItem da = DAItem.get(it);
                        if (da != null)
                            da.manage(dm, mg);
                    }
                    if (mg.guiClickEvent(event)) {
                        if (NSK.hasNSK(it, ITEM_RENAME)) {
                            Manageable m = Manageable.get(opi);
                            if (m == null) mg.rename(dm, () -> dm.openInventory(opi));
                            else mg.rename(dm, () -> mg.manage(dm));
                        }
                        if (NSK.hasNSK(it, ITEM_FIELD_VALUE)) mg.setField(dm, (String) NSK.getNSK(event.getCurrentItem(), ITEM_FIELD_VALUE));
                        if (NSK.hasNSK(it, ITEM_ACTION)) mg.doAction(dm, (String) NSK.getNSK(it, ITEM_ACTION), event);
                        if (NSK.hasNSK(it, ITEM_LIST_GUI)) ItemListGUI.get(UUID.fromString((String) NSK.getNSK(it, ITEM_LIST_GUI))).manage(dm, mg);
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
    public void onPlayerLeave(@NotNull PlayerQuitEvent event) {
        DungeonMaster dm = DungeonMaster.getOrNew(event.getPlayer());
        if (dm.isEditing()) {
            Dungeon d = dm.getCurrentDungeon();
            d.save();
            d.removeViewer(dm);
            d.removeEditor(dm);
            dm.setCurrentDungeon(null);
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
        for (EntitySpawnAction.EntityDeathCondition ed : EntitySpawnAction.EntityDeathCondition.instances)
            if (ed.getAction() != null && ed.getAction().getSpawnedEntities().contains(event.getEntity()))
                ed.onTrigger();
    }

    @EventHandler
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        DungeonMaster dm = DungeonMaster.getOrNew(event.getPlayer());
        if (dm.isOp()) return;
        for (Dungeon d : Dungeon.dungeons) {
            if (dm.getCurrentDungeon() != d && d.contains(dm.p)) {
                if (d.isClosed()) {
                    Location l;
                    Vector v = dm.getVelocity().normalize();
                    if (v.isZero()) {
                        l = LocationUtil.join(d.getWorld(), d.getBoundingBox().getCenter());
                        l.setY(d.getBoundingBox().getMaxY());
                        l.add(.5, .5, .5);
                        while (!l.getBlock().getType().isAir())
                            l.add(0, 1, 0);
                    }
                    else {
                        l = dm.getLocation();
                        Location cl = d.getSection(dm.p).getCenter();
                        cl.setY(l.getY());
                        v = VectorUtil.genVec(dm.p.getLocation(), cl);
                        while (d.contains(dm.p)) {
                            l.subtract(v);
                            dm.teleport(l);
                        }
                    }
                    dm.sendMessage(ChatColor.RED + "This Dungeon is currently closed!");
                } else d.addPlayer(dm.p);
            }
        }
    }
}