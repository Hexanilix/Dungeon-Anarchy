package org.hexils.dnarch.items.actions;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Action;
import org.hexils.dnarch.DAItem;
import org.hexils.dnarch.DungeonMaster;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResetAction extends Action {
    private final Set<Action> actions = new HashSet<>();

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
    public void onTrigger() {
        actions.forEach(Action::reset);
    }

    @Override
    public DAItem create(DungeonMaster dm, String[] args) { return new ResetAction(); }
}
