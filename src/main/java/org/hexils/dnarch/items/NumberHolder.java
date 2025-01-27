package org.hexils.dnarch.items;

import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.DAItem;
import org.hexils.dnarch.DungeonMaster;

@Type.NotCreatable
public class NumberHolder extends DAItem {
    @Override
    public DAItem create(DungeonMaster dm, String[] args) { return null; }

    public interface DoubleGetter { double get(); }

    private DoubleGetter multi = () -> 1;
    private DAItem root;
    public NumberHolder(DAItem root) {
        super(Type.NUMBER_HOLDER);

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
