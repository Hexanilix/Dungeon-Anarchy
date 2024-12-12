package org.hexils.dnarch.objects.conditions;

import org.bukkit.inventory.ItemStack;
import org.hetils.mpdl.InventoryUtil;
import org.hexils.dnarch.Condition;

import java.util.ArrayList;
import java.util.List;

public class NOTCondition extends Condition {
    private List<Condition> conditions = new ArrayList<>();
    public NOTCondition() {
        super(Type.NOT);
    }

    @Override
    protected void onTrigger() {

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
        InventoryUtil.fillBox(gui, 10, 7, 4, (ItemStack) null);
    }

    @Override
    protected ItemStack toItem() {
        return null;
    }
}
