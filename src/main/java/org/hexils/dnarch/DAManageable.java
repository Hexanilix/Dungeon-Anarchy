package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.hetils.jgl17.Getter;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.Manageable;
import org.hetils.mpdl.inventory.InventoryUtil;
import org.hetils.mpdl.item.ItemObtainable;
import org.hetils.mpdl.item.NSK;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.hetils.mpdl.item.ItemUtil.newItemStack;
import static org.hexils.dnarch.DAManageable.ItemListGUI.ITEM_LIST_GUI;

public abstract class DAManageable extends Manageable {

    public DAManageable() { this("Manageable", true, 54); }
    public DAManageable(boolean renameable) { this(()->"Manageable", renameable, 54); }
    public DAManageable(boolean renameable, int size) { this(()->"Manageable", true, size); }
    public DAManageable(String name) { this(name, true, 54); }
    public DAManageable(String name, boolean renameable) { this(()->name, renameable, 54); }
    public DAManageable(String name, boolean renameable, int size) { this(()->name, renameable, size); }
    public DAManageable(Getter<String> name, boolean renameable) { this(name, renameable, 54); }
    public DAManageable(Getter<String> name) { this(name, true, 54); }
    public DAManageable(@NotNull Getter<String> name, boolean renameable, int size) { super(name, renameable, size); }

    @Contract(pure = true)
    public static boolean clazzUsesAny(Class<?> clazz, @NotNull Set<Class<?>> classes) {
        for (Class<?> c : classes)
            if (c.isAssignableFrom(clazz))
                return true;
        return false;
    }

    @OODPExclude
    private final Set<Class<?>> allowedClasses = new HashSet<>();

    @Override
    public final boolean onGuiClickEvent(@NotNull InventoryClickEvent event) {
        ItemStack it = event.getCurrentItem();
        DungeonMaster dm = DungeonMaster.get((Player) event.getWhoClicked());
        DAItem da = DAItem.get(it);
        if (da == this)
            return false;
        else if (da != null) {
            if (event.getAction() == InventoryAction.CLONE_STACK) {
                dm.manage(da);
                return false;
            }
            if (clazzUsesAny(da.getClass(), allowedClasses)) event.setCancelled(false);
        } else if (NSK.hasNSK(it, ITEM_LIST_GUI)) dm.manage(ItemListGUI.get(UUID.fromString(NSK.getNSK(it, ITEM_LIST_GUI))));
        return guiClickEvent(event);
    }

    public boolean guiClickEvent(InventoryClickEvent event) { return true; }

    @Override
    protected final void action(Player p, String action, @NotNull String[] args, ClickType click) { this.action(DungeonMaster.get(p), action, args, click); }
    protected void action(DungeonMaster dm, @NotNull String action, String[] args, ClickType click) {}

    protected final void allowClassesForGui(Class<?> clazz) { allowedClasses.add(clazz); }

    public static class ItemListGUI extends DAManageable implements Idable {
        public static List<ItemListGUI> instances = new ArrayList<>();
        @Contract(pure = true)
        public static @Nullable ItemListGUI get(UUID id) {
            for (ItemListGUI c : instances)
                if (c.id.equals(id))
                    return c;
            return null;
        }
        public static NSK<String, String> ITEM_LIST_GUI = new NSK<>(new NamespacedKey(Main.plugin(), "open_item_list_gui"), PersistentDataType.STRING);

        @OODPExclude
        private final Getter<? extends Set<? extends DAItem>> items;
        @OODPExclude
        private final UUID id;

        public ItemListGUI(String name, DAItem... items) { this(() -> name, null, () -> Set.of(items)); }
        public ItemListGUI(String name, Getter<? extends Set<? extends DAItem>> items) { this(() -> name, null, items); }
        public ItemListGUI(String name, ItemGenerator item_gen, DAItem... items) { this(() -> name, item_gen, () -> Set.of(items)); }
        public ItemListGUI(String name, ItemGenerator item_gen, Getter<? extends Set<? extends DAItem>> items) { this(() -> name, item_gen, items); }
        public ItemListGUI(Getter<String> name, DAItem... items) { this(name, null, () -> Set.of(items)); }
        public ItemListGUI(Getter<String> name, Getter<? extends Set<? extends DAItem>> items) { this(name, null, items); }
        public ItemListGUI(Getter<String> name, ItemGenerator item_gen, DAItem... items) { this(name, item_gen, () -> Set.of(items)); }
        public ItemListGUI(Getter<String> name, ItemGenerator item_gen, Getter<? extends Set<? extends DAItem>> items) {
            super(name);
            this.id = UUID.randomUUID();
            this.item_gen = item_gen != null ? item_gen : () -> newItemStack(
                    Material.BARREL,
                    ChatColor.GREEN + this.getName(),
                    List.of(ChatColor.GRAY + "A list of items"),
                    ITEM_LIST_GUI, this.id.toString()
            );
            this.items = items;
            instances.add(this);
        }

        @Override
        protected void createGUI() { this.setSize(54); }

        @Override
        protected void updateGUI() { if (items != null) this.fillBox(0, 9, 6, items.get().stream().map(DAItem::getItem).toList()); }

        @Override
        public boolean guiClickEvent(@NotNull InventoryClickEvent event) {
            DAItem da = DAItem.get(event.getCurrentItem());
            if (da != null) { DungeonMaster.get((Player) event.getWhoClicked()).giveItem(da); }
            return false;
        }

        public interface ItemGenerator { ItemStack genItem(); }

        private final ItemGenerator item_gen;
        public @Nullable ItemStack toItem() {
            ItemStack it = item_gen.genItem();
            NSK.setNSK(it, ITEM_LIST_GUI, this.id.toString());
            return it;
        }

        @Override
        public UUID getId() { return id; }
    }

}
