package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.hetils.mpdl.GeneralListener;
import org.hetils.mpdl.InventoryUtil;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;
import static org.hexils.dnarch.commands.DungeonCommandExecutor.*;


public abstract class Manageable implements Deletable {
    public static Collection<Manageable> instances = new ArrayList<>();
    @Contract(pure = true)
    public static @Nullable Manageable get(Inventory inventory) {
        for (Manageable m : instances)
            if (m.gui == inventory)
                return m;
        return null;
    }

    public static final NSK<Byte, Boolean> MODIFIABLE = new NSK<>(new NamespacedKey(Main.plugin, "gui_item-modifiable"), PersistentDataType.BOOLEAN);
    public static final NSK<String, String> ITEM_FIELD_VALUE = new NSK<>(new NamespacedKey(Main.plugin, "gui_item-field_value"), PersistentDataType.STRING);
    public static final NSK<String, String> ITEM_ACTION = new NSK<>(new NamespacedKey(Main.plugin, "gui_item-action"), PersistentDataType.STRING);
    public static final NSK<Byte, Boolean> SIGN_CHANGEABLE = new NSK<>(new NamespacedKey(Main.plugin, "gui_item-sign_changeable"), PersistentDataType.BOOLEAN);
    public static final NSK<Byte, Boolean> ITEM_RENAME = new NSK<>(new NamespacedKey(Main.plugin, "gui_item-prompt_rename"), PersistentDataType.BOOLEAN);

    public static void setField(ItemStack i, String field, String value) { NSK.setNSK(i, ITEM_FIELD_VALUE, field + " " + value); }

    public static void setGuiAction(ItemStack i, String action) { NSK.setNSK(i, ITEM_ACTION, action); }

    public static void setGuiAction(ItemStack i, String action, String... args) { NSK.setNSK(i, ITEM_ACTION, action + " " + String.join(" ", args)); }

    public static class ManagableListener implements Listener {
        @EventHandler
        public void onInvClose(@NotNull InventoryCloseEvent event) {
            Inventory inv = event.getInventory();
            for (Manageable m : instances)
                if (m.gui == inv) {
                    if (m.underManageable == null && m.aboveManageable != null && !m.is_being_renamed) {
                        m.aboveManageable.underManageable = null;
                        Manageable am = m.aboveManageable;
                        m.aboveManageable = null;
                        new BukkitRunnable() { @Override public void run() { am.manage(DungeonMaster.getOrNew((Player) event.getPlayer())); } }.runTaskLater(Main.plugin, 1);
                    }
                    DungeonMaster.getOrNew((Player) event.getPlayer()).setCurrentManageable(null);
                    break;
                }
        }
    }

    public interface NameGetter { String getName(); }

    private Inventory gui = null;
    protected Manageable aboveManageable = null;
    protected Manageable underManageable = null;
    private ItemStack name_sign = null;
    private NameGetter name;
    public boolean is_being_renamed = false;
    private boolean renameable;

    public Manageable() { this(()->"Manageable", true, 0); }
    public Manageable(boolean renameable) { this(()->"Manageable", renameable, 0); }
    public Manageable(boolean renameable, int size) { this(()->"Manageable", renameable, size); }

    public Manageable(String name) { this(()->name, true, 0); }
    public Manageable(String name, boolean renameable) { this(()->name, renameable, 0); }
    public Manageable(String name, boolean renameable, int size) { this(()->name, renameable, size); }

    public Manageable(NameGetter name) { this(name, true, 0); }
    public Manageable(NameGetter name, boolean renameable) { this(name, renameable, 0); }
    public Manageable(NameGetter name, boolean renameable, int size) {
        this.name = name;
        this.renameable = renameable;
        setSize(size);
        instances.add(this);
    }

    public final ItemStack genNameSign() {
        this.name_sign = newItemStack(Material.OAK_HANGING_SIGN, name.getName() != null ? (ChatColor.RESET + ChatColor.WHITE.toString() + name.getName()) : "null", List.of(ChatColor.GRAY + "Click to rename"), ITEM_RENAME, true);
        return name_sign;
    }
    public void setNameSign(int index) { this.setItem(index, name_sign == null ? genNameSign() : name_sign); }

    public final boolean isThisGUI(Inventory inv) { return inv == gui; }

