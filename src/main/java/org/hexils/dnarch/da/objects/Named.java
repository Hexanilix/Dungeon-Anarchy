package org.hexils.dnarch.da.objects;

import org.bukkit.entity.Player;

public class Named {
    protected String name;

    //TODO FIX THIS MFFFFF
    public final void promptRename(Player p) {
//        new Renamer(this).rename(p);
    }

    public void rename(String name) {
        this.name = name;
    }
}
