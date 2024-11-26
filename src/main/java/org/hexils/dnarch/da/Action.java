package org.hexils.dnarch.da;

import java.util.Collection;
import java.util.HashSet;

public abstract class Action extends DA_item {
    public final static Collection<Action> actions = new HashSet<>();

    protected boolean triggered;

    public Action() {
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
