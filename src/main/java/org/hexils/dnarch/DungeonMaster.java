package org.hexils.dnarch;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.hetils.jgl17.Pair;
import org.hetils.jgl17.oodp.OODP;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.WrappedPlayer;
import org.hetils.mpdl.item.NSK;
import org.hetils.mpdl.plugin.PluginThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.hetils.mpdl.item.ItemUtil.newItemStack;

public class DungeonMaster extends WrappedPlayer {

    public static final Set<DungeonMaster> dms = new HashSet<>();
    public static @Nullable DungeonMaster get(CommandSender sender) {
        if (sender instanceof Player p) {
            if (WrappedPlayer.get(p) instanceof DungeonMaster dm) return dm;
            else return FileManager.loadMaster(p);
        } else return null;
    }
    public static @NotNull DungeonMaster get(@NotNull Player p) {
        if (WrappedPlayer.get(p) instanceof DungeonMaster dm) return dm;
        else return FileManager.loadMaster(p);
    }

    public static Set<UUID> permittedPlayers = new HashSet<>();


    @OODPExclude
    private final Pair<Location, Location> selected_area = new Pair<>();
    @OODPExclude
    private final Pair<Location, Location> raw_selected_area = new Pair<>();
    @OODPExclude
    private final List<Block> slb = new ArrayList<>();
    @OODPExclude
    private SelectThread select_thread = new SelectThread();
    @OODPExclude
    private Dungeon current_dungeon = null;
    @OODPExclude
    private boolean build_mode = false;

    private double ppm = 5;

    DungeonMaster(Player p) { this(p, null); }
    DungeonMaster(Player p, OODP.ObjectiveMap conf) {
        super(p);
        this.select_thread.start();
        if (conf != null) {
            ppm = conf.getInt("particles_per_block");
        }
        dms.add(this);
    }

    public static boolean isPermitted(@NotNull Player p) { return p.isOp() || isPermitted(p.getUniqueId()); }
    public static boolean isPermitted(UUID id) { return permittedPlayers.contains(id); }

    public boolean setSelectionA(Block b) {
        Location key = b != null ? b.getLocation() : null;
        if (key != null && key.equals(raw_selected_area.key()))
            return false;
        else {
            raw_selected_area.setKey(key);
            if (key != null && raw_selected_area.value() != null && key.getWorld() != raw_selected_area.value().getWorld())
                raw_selected_area.setValue(null);
            selected_area.set(org.hetils.mpdl.location.LocationUtil.toMaxMin(raw_selected_area));
            return true;
        }
    }
    public boolean setSelectionB(Block b) {
        Location value = b != null ? b.getLocation() : null;
        if (value != null && value.equals(raw_selected_area.value()))
            return false;
        else {
            raw_selected_area.setValue(value);
            if (value != null && raw_selected_area.key() != null && value.getWorld() != raw_selected_area.key().getWorld())
                raw_selected_area.setKey(null);
            selected_area.set(org.hetils.mpdl.location.LocationUtil.toMaxMin(raw_selected_area));
            return true;
        }
    }
    public Location getSelectionA() { return getSelectedArea().key(); }
    public Location getSelectionB() { return getSelectedArea().value(); }
    public void clearSelectionA() {
        raw_selected_area.setKey(null);
        selected_area.set(org.hetils.mpdl.location.LocationUtil.toMaxMin(raw_selected_area));
    }
    public void clearSelectionB() {
        raw_selected_area.setValue(null);
        selected_area.set(org.hetils.mpdl.location.LocationUtil.toMaxMin(raw_selected_area));
    }
    public Pair<Location, Location> getSelectedArea() { return new Pair<>(selected_area.key() != null ? selected_area.key().clone().add(1, 1, 1) : null, selected_area.value()); }
    public void clearSelection() {
        this.raw_selected_area.setKey(null);
        this.raw_selected_area.setValue(null);
        this.selected_area.setKey(null);
        this.selected_area.setValue(null);
    }
    public boolean hasAreaSelected() { return selected_area.key() != null && selected_area.value() != null; }

