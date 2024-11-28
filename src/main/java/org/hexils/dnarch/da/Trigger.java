package org.hexils.dnarch.da;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.hexils.dnarch.Main;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.hetils.mpdl.Item.newItemStack;
import static org.hexils.dnarch.da.GUI.BACKGROUND;

public class Trigger extends DA_block implements Booled, Triggerable {
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
    protected Inventory createGUIInventory() {
        gui = GUI.newInv(54, this.name);
        GUI.fillBox(gui, 18, 4, 4, null);
        GUI.fillBox(gui, 23, 4, 4, null);
        gui.setItem(10, newItemStack(Material.COMPARATOR,  ChatColor.LIGHT_PURPLE + "Conditions to trigger: "));
        gui.setItem(15, newItemStack(Material.REDSTONE_BLOCK,  ChatColor.AQUA + "Actions on trigger: "));
        return gui;
    }

    public Trigger() {
        this.name = "Trigger";
    }

    @Override
    public void updateGUI() {}

    @Override
    protected ItemStack toItem() {
        ItemStack i = new ItemStack(Material.COMPARATOR);
        ItemMeta m = i.getItemMeta();
        assert m != null;
        m.setDisplayName(name);

        m.getPersistentDataContainer().set(GUI.MODIFIABLE.key(), PersistentDataType.BOOLEAN, true);
        i.setItemMeta(m);
        return i;
    }

    @Override
    protected void changeField(DM dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DM dm, String action, String[] args) {

    }

    private boolean addTriggerable(ItemStack it) {
        String s = (String) Main.getNSK(it, ITEM_UUID);
        if (s != null && DA_item.get(UUID.fromString(s)) instanceof Condition condition && !conditions.contains(condition)) {
            condition.runnables.put(this, this::trigger);
            return conditions.add(condition);
        }
        return false;
    }

    private boolean addAction(ItemStack it) {
        String s = (String) Main.getNSK(it, ITEM_UUID);
        if (s != null && DA_item.get(UUID.fromString(s)) instanceof Action action && !actions.contains(action)) {
            return actions.add(action);
        }
        return false;
    }

    @Override
    public boolean guiClickEvent(@NotNull InventoryClickEvent event) {
        ItemStack ci = event.getCurrentItem();
        ItemStack iih = event.getCursor();
        if ((ci == null || ci.isSimilar(BACKGROUND)) && iih == null)
            return false;
        Inventory cinv = event.getClickedInventory();
        DA_item da = DA_item.get(ci);
        if ((event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) && cinv != this.gui) {
            if (da instanceof Condition) {
                GUI.addToBox(gui, 18, 4, 4, ci);
                cinv.setItem(event.getSlot(), null);
                updateAC();
            } else if (da instanceof Action) {
                GUI.addToBox(gui, 23, 4, 4, ci);
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
//        if (event.getClickedInventory() instanceof PlayerInventory) {
//            ItemStack ci = event.getCurrentItem();
//            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
//                DA_item da = DA_item.get(ci);
//                if (da instanceof Condition)
//                    GUI.addToBox(gui, 18, 4, 4, ci);
//                else if (da instanceof Action)
//                    GUI.addToBox(gui, 23, 4, 4, ci);
//                else return true;
//            } else event.setCancelled(false);
//        } else {
//
//            int slot = event.getRawSlot();
//            if (slot / 9 >= 2) {
//                if (iih == null || iih.getType() == Material.AIR) {
//                    event.setCancelled(false);
//                } else if (slot % 9 < 4) {
//                    conditions.clear();
//                    for (ItemStack it : GUI.getBox(this.gui, 18, 4, 4))
//                        addTriggerable(it);
//                    event.setCancelled(!addTriggerable(iih));
//                } else if (slot % 9 > 4) {
//                    actions.clear();
//                    for (ItemStack it : GUI.getBox(this.gui, 22, 4, 4))
//                        addAction(it);
//                    event.setCancelled(!addAction(iih));
//                }
//            }
//        }
        return true;
    }

    private void updateAC() {
        updateAC(null, null);
    }

    private void updateAC(ItemStack ex) {
        updateAC(null, ex);
    }

    private void updateAC(InventoryClickEvent event) {
        updateAC(event, null);
    }

    private void updateAC(InventoryClickEvent event, ItemStack ex) {
        conditions.clear();
        for (ItemStack it : GUI.getBox(this.gui, 18, 4, 4))
            if (it != ex) addTriggerable(it);
        actions.clear();
        for (ItemStack it : GUI.getBox(this.gui, 22, 4, 4))
            if (it != ex) addAction(it);
        if (event != null && !event.getClick().name().contains("SHIFT")) {
            addTriggerable(event.getCursor());
            addAction(event.getCursor());
        }
    }



    @Override
    public void trigger() {
        if (isSatisfied())
            for (Action a : actions)
                a.execute();
    }
}