package org.hexils.dnarch.items.actions.entity;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Action;
import org.hexils.dnarch.DAItem;
import org.hexils.dnarch.DungeonMaster;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.hetils.mpdl.item.ItemUtil.newItemStack;

public class ModifyEntity extends Action {
    private List<EntitySpawnAction> spawns = new ArrayList<>();

    public ModifyEntity() { super(Type.ENTITY_MOD); }

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
    protected void updateGUI() {

    }

    @Override
    protected ItemStack genItemStack() {
        ItemStack i = newItemStack(Material.TRIPWIRE_HOOK, "Entity Modification");
        return i;
    }

    @Override
    public boolean guiClickEvent(InventoryClickEvent event) {
        for (ItemStack i : this.getContents()) {
            DAItem da = DAItem.get(i);
            if (da instanceof EntitySpawnAction ec)
                spawns.add(ec);
        }
        return true;
    }

    @Contract(pure = true)
    public static @NotNull String commandNew(@NotNull DungeonMaster dm, @NotNull String[] args) {
        return "";
    }

    @Override
    public String toString() {
        return "ModifyEntity{" +
                "spawns=" + spawns.stream().map(s -> getId()) +
                '}';
    }

    @Override
    public DAItem create(DungeonMaster dm, String[] args) { return new ModifyEntity(); }

    @Override
    public void trigger() {

    }
}
