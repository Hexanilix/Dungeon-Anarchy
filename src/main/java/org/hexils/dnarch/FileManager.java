package org.hexils.dnarch;

import org.hetils.jgl17.oodp.OODP;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

import static org.hexils.dnarch.Main.log;

public final class FileManager {
    public static String EXT = ".oodp";
    public static File dungeon_dir = new File(Main.plugin.getDataFolder() + "/dungeons/");
    public static File permitted_player_f = new File(Main.plugin.getDataFolder() + "/permitted_players.txt");
    public static File config_file = new File(Main.plugin.getDataFolder() + "/config" + EXT);
    public static File debug_config_file = new File(Main.plugin.getDataFolder() + "/debug_config" + EXT);
    public static File dungeon_log_dir = new File(dungeon_dir + "/logs/");

    @Contract("_ -> param1")
    public static File create(@NotNull File f) {
        try { f.createNewFile(); } catch (IOException e) { throw new RuntimeException(e); }
        return f;
    }

    public static void write(@NotNull File f, @NotNull String str) { write(f, str.getBytes()); }
    public static void write(@NotNull File f, byte[] bytes) {
        if (f.exists())
            try {
                if (!f.exists()) create(f);
                new FileOutputStream(f).write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

    public static void deleteFile(Dungeon d) { getFile(d).delete(); }

    public static File getFile(Object o) {
        File f = null;
        if (o instanceof Dungeon d) {
            for (Map.Entry<Path, OODP.ObjectiveMap> e : mapped_dungeon_files.entrySet())
                if (Objects.equals(e.getValue().getString("name"), d.getOGName()))
                    return e.getKey().toFile();
            f = new File(dungeon_dir + "/" + d.getName() + EXT);
        }
        if (f != null && !f.exists()) create(f);
        return f;
    }

    public static HashMap<Path, OODP.ObjectiveMap> mapped_dungeon_files = new HashMap<>();



    public static void loadData() {
        if (!Main.plugin.getDataFolder().exists()) Main.plugin.getDataFolder().mkdir();
        else {
            loadConfig();
            loadDungeons();
            loadPermittedPlayers();
        }
    }

    public static void loadConfig() {
        if (config_file.exists()) Main.config = Main.dp.map(config_file).as(Main.Config.class);
        else create(config_file);
        if (debug_config_file.exists()) Main.debug = Main.dp.map(debug_config_file).as(Main.Debug.class);
        else create(debug_config_file);
    }

    public static final DateTimeFormatter file_df = DateTimeFormatter.ofPattern("HHmmss_ddMMyyyy");

    public static void loadDungeons() {
        if (!FileManager.dungeon_dir.exists()) FileManager.dungeon_dir.mkdir();
        Dungeon.dungeons.clear();
        mapped_dungeon_files.clear();
        for (File f : dungeon_dir.listFiles()) {
            if (f.isFile()) {
                try {
                    OODP.ObjectiveMap m = Main.dp.map(f);
                    if (m.has("name")) {
                        mapped_dungeon_files.put(f.toPath(), m);
                        m.as(Dungeon.class);
                    } else if (m.isEmpty())
                        f.delete();
                } catch (Exception e) {
                    //TODO add error reports
                    if (!dungeon_log_dir.exists()) dungeon_log_dir.mkdir();
                    File ef = new File(dungeon_log_dir + "/" + LocalDateTime.now().format(file_df) + ".txt");
                    create(ef);
                    try (FileWriter fileWriter = new FileWriter(ef, true)) {
                        e.printStackTrace(new PrintWriter(fileWriter));
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    log(Level.SEVERE, "Error occurred while loading dungeon from file " + f + ". Saved error to " + ef.getName());
                }
            }
        }
    }

    public static void loadPermittedPlayers() {
//        OODP.ObjectiveMap m = Main.dp.map(permitted_player_f);
//        HashMap<UUID, String> p = m.asHashMap(UUID.class, String.class);
//        DungeonMaster.permittedPlayers.addAll(p.keySet());
        if (permitted_player_f.exists()) {
            try {
                List<String> str = Files.readAllLines(permitted_player_f.toPath());
                str.forEach(s -> DungeonMaster.permittedPlayers.add(UUID.fromString(s)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }



    public static void saveData() {
        if (!Main.plugin.getDataFolder().exists()) Main.plugin.getDataFolder().mkdir();
        savePermittedPlayers();
        saveConfig();
        if (!FileManager.dungeon_dir.exists()) FileManager.dungeon_dir.mkdir();
        saveDungeons();
    }

    public static void saveDungeon(Dungeon dungeon) {
        Main.dp.saveToFile(dungeon, FileManager.getFile(dungeon));
    }
    public static void saveDungeons() { for (Dungeon d : Dungeon.dungeons) d.save(); }

    public static void savePermittedPlayers() {
//        HashMap<UUID, String> map = new HashMap<>();
//        DungeonMaster.permittedPlayers.forEach(uuid -> {
//            Player p = Bukkit.getPlayer(uuid);
//            if (p == null) Bukkit.getOfflinePlayer(uuid);
//            map.put(uuid, "nnnnnnnnnn");
//        });
//        if (!map.isEmpty()) Main.dp.saveToFile(Main.dp.mapToOodp(map), permitted_player_f);
//        else Main.dp.saveToFile("{}",permitted_player_f);
        try {
            FileOutputStream outputStream = new FileOutputStream(create(permitted_player_f));
            outputStream.write(String.join("\n", DungeonMaster.permittedPlayers.stream().map(UUID::toString).toList()).getBytes());
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void saveConfig() {
        if (Main.config != null) Main.dp.saveToFile(Main.config, config_file, true);
        if (Main.debug != null) Main.dp.saveToFile(Main.debug, debug_config_file, true);
    }

    public static void reloadDungeon(Dungeon d) {
        File f = getFile(d);
        OODP.ObjectiveMap m = Main.dp.map(f);
        mapped_dungeon_files.put(f.toPath(), m);
        d.delete();
        m.as(Dungeon.class);
    }
}
