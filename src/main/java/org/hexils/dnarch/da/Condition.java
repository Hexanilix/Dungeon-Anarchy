package org.hexils.dnarch.da;

import java.util.HashMap;
import java.util.Map;

public abstract class Triggerable extends DA_item implements Booled, Triggable {
    public enum Type {
        DISTANCE
    }

    public final Map<DA_item, Runnable> runnables = new HashMap<>();

    protected final Type type;

    public final Type getType() {
        return type;
    }

    public Triggerable(Type type) {
        this.type = type;
    }

}
