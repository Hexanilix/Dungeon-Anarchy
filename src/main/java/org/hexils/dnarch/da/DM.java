package org.hexils.dnarch.da;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.hexils.dnarch.MainListener;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.hexils.dnarch.Main.*;
import static org.hexils.dnarch.da.DA_item.get;

public class DM {
    public static final Collection<DM> dms = new HashSet<>();

    public final Player p;

    public static @NotNull DM getOrNew(Player p) {
        for (DM m : dms)
            if (m.p == p)
                return m;
        return new DM(p);
    }

    public final List<Block> slb = new ArrayList<>();

    private SelectThread selectThread = null;
    private DM(Player p) {
        this.p = p;
        dms.add(this);
        this.selectThread = (SelectThread) addThread(new SelectThread());
        this.selectThread.start();
        MainListener.runOnPlayerLeave(this, p, () -> this.selectThread.interrupt());
    }

    public static void selectBlock(@NotNull Player p, @NotNull Location l, int ppl) {
        double x = (int) l.getX();
        double y = (int) l.getY();
        double z = (int) l.getZ();
        p.spawnParticle(Particle.COMPOSTER, x+1, y+1, z+1, 1, 0, 0, 0, 0);
        for (double i = 0; i < 1; i+=(double) 1/ppl) {
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

    private class SelectThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    slb.forEach(b -> selectBlock(p, b.getLocation(), 5));
                    Thread.sleep(100);
                }
            } catch (InterruptedException ignore) {}
        }
    }
}
