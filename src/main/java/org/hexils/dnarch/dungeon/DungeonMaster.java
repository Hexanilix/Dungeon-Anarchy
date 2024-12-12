package org.hexils.dnarch.dungeon;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.hetils.jgl17.Pair;
import org.hetils.mpdl.PluginThread;
import org.hexils.dnarch.DA_item;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DungeonMaster {
    public static final Collection<DungeonMaster> dms = new HashSet<>();
    public static @NotNull DungeonMaster getOrNew(Player p) {
        for (DungeonMaster m : dms)
            if (m.p == p)
                return m;
        return new DungeonMaster(p);
    }


    private final Pair<Location, Location> selectedArea = new Pair<>();
    private final Pair<Location, Location> rawSelectedArea = new Pair<>();
    private final List<Block> slb = new ArrayList<>();
    private double ppm = 5;
    private SelectThread selectThread = new SelectThread();
    private Dungeon current_dungeon = null;
    private boolean build_mode = false;
    public final Player p;

    public DungeonMaster(Player p) {
        this.p = p;
        dms.add(this);
        this.selectThread.start();
    }

    public boolean setSelectionA(Block b) {
        Location key = b != null ? b.getLocation() : null;
        if (key != null && key.equals(rawSelectedArea.key()))
            return false;
        else {
            rawSelectedArea.setKey(key);
            if (key != null && rawSelectedArea.value() != null && key.getWorld() != rawSelectedArea.value().getWorld())
                rawSelectedArea.setValue(null);
            selectedArea.set(org.hetils.mpdl.LocationUtil.toMaxMin(rawSelectedArea));
            return true;
        }
    }
    public boolean setSelectionB(Block b) {
        Location value = b != null ? b.getLocation() : null;
        if (value != null && value.equals(rawSelectedArea.value()))
            return false;
        else {
            rawSelectedArea.setValue(value);
            if (value != null && rawSelectedArea.key() != null && value.getWorld() != rawSelectedArea.key().getWorld())
                rawSelectedArea.setKey(null);
            selectedArea.set(org.hetils.mpdl.LocationUtil.toMaxMin(rawSelectedArea));
            return true;
        }
    }
    public Location getSelectionA() {
        return getSelectedArea().key();
    }
    public Location getSelectionB() {
        return getSelectedArea().value();
    }
    public void clearSelectionA() {
        rawSelectedArea.setKey(null);
        selectedArea.set(org.hetils.mpdl.LocationUtil.toMaxMin(rawSelectedArea));
    }
    public void clearSelectionB() {
        rawSelectedArea.setValue(null);
        selectedArea.set(org.hetils.mpdl.LocationUtil.toMaxMin(rawSelectedArea));
    }
    public Pair<Location, Location> getSelectedArea() {
        return new Pair<>(selectedArea.key() != null ? selectedArea.key().clone().add(1, 1, 1) : null, selectedArea.value());
    }
    public void clearSelection() {
        this.rawSelectedArea.setKey(null);
        this.rawSelectedArea.setValue(null);
        this.selectedArea.setKey(null);
        this.selectedArea.setValue(null);
    }
    public boolean hasAreaSelected() {
        return selectedArea.key() != null && selectedArea.value() != null;
    }

    public List<Block> getSelectedBlocks() {
        List<Block> l = new ArrayList<>();
        l.addAll(slb);
        l.addAll(getSelectionBlocks());
        return l;
    }
    public boolean selectBlock(Block b) {
        if (this.isEditing()) {
            Dungeon d = this.current_dungeon;
            if (!slb.contains(b)) {
                if (!d.isWithinDungeon(b.getLocation())) {
                    p.sendMessage("Yo bblocks out of bounds!");
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
    public boolean deselectBlock(Block b) {
        return slb.remove(b);
    }
    public void deselectBlocks() {
        this.slb.clear();
        this.clearSelection();
    }

    public List<Block> getSelectionBlocks() {
        List<Block> c = new ArrayList<>();
        Pair<Location, Location> area = getSelectedArea();
        if (area.key() != null && area.value() != null) {
            World w = area.key().getWorld();
            int xm = (int) Math.max(area.key().getX(), area.value().getX());
            int ym = (int) Math.max(area.key().getY(), area.value().getY());
            int zm = (int) Math.max(area.key().getZ(), area.value().getZ());
            int mx = (int) Math.min(area.key().getX(), area.value().getX());
            int my = (int) Math.min(area.key().getY(), area.value().getY());
            int mz = (int) Math.min(area.key().getZ(), area.value().getZ());
            for (int x = mx; x <= xm; x++)
                for (int y = my; y <= ym; y++)
                    for (int z = mz; z <= zm; z++) {
                        Block b = w.getBlockAt(x, y, z);
                        if (current_dungeon.isWithinDungeon(b.getLocation())) c.add(b);
                    }
        }
        return c;
    }

    public Dungeon getCurrentDungeon() {
        return current_dungeon;
    }
    public void setCurrentDungeon(Dungeon current_dungeon) {
        this.current_dungeon = current_dungeon;
    }
    public boolean isEditing() {
        return current_dungeon != null;
    }

    public void give(DA_item a) {
        if (a != null) this.p.getInventory().addItem(a.getItem());
    }

    public boolean inBuildMode() {
        return build_mode && current_dungeon != null;
    }

    public boolean hasBlocksSelected() {
        return !slb.isEmpty();
    }

    public void setBuildMode(boolean b) {
        this.build_mode = b;
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
                    slb.forEach(b -> selectBlock(b.getLocation(), 5));
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
    public void selectBlock(@NotNull Location l, int ppm) {
        double x = (int) l.getX();
        double y = (int) l.getY();
        double z = (int) l.getZ();
        p.spawnParticle(Particle.COMPOSTER, x+1, y+1, z+1, 1, 0, 0, 0, 0);
        for (double i = 0; i < 1; i+= 1d/ppm) {
            p.spawnParticle(Particle.COMPOSTER, x+i, y, z, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+i, y+1, z, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+i, y, z+1, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+i, y+1, z+1, 1, 0, 0, 0, 0);

            p.spawnParticle(Particle.COMPOSTER, x, y, z+i, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x, y+1, z+i, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+1, y, z+i, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+1, y+1, z+i, 1, 0, 0, 0, 0);

            p.spawnParticle(Particle.COMPOSTER, x, y+i, z, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x, y+i, z+1, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+1, y+i, z, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+1, y+i, z+1, 1, 0, 0, 0, 0);
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
                        p.spawnParticle(par, mx + i, my, mz, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + i, my, mz + zd, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + i, my + yd, mz + zd, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + i, my + yd, mz, 1, 0, 0, 0, 0);
                    }
                    for (double i = 0; i < yd; i += ppmc) {
                        p.spawnParticle(par, mx, my + i, mz, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + xd, my + i, mz, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + xd, my + i, mz + zd, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx, my + i, mz + zd, 1, 0, 0, 0, 0);
                    }
                    for (double i = 0; i < zd; i += ppmc) {
                        p.spawnParticle(par, mx, my, mz + i, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + xd, my, mz + i, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + xd, my + yd, mz + i, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx, my + yd, mz + i, 1, 0, 0, 0, 0);
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

        public void setPar(Particle par) {
            this.par = par;
        }
    }
    public void select(Object o, Pair<Location, Location> sec) {
        this.selectThread.addSelect(o, new SectionSelect(sec));
    }
    public void select(Object o, Pair<Location, Location> sec, Particle p) {
        this.selectThread.addSelect(o, new SectionSelect(sec, p));
    }
    public boolean hideSelection(Object o) { return selectThread.clearSelection(o); }
    public void hideSelections() { selectThread.clearSelected(); }
}
