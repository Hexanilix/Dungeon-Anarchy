package org.hexils.dnarch.items.conditions;

import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Condition;
import org.hexils.dnarch.DAItem;
import org.hexils.dnarch.DungeonMaster;
import org.hexils.dnarch.items.InnerItemClass;
import org.hexils.dnarch.items.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@InnerItemClass
public class NOTCondition extends Condition {

    private final Set<Condition> conditions = new HashSet<>();


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

    @Override
    public DAItem create(DungeonMaster dm, String[] args) { return new NOTCondition(); }
}
