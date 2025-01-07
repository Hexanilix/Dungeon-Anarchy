package org.hexils.dnarch.items.conditions;

import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Condition;
import org.hexils.dnarch.items.InnerItemClass;
import org.hexils.dnarch.items.Type;

import java.util.ArrayList;
import java.util.List;

@InnerItemClass
public class NOTCondition extends Condition {
    private List<Condition> conditions = new ArrayList<>();
    public NOTCondition() {
        super(Type.NOT);
    }

    @Override
    public boolean isSatisfied() {
        for (Condition c : conditions)
            if (c.isSatisfied())
                return false;
        return true;
    }

    @Override
    protected void createGUI() {
        this.setSize(54);
        this.fillBox(10, 7, 4, (ItemStack) null);
    }

    @Override
    protected ItemStack genItemStack() {
        return null;
    }
}