    public List<Block> getSelectedBlocks() {
        List<Block> l = new ArrayList<>();
        l.addAll(slb);
        l.addAll(getSelectionBlocks());
        return l;
    }
    public boolean selectBlock(Block b) {
        if (this.isEditing()) {
            if (!slb.contains(b)) {
                if (!this.current_dungeon.contains(b.getLocation())) {
                    sendMessage(W + "Block is out of dungeon bounds!");
                } else {
                    if (!slb.isEmpty() && slb.get(0).getWorld() != b.getWorld())
                        slb.clear();
                    slb.add(b);
                    return true;
                }
            }
        }
        return false;
    }
    public boolean deselectBlock(Block b) { return slb.remove(b); }
    public void deselectBlocks() {
        this.slb.clear();
        this.clearSelection();
    }

    public List<Block> getSelectionBlocks() {
        List<Block> c = new ArrayList<>();
        if (raw_selected_area.key() != null && raw_selected_area.value() != null) {
            World w = raw_selected_area.key().getWorld();
            int xm = (int) Math.max(raw_selected_area.key().getX(), raw_selected_area.value().getX());
            int ym = (int) Math.max(raw_selected_area.key().getY(), raw_selected_area.value().getY());
            int zm = (int) Math.max(raw_selected_area.key().getZ(), raw_selected_area.value().getZ());
            int mx = (int) Math.min(raw_selected_area.key().getX(), raw_selected_area.value().getX());
            int my = (int) Math.min(raw_selected_area.key().getY(), raw_selected_area.value().getY());
            int mz = (int) Math.min(raw_selected_area.key().getZ(), raw_selected_area.value().getZ());
            for (int x = mx; x <= xm; x++)
                for (int y = my; y <= ym; y++)
                    for (int z = mz; z <= zm; z++) {
                        Block b = w.getBlockAt(x, y, z);
                        if (current_dungeon.contains(b.getLocation())) c.add(b);
                    }
        }
        return c;
    }

    public boolean isEditing() { return current_dungeon != null; }
    public void editDungeon(Dungeon dungeon) {
        if (this.current_dungeon != dungeon) {
            this.current_dungeon = dungeon;
            if (this.current_dungeon != null) {
                dungeon.addEditor(this);
                this.sendMessage(DungeonMaster.Sender.CREATOR, OK + "Editing dungeon " + current_dungeon.getName());
            }
        }
    }
    public Dungeon getCurrentDungeon() { return current_dungeon; }

    public void giveItem(DAItem a) { if (a != null) this.getInventory().addItem(a.getItem()); }

    public boolean inBuildMode() { return build_mode && current_dungeon != null; }
    public void setBuildMode(boolean b) { this.build_mode = b; }

    public boolean hasBlocksSelected() { return !slb.isEmpty(); }

    public void clearOfDeletedDAItems() { DAItem.updateItems(this.getInventory()); }

    public double getPPM() {
        return ppm;
    }

    public Dungeon.Section getSection() {
        if (isEditing()) return current_dungeon.getSection(this);
        else return null;
    }

    private final class SelectThread extends PluginThread {
        private final Map<Object, SectionSelect> selects = new HashMap<>();
        public void addSelect(Object o, SectionSelect select) {
            SectionSelect sl = selects.get(o);
            if (sl != null) sl.halt();
            selects.put(o, select);
            select.start();
        }
        public boolean clearSelection(Object o) {
            SectionSelect sl = selects.get(o);
            if (sl != null)
                if (sl.isRunning()) {
                    sl.halt();
                    return true;
                }
            return false;
        }
        public void clearSelected() {
            selects.values().forEach(PluginThread::halt);
            selects.clear();
        }

