package org.hexils.dnarch.da;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.hetils.jgl17.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Dungeon {
    public static class DuplicateNameException extends Exception {
        public DuplicateNameException(String s) {
            super(s);
        }
    }
    public static final Collection<Dungeon> dungeons = new ArrayList<>();
    @Contract(pure = true)
    public static @Nullable Dungeon get(String name) {
        for (Dungeon d : dungeons)
            if (Objects.equals(d.name, name))
                return d;
        return null;
    }

    public static class Sector {
        private Pair<Location, Location> points;

        public Sector(Pair<Location, Location> selection) {
            this.points = selection;
        }

        public Pair<Location, Location> getPoints() {
            return points;
        }

        public void setPoints(Pair<Location, Location> points) {
            this.points = points;
        }
    }

    public static int nint = 1;

    private String name;
    private final List<DA_item> items = new ArrayList<>();
    private final List<Sector> sectors = new ArrayList<>();
    private Sector mains;
    private UUID creator;
    private String creator_name;

    public Dungeon(Sector sec) {
        do {
            this.name = "Dungeon" + nint;
            nint++;
        } while (get(name) != null);

        this.mains = sec;
        dungeons.add(this);
    }
    public Dungeon(String name, Sector sec) {
        try {
            if (get(name) != null) throw new DuplicateNameException("");
            this.name = name;
            this.mains = sec;
        } catch (DuplicateNameException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCreator(Player p) {
        this.creator = p == null ? null : p.getUniqueId();
        this.creator_name = p == null ? null : p.getName();
    }

    public void addSector(Sector s) {
        sectors.add(s);
    }

    public List<Sector> getSectors() {
        return sectors;
    }

    public void displaySectors(Player p) {

    }
}
