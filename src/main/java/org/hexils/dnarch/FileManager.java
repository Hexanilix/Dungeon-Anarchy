package org.hexils.dnarch;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.hetils.jgl17.oodp.OODP;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

import static org.hexils.dnarch.Main.log;

public class FileManager {
    public static File dungeon_dir = new File(Main.plugin.getDataFolder() + "/dungeons/");
    public static File permitted_player_f = new File(Main.plugin.getDataFolder() + "/permitted_players.txt");

    public static HashMap<Path, OODP.ObjectiveMap> mapped_dungeon_files = new HashMap<>();

    public static void saveData() {
        saveDungeon();
        savePermittedPlayers();
    }

    public static void loadData() {
        if (!Main.plugin.getDataFolder().exists()) Main.plugin.getDataFolder().mkdir();
        if (!FileManager.dungeon_dir.exists()) FileManager.dungeon_dir.mkdir();
        try {
            if (!permitted_player_f.exists()) permitted_player_f.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loadDungeons();
        loadPermittedPlayers();
    }

    public static @NotNull File getFile(Dungeon dungeon) {
        for (Map.Entry<Path, OODP.ObjectiveMap> e : mapped_dungeon_files.entrySet())
            if (Objects.equals(e.getValue().getString("name"), dungeon.getOGName()))
                return e.getKey().toFile();
        File f = new File(dungeon_dir + "/" + dungeon.getName() + ".txt");
        try {
            f.createNewFile();
        } catch (IOException e) { throw new RuntimeException(e); }
        return f;
    }

    public static void loadDungeons() {
        Dungeon.dungeons.clear();
        mapped_dungeon_files.clear();
        for (File f : dungeon_dir.listFiles()) {
            try {
                OODP.ObjectiveMap m = Main.dp.map(f);
                mapped_dungeon_files.put(f.toPath(), m);
                m.as(Dungeon.class);
            } catch (Exception e) {
                log(Level.SEVERE, "Error occurred while loading dungeon from file " + f + ", reason:");
                e.printStackTrace();
            }
        }
    }

    public static void saveDungeon() { for (Dungeon d : Dungeon.dungeons) d.save(); }

    public static void deleteFile(Dungeon d) { getFile(d).delete(); }

    public static void savePermittedPlayers() {
//        HashMap<UUID, String> map = new HashMap<>();
//        DungeonMaster.permittedPlayers.forEach(uuid -> {
////            Player p = Bukkit.getPlayer(uuid);
////            if (p == null) Bukkit.getOfflinePlayer(uuid);
//            map.put(uuid, "nnnnnnnnnn");
//        });
//        if (!map.isEmpty()) Main.dp.saveToFile(Main.dp.mapToOodp(map), permitted_player_f);
//        else Main.dp.saveToFile("{}",permitted_player_f);
        try {
            if (!permitted_player_f.exists()) permitted_player_f.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(permitted_player_f);
            outputStream.write(String.join("\n", DungeonMaster.permittedPlayers.stream().map(UUID::toString).toList()).getBytes());
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void loadPermittedPlayers() {
//        OODP.ObjectiveMap m = Main.dp.map(permitted_player_f);
//        HashMap<UUID, String> p = m.asHashMap(UUID.class, String.class);
//        DungeonMaster.permittedPlayers.addAll(p.keySet());
        try {
            List<String> str = Files.readAllLines(permitted_player_f.toPath());
            str.forEach(s ->DungeonMaster.permittedPlayers.add(UUID.fromString(s)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
