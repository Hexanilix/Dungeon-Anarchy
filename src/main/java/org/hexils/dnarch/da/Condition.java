package org.hexils.dnarch.da;

import org.hexils.dnarch.da.objects.conditions.Type;

import java.util.HashMap;
import java.util.Map;

public abstract class Condition extends DA_item implements Booled, Triggerable {

    public final Map<DA_item, Runnable> runnables = new HashMap<>();

    protected Type type;

    public final Type getType() {
        return type;
    }

    public Condition(Type type) {
        this.type = type;
    }

    public final void trigger() {
        runnables.values().forEach(Runnable::run);
        onTrigger();
    }

    protected abstract void onTrigger();
}
