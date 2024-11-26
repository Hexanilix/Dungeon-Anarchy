package org.hexils.dnarch;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public record NSK(NamespacedKey key, PersistentDataType type) {}