    public final void manage(@NotNull DungeonMaster dm, @NotNull Manageable da) {
        da.underManageable = this;
        aboveManageable = da;
        this.manage(dm);
    }
    public final void manage(@NotNull DungeonMaster dm) {
        if (dm.getCurrentManageable() == this) return;
        if (gui == null)
            this.createGUI();
        if (gui == null)
            this.setSize(27);
        this.updateGUI();
        if (gui != null) {
            dm.setCurrentManageable(this);
            dm.openInventory(gui);
        }
        else {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StringBuilder sb = new StringBuilder("An error occurred when attempting to open gui of Manageable \"" + this.name.getName() + "\", gui is null:");
            for (StackTraceElement stackTraceElement : stackTrace)
                if (stackTraceElement.getClassName().startsWith("org.hexils.dnarch"))
                    sb.append('\n').append("\tat ").append(stackTraceElement.getClassName()).append("::").append(stackTraceElement.getMethodName()).append(" on line ").append(stackTraceElement.getLineNumber());
            sb.append('\n').append("Since you're seeing this error, please report this at https://github.com/Hexanilix/Dungeon-Anarchy-vv1.20.2/issues with a screenshot/copy of this message, and if possible a screenshot of the affected item");
            log(Level.SEVERE, sb.toString());
            dm.sendMessage(ER + "An error occurred when attempting to open gui of " + this.name.getName());
        }
    }

    public String getName() { return name.getName(); }

    public final void setName(String s) {
        this.name = ()->s;
        setSize(ls);
    }

    public final void setRenameable(boolean bool) { this.renameable = bool; }
    public void rename(@NotNull DungeonMaster dm) { rename(dm, null); }
    public void rename(@NotNull DungeonMaster dm, Runnable onRename) {
        if (renameable) {
            is_being_renamed = true;
            dm.closeInventory();
            GeneralListener.confirmWithPlayer(dm.p, ChatColor.AQUA + "Please type in the new name for \"" + this.name.getName() + "\" or 'cancel':", s -> {
                if (s.equalsIgnoreCase("cancel")) {
                    dm.sendMessage(W + "Cancelled.");
                    return true;
                }
                String oldn = name.getName();
                String nn = s.replace(" ", "_");
                name = ()->nn;
                genNameSign();
                createGUI();
                updateGUI();
                if (dm.isManaging() && dm.getCurrentManageable() == this) manage(dm);
                if (onRename != null) onRename.run();
                is_being_renamed = false;
                dm.sendMessage(OK + "Renamed \"" + oldn + "\" to \"" + name.getName() + "\"!");
                return true;
            }, () -> {
                is_being_renamed = false;
                dm.sendMessage(W + "Cancelled.");
            });
        }
    }

    public final void doAction(DungeonMaster dm, String command, InventoryClickEvent event) {
        if (command != null) {
            String[] v = command.split(" ", 2);
            action(dm, v[0], v.length > 1 ? v[1].split(" ") : new String[]{}, event);
        }
    }

    protected void createGUI() {}

    protected void updateGUI() {}

    public final void setField(DungeonMaster dm, String value) {
        if (value != null) {
            String[] v = value.split(" ", 2);
            if (v.length > 1)
                changeField(dm, v[0], v[1]);
        }
    }

    protected void changeField(DungeonMaster dm, @NotNull String field, String value) {}

    protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {}

    public boolean guiClickEvent(InventoryClickEvent event) {return true;}

    private int ls = 0;
    private String ln = "";
    protected final void setSize(int size) {
        if (size != 0) {
            String genned = name.getName();
            if (gui == null || !ln.equals(genned))
                gui = org.hetils.mpdl.InventoryUtil.newInv(size, genned);
            else if (ls != size) {
                Inventory i = org.hetils.mpdl.InventoryUtil.newInv(size, genned);
                for (int j = 0; j < Math.min(size, gui.getSize()); j++)
                    i.setItem(j, gui.getItem(j));
                gui = i;
            }
            ls = size;
            ln = genned;
        }
    }

    public void delete() {
        instances.remove(this);
        gui = null;
        System.gc();
    }

