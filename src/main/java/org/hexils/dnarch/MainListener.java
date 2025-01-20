package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
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
        Action ac = event.getAction();
        if (item.isSimilar(wand)) {
            event.setCancelled(true);
            Block b = event.getClickedBlock();
            switch (ac) {
                case LEFT_CLICK_BLOCK -> {
                    if (p.isSneaking()) {
                        if (b == null) return;
                        if (!dm.setSelectionA(b))
                            dm.clearSelectionA();
                    }
                }
                case RIGHT_CLICK_BLOCK -> {
                    if (p.isSneaking()) {
                        if (b == null) return;
                        if (!dm.setSelectionB(b))
                            dm.clearSelectionB();
                    } else if (dm.isEditing() && !dm.selectBlock(b))
                            dm.deselectBlock(b);
                }
            }
        } else {
            DAItem da = DAItem.get(item);
            if (da != null) {
                if (p.isSneaking()) {
                    if ((ac == Action.LEFT_CLICK_AIR || ac == Action.LEFT_CLICK_BLOCK) && da instanceof Triggerable tr) {
                        tr.trigger();
                        dm.sendInfo(DungeonMaster.Sender.CREATOR, "Running " + da.getName());
                        return;
                    } else if ((ac == Action.RIGHT_CLICK_AIR || ac == Action.RIGHT_CLICK_BLOCK) && da instanceof Resetable re) {
                        re.reset();
                        dm.sendInfo(DungeonMaster.Sender.CREATOR, "Reseting " + da.getName());
                        return;
                    }
                }
                String s = (String) NSK.getNSK(event.getItem(), ITEM_UUID);
                if (s != null) {
                    DAItem di = DAItem.get(UUID.fromString(s));
                    if (di != null) {
                        di.manage(dm);
                        event.setCancelled(true);
                    }
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