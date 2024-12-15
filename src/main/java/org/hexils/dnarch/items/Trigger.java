package org.hexils.dnarch.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.*;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.jetbrains.annotations.NotNull;

import java.util.*;


import static org.hetils.mpdl.InventoryUtil.*;
import static org.hetils.mpdl.ItemUtil.newItemStack;

public class Trigger extends DA_item implements Booled, Triggerable {
    public final List<Condition> conditions = new ArrayList<>();
    public final List<Action> actions = new ArrayList<>();

    @Override
    public boolean isSatisfied() {
        for (Condition c : conditions)
            if (!c.isSatisfied())
                return false;
        return true;
    }

    @Override
    protected void createGUI() {
        setSize(54);
        fillBox(18, 4, 4, (ItemStack) null);
        fillBox(23, 4, 4, (ItemStack) null);
        this.setItem(10, newItemStack(Material.COMPARATOR,  ChatColor.LIGHT_PURPLE + "Conditions to trigger: "));
        this.setItem(15, newItemStack(Material.REDSTONE_BLOCK,  ChatColor.AQUA + "Actions on trigger: "));
    }

    public Trigger() {
        super(Type.TRIGGER);
    }

    @Override
    public void updateGUI() {}

    @Override
    protected ItemStack genItemStack() {
        ItemStack i = new ItemStack(Material.COMPARATOR);
        ItemMeta m = i.getItemMeta();
        assert m != null;
        m.setDisplayName(getName());

        m.getPersistentDataContainer().set(MODIFIABLE.key(), PersistentDataType.BOOLEAN, true);
        i.setItemMeta(m);
        return i;
    }

    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {

    }

    @Override
    public boolean guiClickEvent(@NotNull InventoryClickEvent event) {
        ItemStack ci = event.getCurrentItem();
        ItemStack iih = event.getCursor();
        if ((ci == null || ci.isSimilar(BACKGROUND)) && iih == null)
            return false;
        Inventory cinv = event.getClickedInventory();
        DA_item da = DA_item.get(ci);
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
                    da = DA_item.get(event.getCursor());
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
        if (s != null && DA_item.get(UUID.fromString(s)) instanceof Condition condition && !conditions.contains(condition)) {
            condition.runnables.put(this, this::trigger);
            return conditions.add(condition);
        }
        return false;
    }

    private boolean addAction(ItemStack it) {
        String s = (String) NSK.getNSK(it, ITEM_UUID);
        if (s != null && DA_item.get(UUID.fromString(s)) instanceof Action action && !actions.contains(action))
            return actions.add(action);
        return false;
    }



    @Override
    public void trigger() {
        if (isSatisfied())
            for (Action a : actions)
                a.trigger();
    }
}