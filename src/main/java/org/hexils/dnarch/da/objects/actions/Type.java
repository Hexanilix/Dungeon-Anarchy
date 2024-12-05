package org.hexils.dnarch.da.objects.actions;

public enum Type {
    DESTROY_BLOCK,
    ENTITY_MOD,
    REPLACE_BLOCK,
    DOOR,
    SPAWN, TIMER;

    public static Type get(String s) {
        if (s != null)
            for (Type t : values())
                if (t.name().equalsIgnoreCase(s))
                    return t;
        return null;
    }
}
