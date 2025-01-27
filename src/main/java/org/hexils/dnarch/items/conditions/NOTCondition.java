package org.hexils.dnarch.items.conditions;

import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Condition;
import org.hexils.dnarch.DAItem;
import org.hexils.dnarch.DungeonMaster;
import org.hexils.dnarch.items.Type;

import java.util.*;

public class NOTCondition extends Condition {

    private final List<Condition> conditions = new ArrayList<>();

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
    public void onInvClose() {
        for (DAItem da : DAItem.get(this.getBox(27, 9, 3)))
            if (da instanceof Condition c)
                conditions.add(c);
    }

    @Override
    protected ItemStack genItemStack() {
        return null;
    }

    @Override
    public DAItem create(DungeonMaster dm, String[] args) { return new NOTCondition(); }
}
