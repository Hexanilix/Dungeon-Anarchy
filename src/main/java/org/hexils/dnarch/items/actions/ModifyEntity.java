package org.hexils.dnarch.items.actions;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Action;
import org.hexils.dnarch.DA_item;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.hetils.mpdl.ItemUtil.newItemStack;

public class ModifyEntity extends Action {
    private List<Spawn.EntityCollection> entities = new ArrayList<>();

    public ModifyEntity() {
        super(Type.ENTITY_MOD);
    }

    @Override
    public void trigger() {

    }

    @Override
    protected void resetAction() {

    }

    @Override
    protected void createGUI() {
        this.setSize(54);
        this.fillBox(18, 4, 4);
        this.fillBox(23, 4, 4);
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
        for (ItemStack i : this.getContents()) {
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
    protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {

    }

    public static String commandNew(@NotNull DungeonMaster dm, @NotNull String[] args) {
        return "";
    }
}
