package org.hexils.dnarch.items.actions;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.*;
import org.hexils.dnarch.items.Type;

import java.util.HashSet;

public class ResetAction extends Action {
    private final HashSet<Action> actions = new DAItemNSLinkedHashSet<>();

    public ResetAction() {
        super(Type.RESET);
        this.allowClassesForGui(Resetable.class);
    }

    @Override
    protected void resetAction() {

    }

    @Override
    protected ItemStack genItemStack() { return new ItemStack(Material.REDSTONE_TORCH); }

    @Override
    protected void createGUI() {
        this.setSize(54);
        this.fillBox(27, 9, 3);
    }

    @Override
    protected void updateGUI() {
        this.fillBox(27, 9, 3, actions);
    }

    @Override
    public void onInvClose() {
        actions.clear();
        for (ItemStack i : this.getBox(27, 9, 3)) {
            if (DAItem.get(i) instanceof Action a)
                actions.add(a);
        }
    }

    @Override
    public void trigger() { actions.forEach(Resetable::reset); }

    @Override
    public DAItem create(DungeonMaster dm, String[] args) { return new ResetAction(); }
}
