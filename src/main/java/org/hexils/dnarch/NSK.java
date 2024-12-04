package org.hexils.dnarch;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record NSK(NamespacedKey key, PersistentDataType type) {
    public static @Nullable Object getNSK(ItemStack i, NSK nsk) {
        return hasNSK(i, nsk) ? i.getItemMeta().getPersistentDataContainer().get(nsk.key(), nsk.type()) : null;
    }

    public static boolean sameType(PersistentDataType type, Object value) {
        return (value instanceof String && type == PersistentDataType.STRING)
                || (value instanceof Integer && type == PersistentDataType.INTEGER)
                || (value instanceof Boolean && type == PersistentDataType.BOOLEAN)
                || (value instanceof Double && type == PersistentDataType.DOUBLE)
                || (value instanceof Long && type == PersistentDataType.LONG_ARRAY)
                || (value instanceof Byte && type == PersistentDataType.BYTE)
                || (value instanceof byte[] && type == PersistentDataType.BYTE_ARRAY)
                || (value instanceof int[] && type == PersistentDataType.INTEGER_ARRAY);
    }

    public static void setNSK(ItemStack i, NSK nsk, Object value) {
        if (i == null || !i.hasItemMeta()) return;
        ItemMeta m = i.getItemMeta();
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        if (sameType(nsk.type(), value) && !pdc.has(nsk.key(), nsk.type()))
            pdc.set(nsk.key(), nsk.type(), value);
        i.setItemMeta(m);
    }

    public static boolean hasNSK(ItemStack i, NSK nsk) {
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(nsk.key(), nsk.type());
    }

    public static boolean hasNSK(ItemStack i, NSK nsk, Object value) {
        return Objects.equals(getNSK(i, nsk), value);
    }
}
