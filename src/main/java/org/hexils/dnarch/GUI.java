package org.hexils.dnarch;

import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.hetils.mpdl.NSK;

public final class GUI {
    public static final NSK MODIFIABLE = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-modifiable"), PersistentDataType.BOOLEAN);
    public static final NSK ITEM_FIELD_VALUE = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-field_value"), PersistentDataType.STRING);
    public static final NSK ITEM_ACTION = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-action"), PersistentDataType.STRING);
    public static final NSK SIGN_CHANGEABLE = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-sign_changeable"), PersistentDataType.BOOLEAN);
    public static final NSK ITEM_RENAME = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-prompt_rename"), PersistentDataType.BOOLEAN);

    public static void setField(ItemStack i, String field, String value) {
        NSK.setNSK(i, GUI.ITEM_FIELD_VALUE, field + ":" + value);
    }

    public static void setGuiAction(ItemStack i, String action) {
        NSK.setNSK(i, ITEM_ACTION, action);
    }
}
