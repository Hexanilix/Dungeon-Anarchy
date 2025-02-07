package org.hexils.dnarch.items.actions;

import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.GeneralListener;
import org.hetils.mpdl.item.ItemUtil;
import org.hetils.mpdl.item.NSK;
import org.hetils.mpdl.location.LocationUtil;
import org.hexils.dnarch.Action;
import org.hexils.dnarch.DAItem;
import org.hexils.dnarch.DungeonMaster;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static org.hetils.jgl17.StringUtil.readableEnum;
import static org.hetils.mpdl.GeneralUtil.runTaskTimer;
import static org.hetils.mpdl.MPDL.*;
import static org.hetils.mpdl.item.ItemUtil.newItemStack;

public class SpawnParticle extends Action {

    static { setTabComplete(args -> args.length == 1 ? Arrays.stream(Particle.values()).map(e -> e.name().toLowerCase()).toList() : null); }

    private Particle particle = Particle.COMPOSTER;
    private Location center = null;
    private int amount = 1;
    private double offsetX = 0;
    private double offsetY = 0;
    private double offsetZ = 0;
    private double inertia = 0;
    @OODPExclude
    private DungeonMaster current_editor = null;
    @OODPExclude
    private final GeneralListener.LiveEditor editor = new GeneralListener.LiveEditor();
    {
        editor.action(3, Material.FIREWORK_STAR, "Amount", a -> amount = Math.max(1, amount +((current_editor.isSneaking() ? 1 : 2) * (a.name().contains("LEFT") ? -1 : 1))));
        editor.action(4, Material.RED_CONCRETE, "Offset X", a -> offsetX = Math.max(0, offsetX+((current_editor.isSneaking() ? .1 : .2) * (a.name().contains("LEFT") ? -1 : 1))));
        editor.action(5, Material.GREEN_CONCRETE, "Offset Y", a -> offsetY = Math.max(0, offsetX+((current_editor.isSneaking() ? .1 : .2) * (a.name().contains("LEFT") ? -1 : 1))));
        editor.action(6, Material.BLUE_CONCRETE, "Offset Z", a -> offsetZ = Math.max(0, offsetX+((current_editor.isSneaking() ? .1 : .2) * (a.name().contains("LEFT") ? -1 : 1))));
        editor.action(7, Material.ARROW, "Inertia", a -> inertia = Math.max(0, offsetX+((current_editor.isSneaking() ? .1 : .2) * (a.name().contains("LEFT") ? -1 : 1))));
    }

    public SpawnParticle() { super(Type.SPAWN_PARTICLE); }
    public SpawnParticle(Particle p) {
        super(Type.SPAWN_PARTICLE);
        this.particle = p;
    }

    public void setParticle(Particle particle) { this.particle = particle; }

    @Override
    public void trigger() {
        if (center != null) {
            World w = center.getWorld();
            if (w != null) {
                center.getWorld().spawnParticle(particle, center, amount, offsetX, offsetY, offsetZ, inertia);
            }
        }
    }

    @Override
    protected void resetAction() {}

    @Override
    protected void createGUI() {
        this.setAction(12, newItemStack(Material.GREEN_CONCRETE, "Location: ", List.of(LocationUtil.toReadableFormat(center))), "location");
        this.setAction(13, newItemStack(Material.BOOKSHELF, "Edit"), "edit");
        this.setAction(14, newItemStack(Material.FIREWORK_ROCKET, "Particle: " + readableEnum(particle)), "particle");
        this.setAction(20, newItemStack(Material.FIREWORK_STAR, "Amount: "), "amount");
        this.setAction(21, newItemStack(Material.RED_CONCRETE, "Offset X: "), "offsetX");
        this.setAction(22, newItemStack(Material.GREEN_CONCRETE, "Offset Y: "), "offsetY");
        this.setAction(23, newItemStack(Material.BLUE_CONCRETE, "Offset Z: "), "offsetZ");
        this.setAction(24, newItemStack(Material.ARROW, "Inertia: "), "inertia");
        updateFields();
    }