        private Particle msp = Particle.COMPOSTER;
        SectionSelect secs = new SectionSelect(getSelectedArea(), msp);
        Location lk = getSelectedArea().key();
        Location lv = getSelectedArea().value();
        @Override
        public void run() {
            secs.start();
            try {
                while (r) {
                    slb.forEach(b -> selectBlock(b.getLocation()));
                    Thread.sleep(100);
                    Pair<Location, Location> area = getSelectedArea();
                    if (area.key() != lk || area.value() != lv) {
                        lk = area.key();
                        lv = area.value();
                        secs.setSec(area);
                    }
                }
            } catch (InterruptedException ignore) {}
        }
    }
    public void selectBlock(@NotNull Location l) {
        double x = (int) l.getX();
        double y = (int) l.getY();
        double z = (int) l.getZ();
        spawnParticle(Particle.COMPOSTER, x+1, y+1, z+1, 1, 0, 0, 0, 0);
        for (double i = 0; i < 1; i+= 1d/ppm) {
            spawnParticle(Particle.COMPOSTER, x+i, y, z, 1, 0, 0, 0, 0);
            spawnParticle(Particle.COMPOSTER, x+i, y+1, z, 1, 0, 0, 0, 0);
            spawnParticle(Particle.COMPOSTER, x+i, y, z+1, 1, 0, 0, 0, 0);
            spawnParticle(Particle.COMPOSTER, x+i, y+1, z+1, 1, 0, 0, 0, 0);

            spawnParticle(Particle.COMPOSTER, x, y, z+i, 1, 0, 0, 0, 0);
            spawnParticle(Particle.COMPOSTER, x, y+1, z+i, 1, 0, 0, 0, 0);
            spawnParticle(Particle.COMPOSTER, x+1, y, z+i, 1, 0, 0, 0, 0);
            spawnParticle(Particle.COMPOSTER, x+1, y+1, z+i, 1, 0, 0, 0, 0);

            spawnParticle(Particle.COMPOSTER, x, y+i, z, 1, 0, 0, 0, 0);
            spawnParticle(Particle.COMPOSTER, x, y+i, z+1, 1, 0, 0, 0, 0);
            spawnParticle(Particle.COMPOSTER, x+1, y+i, z, 1, 0, 0, 0, 0);
            spawnParticle(Particle.COMPOSTER, x+1, y+i, z+1, 1, 0, 0, 0, 0);
        }
    }

    private class SectionSelect extends PluginThread {
        private Particle par;
        public SectionSelect(Pair<Location, Location> sec) {
            setSec(sec);
            this.par = Particle.COMPOSTER;
        }
        public SectionSelect(Pair<Location, Location> sec, Particle par) {
            setSec(sec);
            this.par = par;
        }
        double xm = 0;
        double ym = 0;
        double zm = 0;
        double mx = 0;
        double my = 0;
        double mz = 0;
        double xd = 0;
        double yd = 0;
        double zd = 0;
        @Override
        public void run() {
            try {
                while (r) {
                    double ppmc = 1d / ppm;
                    for (double i = 0; i < xd; i += ppmc) {
                        spawnParticle(par, mx + i, my, mz, 1, 0, 0, 0, 0);
                        spawnParticle(par, mx + i, my, mz + zd, 1, 0, 0, 0, 0);
                        spawnParticle(par, mx + i, my + yd, mz + zd, 1, 0, 0, 0, 0);
                        spawnParticle(par, mx + i, my + yd, mz, 1, 0, 0, 0, 0);
                    }
                    for (double i = 0; i < yd; i += ppmc) {
                        spawnParticle(par, mx, my + i, mz, 1, 0, 0, 0, 0);
                        spawnParticle(par, mx + xd, my + i, mz, 1, 0, 0, 0, 0);
                        spawnParticle(par, mx + xd, my + i, mz + zd, 1, 0, 0, 0, 0);
                        spawnParticle(par, mx, my + i, mz + zd, 1, 0, 0, 0, 0);
                    }
                    for (double i = 0; i < zd; i += ppmc) {
                        spawnParticle(par, mx, my, mz + i, 1, 0, 0, 0, 0);
                        spawnParticle(par, mx + xd, my, mz + i, 1, 0, 0, 0, 0);
                        spawnParticle(par, mx + xd, my + yd, mz + i, 1, 0, 0, 0, 0);
                        spawnParticle(par, mx, my + yd, mz + i, 1, 0, 0, 0, 0);
                    }
                    Thread.sleep(100);
                }
            } catch (InterruptedException ignore) {}
        }

