package org.hexils.dnarch;

import org.hetils.jgl17.oodp.OODPExclude;
import org.hexils.dnarch.items.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hexils.dnarch.Main.log;

public abstract class Condition extends DAItem implements Booled, Triggerable {
    @OODPExclude
    private final Set<Trigger> triggers = new HashSet<>();

    public Condition(Type type) { this(type, true); }
    public Condition(Type type, boolean renamealbe) {
        super(type, renamealbe);
    }

    public void bind(Trigger t) { triggers.add(t); }
    public void unbind(Trigger t) { triggers.remove(t); }

    @Override
    public final void trigger() {
        onTrigger();
        triggers.forEach(Trigger::trigger);
    }

    protected void onTrigger() {}
}
