package org.hexils.dnarch;

import org.hetils.jgl17.NSLinkedHashSet;
import org.hetils.mpdl.Manageable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DAItemNSLinkedHashSet<T extends DAItem> extends NSLinkedHashSet<T> {
    public DAItemNSLinkedHashSet(@NotNull Collection<T> c) { super(c.stream().filter(m -> !m.isDeleted()).collect(Collectors.toSet())); }
    public DAItemNSLinkedHashSet() { super(); }
    public DAItemNSLinkedHashSet(int initialCapacity) { super(initialCapacity); }
    public DAItemNSLinkedHashSet(int initialCapacity, float loadFactor) { super(initialCapacity, loadFactor); }

    @Override
    public @NotNull Stream<T> stream() {
        removeIf(Manageable::isDeleted);
        return super.stream();
    }
}
