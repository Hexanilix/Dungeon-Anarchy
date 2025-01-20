package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


import static org.hetils.mpdl.InventoryUtil.*;
import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;

public final class Trigger extends DAItem implements Booled, Triggerable, Resetable {
    public static Collection<Trigger> triggers = new HashSet<>();

    private final List<Action> actions = new ArrayList<>();
    private final List<Condition> conditions = new ArrayList<>();

    public Trigger() {
        super(Type.TRIGGER);
        triggers.add(this); }
    public Trigger(List<Action> actions, List<Condition> conditions) {
        super(Type.TRIGGER);
        this.actions.addAll(actions);
        this.conditions.addAll(conditions);
        updateSec();
        triggers.add(this);
    }

    public List<Action> getActions() { return actions; }

    public List<Condition> getConditions() { return conditions; }

    @Override
    protected void updateGUI() {
        fillBox(18, 4, 4, conditions.stream().map(DAItem::getItem).toList());
        fillBox(23, 4, 4, actions.stream().map(DAItem::getItem).toList());
    }

    @Override
    public boolean isSatisfied() { return conditions.stream().allMatch(Booled::isSatisfied); }

    @Override
    protected ItemStack genItemStack() {
        ItemStack i = new ItemStack(Material.COMPARATOR);
        ItemMeta m = i.getItemMeta();
        assert m != null;
        m.setDisplayName(getName());
        i.setItemMeta(m);
        return i;
    }

    public static <T> T getMostFrequentValue(Collection<T> c, T def) {
        if (c == null || c.isEmpty()) return def;

        Map<T, Integer> m = new HashMap<>();
        for (T item : c) m.put(item, m.getOrDefault(item, 0) + 1);

        T mf = null;
        int amnt = 0;

        for (Map.Entry<T, Integer> entry : m.entrySet())
            if (entry.getValue() > amnt) {
                amnt = entry.getValue();
                mf = entry.getKey();
            }

        return mf != null ? mf : def;
    }

    @Override
    public void setSection(Dungeon.Section s) {
        if (section != null) {
            if (s == section && s.getDungeon().getTriggers().contains(this)) return;
            else section.removeTrigger(this);
        }
        section = s;
        if (section != null) section.addTrigger(this);
    }

    public void updateSec() {
        this.setSection(getMostFrequentValue(actions.stream().map(DAItem::getSection).toList(), null));
    }

    @Override
    protected void createGUI() {
        fillBox(18, 4, 4, (ItemStack) null);
        fillBox(23, 4, 4, (ItemStack) null);
        this.setItem(10, newItemStack(Material.COMPARATOR,  ChatColor.LIGHT_PURPLE + "Conditions to trigger: "));
        this.setItem(16, newItemStack(Material.REDSTONE_BLOCK,  ChatColor.AQUA + "Actions on trigger: "));
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
            if ((event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)) {
                if (cinv == getGui()) {
                    event.setCancelled(false);
                    return false;
                }
                if (da instanceof Condition) {
                    this.addToBox(18, 4, 4, ci);
                    cinv.setItem(event.getSlot(), null);
                } else if (da instanceof Action) {
                    this.addToBox(23, 4, 4, ci);
                    cinv.setItem(event.getSlot(), null);
                } else return true;
            } else {
                if (da == null) {
                    da = DAItem.get(event.getCursor());
                    if ((da instanceof Action || da instanceof Condition) && ci == null)
                        event.setCancelled(false);
                } else event.setCancelled(false);
            }
        }
        return true;
    }

    @Override
    public void onInvClose() {
        conditions.clear();
        actions.clear();
        for (ItemStack it : this.getBox(18, 4, 4)) addCondition(it);
        for (ItemStack it : this.getBox(23, 4, 4)) addAction(it);
        updateSec();
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
        if (isSatisfied())
            for (Action a : actions)
                a.onTrigger();
    }

    public void trigger(boolean force) {
        if (force) {
            actions.forEach(org.hexils.dnarch.Action::trigger);
        }
        else trigger();
    }

    @Override
    public void reset() {
        actions.forEach(Action::reset);
    }

    @Override
    public @Nullable DAItem create(@NotNull DungeonMaster dm, String[] args) {
        if (dm.isEditing())
            return dm.getCurrentDungeon().newTrigger();
        else {
            dm.sendError(DungeonMaster.Sender.CREATOR, "You must be editing a dungeon to create triggers");
            return null;
        }
    }
}