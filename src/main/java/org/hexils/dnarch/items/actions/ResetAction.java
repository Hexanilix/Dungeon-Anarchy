package org.hexils.dnarch.items.actions;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Action;
import org.hexils.dnarch.DAItem;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ResetAction extends Action {
    private final List<Action> actions = new ArrayList<>();

    public ResetAction() {
        super(Type.RESET_ACTION);
    }

    @Override
    protected void resetAction() {

    }

    @Override
    protected ItemStack genItemStack() {
        return new ItemStack(Material.CLOCK);
    }

    @Override
    protected void createGUI() {
        this.setSize(54);
        this.fillBox(10, 7, 4);
    }

    @Override
    public boolean guiClickEvent(@NotNull InventoryClickEvent event) {
        ItemStack ci = event.getCurrentItem();
        ItemStack iih = event.getCursor();
        DAItem cda = DAItem.get(ci);
        DAItem hda = DAItem.get(iih);
        if ((cda != null && !(cda instanceof Action)) || (hda != null && !(hda instanceof Action))) return false;

        return false;
    }

    @Override
    public void trigger() {
//        for (Item)
    }
}
