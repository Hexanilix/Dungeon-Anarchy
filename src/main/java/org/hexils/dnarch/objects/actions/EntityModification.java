package org.hexils.dnarch.objects.actions;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.hetils.mpdl.Inventory;
import org.hexils.dnarch.Action;
import org.hexils.dnarch.DA_item;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.hetils.mpdl.Item.newItemStack;

public class EntityModification extends Action {
    private List<Spawn.EntityCollection> entities = new ArrayList<>();

    public EntityModification() {
        super(Type.ENTITY_MOD);
    }

    @Override
    public void execute() {

    }

    @Override
    protected void resetAction() {

    }

    @Override
    protected void createGUIInventory() {
        this.guiSize(54);
        Inventory.fillBox(gui, 18, 4, 4);
        Inventory.fillBox(gui, 23, 4, 4);
    }

    @Override
    public void updateGUI() {

    }

    @Override
    protected ItemStack toItem() {
        ItemStack i = newItemStack(Material.TRIPWIRE_HOOK, "Entity Modification");
        return i;
    }

    @Override
    public boolean guiClickEvent(InventoryClickEvent event) {
        for (ItemStack i : gui.getContents()) {
            DA_item da = DA_item.get(i);
            if (da instanceof Spawn.EntityCollection ec)
                entities.add(ec);
        }
        return true;
    }

    @Override
    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {

    }

    @Override
    protected void action(DungeonMaster dm, String action, String[] args) {

    }
}
