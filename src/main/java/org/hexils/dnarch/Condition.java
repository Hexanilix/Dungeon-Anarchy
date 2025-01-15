package org.hexils.dnarch;

import org.hetils.jgl17.oodp.OODPExclude;
import org.hexils.dnarch.items.Type;

import java.util.ArrayList;
import java.util.List;

public abstract class Condition extends DAItem implements Booled, Triggerable {
    @OODPExclude
    private final List<Trigger> triggers = new ArrayList<>();

    public Condition(Type type) { this(type, true); }
    public Condition(Type type, boolean renamealbe) {
        super(type, renamealbe);
    }

    public void bind(Trigger t) { if (triggers.contains(t)) triggers.add(t); }
    public void unbind(Trigger t) { triggers.remove(t); }
}
