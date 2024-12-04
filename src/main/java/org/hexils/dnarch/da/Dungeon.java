package org.hexils.dnarch.da;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.hetils.jgl17.Pair;
import org.hexils.dnarch.da.conditions.DungeonStart;
import org.hexils.dnarch.da.conditions.Type;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.hexils.dnarch.Main.log;

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
    @Contract(pure = true)
    public static @Nullable Dungeon get(Location loc) {
        for (Dungeon d : dungeons)
            if (d.isWWithinDungeon(loc))
                return d;
        return null;
    }

    private class Section {
        private final Pair<Location, Location> points;
        private String name;

        public Section(Pair<Location, Location> selection) {
            this(selection, "Section" + sections.size());
        }

        public Section(Pair<Location, Location> selection, String name) {
            this.points = selection != null ? new Pair<>(selection.key(), selection.value()) : null;
            this.name = name;
        }

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        public Pair<Location, Location> getPoints() {
            return points;
        }
        public boolean withinBounds(Location l) {
            return (points != null && points.key() != null && points.value() != null && l != null) &&
                    (Math.max(points.key().getX(), points.value().getX()) >= l.getX() && l.getX() >= Math.min(points.key().getX(), points.value().getX())) &&
                    (Math.max(points.key().getY(), points.value().getY()) >= l.getY() && l.getY() >= Math.min(points.key().getY(), points.value().getY())) &&
                    (Math.max(points.key().getZ(), points.value().getZ()) >= l.getZ() && l.getZ() >= Math.min(points.key().getZ(), points.value().getZ()));
        }
    }


    public static int nint = 0;

    private String name;
    private final List<DA_item> items = new ArrayList<>();
    private final List<Section> sections = new ArrayList<>();
    private Section mains;
    private final UUID creator;
    private String creator_name;
    private Condition dungeon_start;
    private boolean is_running = false;
    public boolean isRunning() {
        return is_running;
    }

    public Dungeon(UUID creator, Pair<Location, Location> sec) {
        Player p = Bukkit.getPlayer(creator);
        this.creator = creator;
        this.creator_name = p == null ? "" : p.getName();
        do {
            this.name = "Dungeon" + nint;
            nint++;
        } while (get(name) != null);

        this.mains = new Section(sec);
        dungeons.add(this);
    }
    public Dungeon(UUID creator, String name, Pair<Location, Location> sec) throws DuplicateNameException {
        Player p = Bukkit.getPlayer(creator);
        this.creator = creator;
        this.creator_name = p == null ? "" : p.getName();
        if (get(name) != null) throw new DuplicateNameException("");
        this.name = name;
        this.mains = new Section(sec, "Main_Sector");
        this.dungeon_start = new DungeonStart(this);
        dungeons.add(this);
    }

    public String getName() {
        return name;
    }

    public Section getMains() {
        return mains;
    }

    public UUID getCreator() {
        return creator;
    }

    public List<DA_item> getItems() {
        return items;
    }

    public String getCreator_name() {
        return creator_name;
    }

    public Condition getEventBlock(Type type) {
        if (type == null) return null;
        return switch (type) {
            case DUNGEON_START -> dungeon_start;
            default -> null;
        };
    }

    public void newSection(Pair<Location, Location> sec) {
        sections.add(new Section(sec));
    }
    public void newSection(Pair<Location, Location> sec, String name) {
        sections.add(new Section(sec, name));
    }


    public static final Particle[] selectParts;
    static {
        selectParts = new Particle[]{Particle.ENCHANTMENT_TABLE, Particle.COMPOSTER, Particle.SOUL_FIRE_FLAME, Particle.FLAME, Particle.END_ROD};
    }
    public void displaySectors(Player p) {
        DM dm = DM.getOrNew(p);
        dm.select(mains.getPoints(), selectParts[0]);
        for (int i = 0; i < sections.size(); i++) {
            dm.select(sections.get(i).getPoints(), selectParts[i%selectParts.length]);
        }
    }

    public boolean isWWithinDungeon(Location l) {
        for (Section s : sections)
            if (s.withinBounds(l))
                return true;
        return mains.withinBounds(l);
    }

    public void startDungeon() {
        this.is_running = true;
        dungeon_start.trigger();
    }
}
