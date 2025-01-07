package org.hexils.dnarch;

import org.hexils.dnarch.items.Type;

import java.util.HashMap;
import java.util.Map;

public abstract class Condition extends DAItem implements Booled, Triggerable {

    public Condition(Type type) { this(type, true); }
    public Condition(Type type, boolean renamealbe) {
        super(type, renamealbe);
    }
}
