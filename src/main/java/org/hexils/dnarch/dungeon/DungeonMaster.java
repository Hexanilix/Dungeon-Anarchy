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

import static org.hetils.mpdl.General.log;

public class DungeonMaster {
    public static final Collection<DungeonMaster> dms = new HashSet<>();
    public static @NotNull DungeonMaster getOrNew(Player p) {
        for (DungeonMaster m : dms)
            if (m.p == p)
                return m;
        return new DungeonMaster(p);
    }


    private final Pair<Location, Location> selectedArea = new Pair<>();
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

    public boolean setSelectionA(Location key) {
        if (key != null && key.equals(selectedArea.key()))
            return false;
        else {
            selectedArea.setKey(key);
            if (key != null && selectedArea.value() != null && key.getWorld() != selectedArea.value().getWorld())
                selectedArea.setValue(null);
            return true;
        }
    }
    public boolean setSelectionB(Location value) {
        if (value != null && value.equals(selectedArea.value()))
            return false;
        else {
            selectedArea.setValue(value);
            if (value != null && selectedArea.key() != null && value.getWorld() != selectedArea.key().getWorld())
                selectedArea.setKey(null);
            return true;
        }
    }
    public Location getSelectionA() {
        return selectedArea.key();
    }
    public Location getSelectionB() {
        return selectedArea.value();
    }
    public void clearSelectionA() {
        selectedArea.setKey(null);
    }
    public void clearSelectionB() {
        selectedArea.setValue(null);
    }
    public Pair<Location, Location> getSelectedArea() {
        return selectedArea;
    }
    public void clearSelection() {
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
        log(selectedArea.key() != null && selectedArea.value() != null);
        if (selectedArea.key() != null && selectedArea.value() != null) {
            World w = selectedArea.key().getWorld();
            int xm = (int) Math.max(selectedArea.key().getX(), selectedArea.value().getX());
            int ym = (int) Math.max(selectedArea.key().getY(), selectedArea.value().getY());
            int zm = (int) Math.max(selectedArea.key().getZ(), selectedArea.value().getZ());
            int mx = (int) Math.min(selectedArea.key().getX(), selectedArea.value().getX());
            int my = (int) Math.min(selectedArea.key().getY(), selectedArea.value().getY());
            int mz = (int) Math.min(selectedArea.key().getZ(), selectedArea.value().getZ());
            for (int x = mx; x <= xm; x++)
                for (int y = my; y <= ym; y++)
                    for (int z = mz; z <= zm; z++)
                        c.add(w.getBlockAt(x, y, z));
        }
        return c;
    }

    public Dungeon getCurrentDungeon() {
        return current_dungeon;
    }
    public void setCurrent_dungeon(Dungeon current_dungeon) {
        this.current_dungeon = current_dungeon;
    }
    public boolean isEditing() {
        return current_dungeon != null;
    }

    public void give(DA_item a) {
        if (a != null) this.p.getInventory().addItem(a.getItem());
    }

    public boolean isBuildMode() {
        return build_mode && current_dungeon != null;
    }

    public boolean hasBlocksSelected() {
        return !slb.isEmpty();
    }

    private final class SelectThread extends PluginThread {
        private final List<SectionSelect> selects = new ArrayList<>();
        public void addSelect(SectionSelect select) {
            selects.add(select);
            select.start();
        }
        public void clearSelected() {
            selects.forEach(PluginThread::halt);
        }

        private Particle msp = Particle.COMPOSTER;
        SectionSelect secs = new SectionSelect(selectedArea, msp);
        Location lk = selectedArea.key();
        Location lv = selectedArea.value();
        @Override
        public void run() {
            secs.start();
            try {
                while (r) {
                    slb.forEach(b -> selectBlock(b.getLocation(), 5));
                    Thread.sleep(100);
                    if (selectedArea.key() != lk || selectedArea.value() != lv) {
                        lk = selectedArea.key();
                        lv = selectedArea.value();
                        secs.setSec(selectedArea);
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
                xm = Math.max(sec.key().getX(), sec.value().getX()) + 1;
                ym = Math.max(sec.key().getY(), sec.value().getY()) + 1;
                zm = Math.max(sec.key().getZ(), sec.value().getZ()) + 1;
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
    public void select(Pair<Location, Location> sec) {
        this.selectThread.addSelect(new SectionSelect(sec));
    }
    public void select(Pair<Location, Location> sec, Particle p) {
        this.selectThread.addSelect(new SectionSelect(sec, p));
    }
    public void hideSelections() {
        selectThread.selects.forEach(PluginThread::halt);
        selectThread.selects.clear();
    }
}
