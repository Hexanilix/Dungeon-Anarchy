package org.hexils.dnarch;

import org.hexils.dnarch.dungeon.Dungeon;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class FileManager {
    public static File dungeon_dir = new File(Main.plugin.getDataFolder() + "/dungeons/");

    public static @NotNull File getFile(Dungeon dungeon) {
        for (File f : dungeon_dir.listFiles()) {
            if (f.getName().equals(dungeon.getName()))
                return f;
        }
        File f = new File(dungeon_dir + "/" + dungeon.getName() + ".txt");
        try {
            f.createNewFile();
        } catch (IOException e) { throw new RuntimeException(e); }
        return f;
    }
}