        public void setSec(Pair<Location, Location> sec) {
            if (sec != null && sec.key() != null && sec.value() != null) {
                xm = Math.max(sec.key().getX(), sec.value().getX());
                ym = Math.max(sec.key().getY(), sec.value().getY());
                zm = Math.max(sec.key().getZ(), sec.value().getZ());
                mx = Math.min(sec.key().getX(), sec.value().getX());
                my = Math.min(sec.key().getY(), sec.value().getY());
                mz = Math.min(sec.key().getZ(), sec.value().getZ());
                xd = xm - mx;
                yd = ym - my;
                zd = zm - mz;
            } else {
                xm = 0;
                ym = 0;
                zm = 0;
                mx = 0;
                my = 0;
                mz = 0;
                xd = 0;
                yd = 0;
                zd = 0;
            }
        }

        public void setPar(Particle par) { this.par = par; }
    }
    public void select(Object o, Pair<Location, Location> sec) { this.select_thread.addSelect(o, new SectionSelect(sec)); }
    public void select(Object o, Pair<Location, Location> sec, Particle p) { this.select_thread.addSelect(o, new SectionSelect(sec, p)); }
    public boolean hideSelection(Object o) { return select_thread.clearSelection(o); }
    public void hideSelections() { select_thread.clearSelected(); }

    public void promptRemove(DAManageable m) {;
        manage(new DAManageable() {

            @Override
            protected void createGUI() {
                setSize(27);
                setItem(4, newItemStack(Material.PAPER, "Are you sure you want to remove:"));
                ItemStack di;
                if (m instanceof DAItem da) di = da.getItem();
                else di = newItemStack(Material.GREEN_CONCRETE, m.getName());
                setItem(13, di);
                setAction(11, newItemStack(Material.GREEN_CONCRETE, ChatColor.GREEN + "Yes"), "delete");
                setAction(15, newItemStack(Material.RED_CONCRETE, ChatColor.RED + "No"), "cancel");
            }

            private boolean deleted = false;

            @Override
            public boolean guiClickEvent(InventoryClickEvent event) {
                String v = NSK.getNSK(event.getCurrentItem(), ITEM_ACTION_NSK);
                if (v != null) {
                    if (v.equals("delete")) {
                        if (DAItem.get(DungeonMaster.this.getInventory().getItemInMainHand()) == m)
                            DungeonMaster.this.getInventory().setItemInMainHand(null);
                        m.delete();
                        deleted = true;
                        DungeonMaster.this.sendInfo(DungeonMaster.Sender.CREATOR, "Deleted " + m.getName());
                        DungeonMaster.this.closeInventory();
                    } else if (v.equals("cancel")) DungeonMaster.this.closeInventory();
                }
                return false;
            }

            @Override
            public void onInvClose() {
                if (!deleted) DungeonMaster.this.sendWarning(DungeonMaster.Sender.CREATOR, "Cancelled.");
                super.delete();
            }
        });
    }

    public static ChatColor IF = ChatColor.AQUA;
    public static ChatColor OK = ChatColor.GREEN;
    public static ChatColor W = ChatColor.YELLOW;
    public static ChatColor ER = ChatColor.RED;

    public enum Sender {
        CREATOR(ChatColor.GOLD),
        DEBUG(ChatColor.LIGHT_PURPLE);

        private final ChatColor color;
        Sender(ChatColor c) { color = c; }

        @Override
        public @NotNull String toString() { return this.name().charAt(0) + this.name().toLowerCase().substring(1); }
    }

    public void sendMessage(@NotNull Sender s, String message) { super.sendMessage(s.color, s.toString(), message); }
    public void sendInfo(@NotNull Sender s, @NotNull String message) { super.sendMessage(s.color, s.toString(), IF + message); }
    public void sendPass(@NotNull Sender s, @NotNull String message) { sendMessage(s.color, s.toString(), OK + message); }
    public void sendWarning(@NotNull Sender s, @NotNull String message) { super.sendMessage(s.color, s.toString(), W + message); }
    public void sendError(@NotNull Sender s, @NotNull String message) { super.sendMessage(s.color, s.toString(), ER + message); }

}