    protected final void fillBox(int i, int i1, int i2, List<ItemStack> list) { InventoryUtil.fillBox(gui, i, i1, i2, list); }
    protected final void fillBox(int i, int i1, int i2, ItemStack... items) { InventoryUtil.fillBox(gui, i, i1, i2, items); }
    protected final void fillBox(int i, int i1, int i2, ItemStack item) { InventoryUtil.fillBox(gui, i, i1, i2, item); }
    protected final void addToBox(int i, int i1, int i2, ItemStack item) { InventoryUtil.addToBox(gui, i, i1, i2, item); }
    protected final ItemStack[] getBox(int i, int i1, int i2) { return InventoryUtil.getBox(gui, i, i1, i2); }

    public static class ItemListGUI extends Manageable implements Idable {
        public static List<ItemListGUI> instances = new ArrayList<>();
        @Contract(pure = true)
        public static @Nullable Manageable.ItemListGUI get(UUID id) {
            for (ItemListGUI c : ItemListGUI.instances)
                if (c.id.equals(id))
                    return c;
            return null;
        }
        public static NSK ITEM_LIST_GUI = new NSK(new NamespacedKey(Main.plugin, "open_item_list_gui"), PersistentDataType.STRING);

        private final List<DA_item> items;

        private final UUID id;

        public ItemListGUI(String name, DA_item... items) { this(name, null, Arrays.asList(items)); }
        public ItemListGUI(String name, List<DA_item> items) { this(name, null, items); }
        public ItemListGUI(String name, ItemGenerator item_gen, DA_item... items) { this(name, item_gen, Arrays.asList(items)); }
        public ItemListGUI(String name, ItemGenerator item_gen, List<DA_item> items) {
            super(name);
            this.id = UUID.randomUUID();
            this.item_gen = item_gen != null ? item_gen : () -> newItemStack(
                    Material.BARREL,
                    ChatColor.GREEN + name,
                    List.of(ChatColor.GRAY + "A list of items"),
                    ITEM_LIST_GUI, this.id.toString()
            );
            this.items = items;
            instances.add(this);
        }

        @Override
        protected void createGUI() { this.setSize(54); }

        @Override
        protected void updateGUI() { if (items != null) this.fillBox(0, 9, 6, items.stream().map(DA_item::getItem).toList()); }

        @Override
        public boolean guiClickEvent(@NotNull InventoryClickEvent event) {
            DA_item da = DA_item.get(event.getCurrentItem());
            if (da != null) { DungeonMaster.getOrNew((Player) event.getWhoClicked()).giveItem(da); }
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

    //<editor-fold defaultstate="collapsed" desc="Inventory (gui) delegations">
    public final int getSize() {
        return gui.getSize();
    }

    @Nullable
    public final ItemStack getItem(int index) {
        return gui.getItem(index);
    }

    public final int getMaxStackSize() {
        return gui.getMaxStackSize();
    }

    public final void setStorageContents(@NotNull ItemStack[] items) throws IllegalArgumentException {
        gui.setStorageContents(items);
    }

    @NotNull
    public final List<HumanEntity> getViewers() {
        return gui.getViewers();
    }

    public final void setMaxStackSize(int size) {
        gui.setMaxStackSize(size);
    }

    @NotNull
    public final HashMap<Integer, ItemStack> addItem(@NotNull ItemStack... items) throws IllegalArgumentException {
        return gui.addItem(items);
    }


    public final void setItem(int index, @Nullable ItemStack item) {
        gui.setItem(index, item);
    }

    @NotNull
    public final HashMap<Integer, ItemStack> removeItem(@NotNull ItemStack... items) throws IllegalArgumentException {
        return gui.removeItem(items);
    }

    public final int firstEmpty() {
        return gui.firstEmpty();
    }

    public final boolean isEmpty() {
        return gui.isEmpty();
    }

    public final void clear(int index) {
        gui.clear(index);
    }

    @NotNull
    public final ItemStack[] getStorageContents() {
        return gui.getStorageContents();
    }

    @NotNull
    public final ItemStack[] getContents() {
        return gui.getContents();
    }

    public final void remove(@NotNull ItemStack item) {
        gui.remove(item);
    }

    public final void setContents(@NotNull ItemStack[] items) throws IllegalArgumentException {
        gui.setContents(items);
    }
    //</editor-fold>
}
