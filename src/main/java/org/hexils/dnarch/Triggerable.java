package org.hexils.dnarch;

import org.hexils.dnarch.items.Trigger;

import static org.hexils.dnarch.Main.log;

public interface Triggerable {

    default void trigger() {
        log("inetftrig");
        for (Trigger t : Trigger.triggers)
            if (t.conditions.stream().anyMatch(c -> c == this)) {
                log("foundOne");
                t.trigger();
                break;
            }
        this.onTrigger();
    }

    default void onTrigger() {}
}
