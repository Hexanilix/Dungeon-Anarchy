package org.hexils.dnarch.items;

import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.DAItem;

public class Multiplier extends DAItem {
    public static interface DoubleGetter { double get(); }

    private DoubleGetter multi = () -> 1;
    public Multiplier() {
        super(Type.MULTIPLIER);
    }
    public void setMulti(DoubleGetter getter) { this.multi = getter; }

    public double multiplier() { return multi.get(); }

    @Override
    protected ItemStack genItemStack() {
        return null;
    }
}
