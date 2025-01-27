package org.hexils.dnarch;

import org.bukkit.entity.Player;
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

import static org.hexils.dnarch.Main.dp;
import static org.hexils.dnarch.Main.log;

public final class FileManager {
    public static String EXT = ".oodp";

    public static File df = Main.plugin().getDataFolder();

    public static File dungeon_dir = new File(df + "/dungeons/");
    public static File permitted_player_f = new File(df + "/permitted_players.txt");
    public static File config_file = new File(df + "/config" + EXT);
    public static File debug_config_file = new File(df + "/debug_config" + EXT);
    public static File dungeon_log_dir = new File(dungeon_dir + "/logs/");
    public static File player_configs = new File(df + "/player_configs/");

    @Contract("_ -> param1")
    public static File create(@NotNull File f) {
        try { new File(f.getParent()).mkdirs(); f.createNewFile(); } catch (IOException e) { throw new RuntimeException(e); }
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
        if (!df.exists()) df.mkdir();
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
        Trigger.instances.clear();
        Action.instances.clear();
        Condition.instances.clear();
        DAItem.instances.clear();
        DAManageable.instances.clear();
        mapped_dungeon_files.clear();
        for (File f : dungeon_dir.listFiles()) {
            if (f.isFile()) {
                try {
                    OODP.ObjectiveMap m = Main.dp.map(f);
                    if (m.has("name")) {
                        mapped_dungeon_files.put(f.toPath(), m);
                        m.as(Dungeon.class);
                    } else if (Main.config.auto_delete_empty_files && m.isEmpty())
                        f.delete();
                } catch (Exception e) {
                    if (!dungeon_log_dir.exists()) dungeon_log_dir.mkdir();
                    File lef = new File(dungeon_log_dir + "/latest.txt");
                    File ef = new File(dungeon_log_dir + "/" + LocalDateTime.now().format(file_df) + ".txt");
                    create(ef);
                    create(lef);
                    try {
                        FileWriter w = new FileWriter(ef, true);
                        e.printStackTrace(new PrintWriter(w));
                        w.close();
                        w = new FileWriter(lef);
                        w.write("");
                        w.close();
                        w = new FileWriter(lef, true);
                        w.flush();
                        e.printStackTrace(new PrintWriter(w));
                        w.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    log(Level.SEVERE, "Error occurred while loading dungeon from file " + f + ". Saved error to /dungeons/logs/latest.txt");
                    if (Main.debug.log_dungeon_error_stacktrace)
                        for (String s : sttss(e.getStackTrace()))
                            log(Level.SEVERE, "\t"+s);
                }
            }
        }
    }

//    public static void write(File f, String data) {}
//    public static void write(File f, char mode, String data) {
//
//    }

    public static String @NotNull [] sttss(StackTraceElement @NotNull [] st) {
        String[] ss = new String[st.length];
        for (int i = 0; i < st.length; i++) {
            StackTraceElement s = st[i];
            ss[i] = "at " + s.getClassName()+ "." +s.getMethodName() + "("+s.getFileName()+":"+s.getLineNumber()+")";
        }
        return ss;
    }

    public static void loadPermittedPlayers() {
//        OODP.ObjectiveMap m = Main.dp.map(permitted_player_f);
//        HashMap<UUID, String> p = m.asHashMap(UUID.class, String.class);
//        DungeonMaster.permittedPlayers.addAll(p.keySet());
        DungeonMaster.dms.clear();
        if (permitted_player_f.exists()) {
            try {
                List<String> str = Files.readAllLines(permitted_player_f.toPath());
                str.forEach(s -> DungeonMaster.permittedPlayers.add(UUID.fromString(s)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Contract("_ -> new")
    public static @NotNull DungeonMaster loadMaster(Player p) {
        if (player_configs.isDirectory()) {
            for (File f : player_configs.listFiles())
                if (f.getName().equals(p.getUniqueId().toString()))
                    return new DungeonMaster(p, dp.map(f));
        }
        return new DungeonMaster(p);
    }



    public static void saveData() {
        if (!df.exists()) df.mkdir();
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
