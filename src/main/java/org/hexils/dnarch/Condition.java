package org.hexils.dnarch;

import org.hexils.dnarch.items.Type;

import java.util.HashMap;
import java.util.Map;

public abstract class Condition extends DAItem implements Booled, Triggerable {

    public final Map<DAItem, Runnable> runnables = new HashMap<>();

    public Condition(Type type) { this(type, true); }
    public Condition(Type type, boolean renamealbe) {
        super(type, renamealbe);
    }

    public final void trigger() {
        onTrigger();
        runnables.values().forEach(Runnable::run);
    }

    protected void onTrigger() {}
}