    @Override
    protected void action(DungeonMaster dm, @NotNull String action, String[] args, @NotNull ClickType click) {
        double v = (click.isLeftClick() ? -.1 : .1) * (click.isShiftClick() ? 1 : 2);
        switch (action) {
            case "edit" -> {
                if (current_editor == null) {
                    current_editor = dm;
                    dm.holdManagement(true);
                    dm.sendMessage("Editing " + getName());
                    dm.getInventory().setHeldItemSlot(3);

                    final BukkitTask runnable = runTaskTimer(()->{
//                        if (move) upl();
                        dm.spawnParticle(particle, center, amount, offsetX, offsetY, offsetZ, inertia);

                        double x = center.getX();
                        double y = center.getY();
                        double z = center.getZ();

                        Particle part = Particle.CRIT;
                        double minX = x - offsetX*2;
                        double maxX = x + offsetX*2;
                        double minY = y - offsetY*2;
                        double maxY = y + offsetY*2;
                        double minZ = z - offsetZ*2;
                        double maxZ = z + offsetZ*2;

                        double step = 1.0 / dm.getPPM();
                        for (double i = minX; i <= maxX; i += step) {
                            dm.spawnParticle(part, i, minY, minZ, 1, 0, 0, 0, 0);
                            dm.spawnParticle(part, i, minY, maxZ, 1, 0, 0, 0, 0);
                            dm.spawnParticle(part, i, maxY, minZ, 1, 0, 0, 0, 0);
                            dm.spawnParticle(part, i, maxY, maxZ, 1, 0, 0, 0, 0);
                        }
                        for (double i = minY; i <= maxY; i += step) {
                            dm.spawnParticle(part, minX, i, minZ, 1, 0, 0, 0, 0);
                            dm.spawnParticle(part, minX, i, maxZ, 1, 0, 0, 0, 0);
                            dm.spawnParticle(part, maxX, i, minZ, 1, 0, 0, 0, 0);
                            dm.spawnParticle(part, maxX, i, maxZ, 1, 0, 0, 0, 0);
                        }
                        for (double i = minZ; i <= maxZ; i += step) {
                            dm.spawnParticle(part, minX, minY, i, 1, 0, 0, 0, 0);
                            dm.spawnParticle(part, minX, maxY, i, 1, 0, 0, 0, 0);
                            dm.spawnParticle(part, maxX, minY, i, 1, 0, 0, 0, 0);
                            dm.spawnParticle(part, maxX, maxY, i, 1, 0, 0, 0, 0);
                        }
                    }, 0, 2);

                    editor.action(2, Material.GREEN_CONCRETE, "Position", a -> GeneralListener.selectLocation(dm, (l)->center=l));

                    editor.edit(dm, () -> {
                        runnable.cancel();
                        current_editor.holdManagement(false);
                        current_editor = null;
                    });
                } else dm.sendWarning(DungeonMaster.Sender.CREATOR, getName() + " is currently being edited by " + current_editor.getDisplayName());

            }
            case "amount" -> amount = (int) Math.max(1, (v*10));
            case "offsetX" -> offsetX = Math.max(0, offsetX+v);
            case "offsetY" -> offsetY = Math.max(0, offsetY+v);
            case "offsetZ" -> offsetZ = Math.max(0, offsetZ+v);
            case "inertia" -> inertia = Math.max(0, inertia+v);
            case "particle" -> {
                dm.holdManagement(true);
                GeneralListener.promptPlayer(dm, null, r -> {
                    if (r != null) {
                        try {
                            SpawnParticle.this.particle = Particle.valueOf(r.toUpperCase());
                        } catch (IllegalArgumentException ignore) {}
                    }
                }, () -> dm.holdManagement(false));
            }
            case "location" -> {
                dm.holdManagement(true);
                dm.sendInfo(DungeonMaster.Sender.CREATOR, "Select a new location for " + getName());
                GeneralListener.selectLocation(dm, l -> {
                    center = l;
                    dm.spawnParticle(particle, center, amount, offsetX, offsetY, offsetZ, inertia);
                } ,l -> dm.holdManagement(false));
            }
        }
        updateFields();
    }

    @Override
    protected void updateGUI() {
        super.updateGUI();
    }

    private void itemSet(int i, List<String> lore, int amnt) {
        ItemUtil.setLore(this.getItem(i), lore);
        this.getItem(i).setAmount(Math.max(Math.min(64, amnt), 1));
    }

    private void updateFields() {
        itemSet(20, List.of(ChatColor.GREEN + "" + amount), amount);
        itemSet(21, List.of(ChatColor.GREEN + "" + (Math.round(offsetX*10)/10d)), (int) Math.round(offsetX*10));
        itemSet(22, List.of(ChatColor.GREEN + "" + (Math.round(offsetY*10)/10d)), (int) Math.round(offsetY*10));
        itemSet(23, List.of(ChatColor.GREEN + "" + (Math.round(offsetZ*10)/10d)), (int) Math.round(offsetZ*10));
        itemSet(24, List.of(ChatColor.GREEN + "" + (Math.round(inertia*10)/10d)), (int) Math.round(inertia*10));
    }

    @Override
    protected ItemStack genItemStack() {
        return new ItemStack(Material.CANDLE);
    }



    @Override
    public DAItem create(@NotNull DungeonMaster dm, String @NotNull [] args) {
        this.center = dm.getLocation();
        if (args.length > 0) this.particle = Particle.valueOf(args[0].toUpperCase());
        return this;
    }
}
