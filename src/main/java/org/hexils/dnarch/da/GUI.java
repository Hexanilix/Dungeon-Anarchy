package org.hexils.dnarch.da;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.hexils.dnarch.NSK;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.hexils.dnarch.Main.*;

public final class GUI {
    public static final NSK MODIFIABLE = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-modifiable"), PersistentDataType.BOOLEAN);
    public static final NSK FIELD_VALUE = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-object_value"), PersistentDataType.STRING);
    public static final NSK SIGN_CHANGEABLE = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-sign_changeable"), PersistentDataType.BOOLEAN);
    public static final NSK ITEM_RENAME = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-prompt_rename"), PersistentDataType.BOOLEAN);

    public static final ItemStack BACKGROUND;

    static {
        BACKGROUND = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = BACKGROUND.getItemMeta();
        assert m != null;
        m.setDisplayName("");
        BACKGROUND.setItemMeta(m);
    }

    public static @NotNull Inventory newInv(int size, String name) {
        Inventory in = Bukkit.createInventory(null, size, name);
        for (int i = 0; i < 54; i++)
            in.setItem(i, BACKGROUND);
        return in;
    }

    public static void fillBox(Inventory inv, int tlc, int width, int height, ItemStack item) {
        if (inv != null) {
            int i = tlc;
            while (i < inv.getSize() && i/9 < (tlc/9) + height) {
                inv.setItem(i, item);
                i++;
                if (i % 9 >= (tlc % 9) + width || i % 9 < (tlc % 9)) i += 9 - width;
            }
        }
    }

    public static ItemStack[] getBox(Inventory inv, int tlc, int width, int height) {
        ItemStack[] items = new ItemStack[width*height];
        int j = 0;
        if (inv != null) {
            int i = tlc;
            while (i < inv.getSize() && i/9 < (tlc/9) + height) {
                items[j] = inv.getItem(i);
                j++;
                i++;
                if (i % 9 >= (tlc % 9) + width || i % 9 < (tlc % 9)) i += 9 - width;
            }
        }
        return items;
    }

    public static boolean addToBox(Inventory inv, int tlc, int width, int height, ItemStack item) {
        if (inv != null) {
            int i = tlc;
            while (i < inv.getSize() && i/9 < (tlc/9) + height) {
                if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                    inv.setItem(i, item);
                    return true;
                }
                i++;
                if (i % 9 >= (tlc % 9) + width || i % 9 < (tlc % 9)) i += 9 - width;
            }
        }
        return false;
    }

    public static ItemStack b2i(Block block) {
        if (block == null) return null;
        ItemStack i = new ItemStack(block.getType());
        ItemMeta m = i.getItemMeta();
        Location l = block.getLocation();
        m.setDisplayName(ChatColor.RESET.toString() + ChatColor.WHITE + toReadableString(block.getType()));
        m.setLore(List.of(ChatColor.GRAY + "Location: [" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + "]"));
        i.setItemMeta(m);
        return i;
    }

    public static ItemStack b2i(Block block, BlockData blockData) {
        if (block == null) return null;
        if (blockData == null) return b2i(block);
        ItemStack i = new ItemStack(blockData.getMaterial());
        ItemMeta m = i.getItemMeta();
        Location l = block.getLocation();
        m.setDisplayName(ChatColor.RESET.toString() + ChatColor.WHITE + toReadableString(block.getType()));
        m.setLore(List.of(ChatColor.GRAY + "Location: [" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + "]"));
        i.setItemMeta(m);
        return i;
    }

    public static void setField(ItemStack i, String field, String value) {
        setNSK(i, GUI.FIELD_VALUE, field + ":" + value);
    }

    public static void setInventory(Inventory gui, int start, int end) {
        setInventory(gui, start, end, BACKGROUND);
    }

    public static void setInventory(Inventory gui, int start, int end, ItemStack item) {
        for (int i = start; i < end; i++)
            gui.setItem(i, item);
    }

    public static void setBkg(Inventory i, int start, int end) {
        for (int j = start; j < end; j++) {
            i.setItem(j, BACKGROUND);
        }
    }
}
