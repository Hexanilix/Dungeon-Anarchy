package org.hexils.dnarch.items;

import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.DAItem;
import org.hexils.dnarch.DungeonMaster;

public class Multiplier extends DAItem {
    @Override
    public DAItem create(DungeonMaster dm, String[] args) {
        return new Multiplier();
    }

    public static interface DoubleGetter { double get(); }

    private DoubleGetter multi = () -> 1;
    public Multiplier() {
        super(Type.MULTIPLIER);
    }

    @Override
    protected void createGUI() {

    }

    public void setMulti(DoubleGetter getter) { this.multi = getter; }

    public double multiplier() { return multi.get(); }

    @Override
    protected ItemStack genItemStack() {
        return null;
    }
}
