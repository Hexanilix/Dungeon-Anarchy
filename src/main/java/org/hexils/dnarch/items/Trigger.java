package org.hexils.dnarch.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;


import static org.hetils.mpdl.InventoryUtil.*;
import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;

public class Trigger extends DAItem implements Booled, Triggerable {
    public static Collection<Trigger> triggers = new HashSet<>();

    public final List<Condition> conditions = new ArrayList<>();
    public final List<Action> actions = new ArrayList<>();

    public Trigger() { super(Type.TRIGGER); triggers.add(this); }

    @Override
    public boolean isSatisfied() { return conditions.stream().allMatch(Booled::isSatisfied); }

    @Override
    protected void createGUI() {
        fillBox(18, 4, 4, (ItemStack) null);
        fillBox(23, 4, 4, (ItemStack) null);
        this.setItem(10, newItemStack(Material.COMPARATOR,  ChatColor.LIGHT_PURPLE + "Conditions to trigger: "));
        this.setItem(16, newItemStack(Material.REDSTONE_BLOCK,  ChatColor.AQUA + "Actions on trigger: "));
    }

    @Override
    protected ItemStack genItemStack() {
        ItemStack i = new ItemStack(Material.COMPARATOR);
        ItemMeta m = i.getItemMeta();
        assert m != null;
        m.setDisplayName(getName());
        i.setItemMeta(m);
        return i;
    }

    @Override
    public boolean guiClickEvent(@NotNull InventoryClickEvent event) {
        ItemStack ci = event.getCurrentItem();
        ItemStack iih = event.getCursor();
        if ((ci == null || ci.isSimilar(BACKGROUND)) && iih == null)
            return false;
        Inventory cinv = event.getClickedInventory();
        DAItem da = DAItem.get(ci);
        if (cinv != null) {
            if ((event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) && cinv != this) {
                if (da instanceof Condition) {
                    this.addToBox(18, 4, 4, ci);
                    cinv.setItem(event.getSlot(), null);
                    updateAC();
                } else if (da instanceof Action) {
                    this.addToBox(23, 4, 4, ci);
                    cinv.setItem(event.getSlot(), null);
                    updateAC();
                } else return true;
            } else {
                if (da == null) {
                    da = DAItem.get(event.getCursor());
                    if ((da instanceof Action || da instanceof Condition) && ci == null) {
                        event.setCancelled(false);
                        updateAC(event);
                    }
                } else {
                    event.setCancelled(false);
                    updateAC(ci);
                }
            }
        }
        return true;
    }

    private void updateAC() { updateAC(null, null); }
    private void updateAC(ItemStack ex) { updateAC(null, ex); }
    private void updateAC(InventoryClickEvent event) { updateAC(event, null); }
    private void updateAC(InventoryClickEvent event, ItemStack ex) {
        conditions.clear();
        for (ItemStack it : this.getBox(18, 4, 4))
            if (it != ex) addCondition(it);
        actions.clear();
        for (ItemStack it : this.getBox(23, 4, 4))
            if (it != ex) addAction(it);
        if (event != null && !event.getClick().name().contains("SHIFT")) {
            addCondition(event.getCursor());
            addAction(event.getCursor());
        }
    }
    private boolean addCondition(ItemStack it) {
        String s = (String) NSK.getNSK(it, ITEM_UUID);
        if (s != null && DAItem.get(UUID.fromString(s)) instanceof Condition condition && !conditions.contains(condition)) {
            return conditions.add(condition);
        }
        return false;
    }

    private boolean addAction(ItemStack it) {
        String s = (String) NSK.getNSK(it, ITEM_UUID);
        if (s != null && DAItem.get(UUID.fromString(s)) instanceof Action action && !actions.contains(action))
            return actions.add(action);
        return false;
    }

    @Override
    public void onTrigger() {
        log("trig Triggered");
        if (isSatisfied())
            for (Action a : actions)
                a.onTrigger();
    }
}