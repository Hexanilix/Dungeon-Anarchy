package org.hexils.dnarch.da;

import org.hexils.dnarch.da.actions.Type;

import java.util.Collection;
import java.util.HashSet;

public abstract class Action extends DA_item {

    public final static Collection<Action> actions = new HashSet<>();

    protected boolean triggered;
    public final Type type;

    public Action(Type type) {
        this.type = type;
        actions.add(this);
    }

    public abstract void execute();
    protected abstract void resetAction();

    public final void reset() {
        resetAction();
        triggered = false;
    }

    public final boolean isTriggered() {
        return triggered;
    }
}
