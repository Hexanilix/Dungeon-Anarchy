package org.hexils.dnarch;

import static org.hexils.dnarch.Main.log;

public interface Triggerable {

    default void trigger() {
        for (Trigger t : Trigger.triggers)
            if (t.getConditions().stream().anyMatch(c -> c == this)) {
                t.trigger();
                break;
            }
        this.onTrigger();
    }

    default void onTrigger() {}
}
