package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
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
import java.util.function.Consumer;
import java.util.logging.Level;

import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;
import static org.hexils.dnarch.commands.DungeonCommandExecutor.*;


public abstract class Manageable implements Deletable {
    public static Collection<Manageable> instances = new ArrayList<>();

    public static final NSK MODIFIABLE = new NSK(new NamespacedKey(Main.plugin, "gui_item-modifiable"), PersistentDataType.BOOLEAN);
    public static final NSK ITEM_FIELD_VALUE = new NSK(new NamespacedKey(Main.plugin, "gui_item-field_value"), PersistentDataType.STRING);
    public static final NSK ITEM_ACTION = new NSK(new NamespacedKey(Main.plugin, "gui_item-action"), PersistentDataType.STRING);
    public static final NSK SIGN_CHANGEABLE = new NSK(new NamespacedKey(Main.plugin, "gui_item-sign_changeable"), PersistentDataType.BOOLEAN);
    public static final NSK ITEM_RENAME = new NSK(new NamespacedKey(Main.plugin, "gui_item-prompt_rename"), PersistentDataType.BOOLEAN);

    public static void setField(ItemStack i, String field, String value) {
        NSK.setNSK(i, ITEM_FIELD_VALUE, field + " " + value);
    }

    public static void setGuiAction(ItemStack i, String action) {
        NSK.setNSK(i, ITEM_ACTION, action);
    }

    public static void setGuiAction(ItemStack i, String action, String... args) {
        NSK.setNSK(i, ITEM_ACTION, action + " " + String.join(" ", args));
    }

    public static class ConditionGUI extends Manageable implements Idable {
        public static List<ConditionGUI> instances = new ArrayList<>();
        @Contract(pure = true)
        public static @Nullable ConditionGUI get(UUID id) {
            for (ConditionGUI c : ConditionGUI.instances)
                if (c.id.equals(id))
                    return c;
            return null;
        }
        public static NSK CONDITION_GUI = new NSK(new NamespacedKey(Main.plugin, "open_condition_gui"), PersistentDataType.STRING);

        private final List<Condition> conditions;

        private final UUID id;

        public ConditionGUI(String name, List<Condition> conditions) {
            super(name);
            this.id = UUID.randomUUID();
            this.item = newItemStack(
                    Material.BARREL,
                    ChatColor.GREEN + "Events",
                    List.of(ChatColor.GRAY + "Shows the list of events this object produces"),
                    CONDITION_GUI, this.id.toString());
            this.conditions = conditions;
            instances.add(this);
        }

        @Override
        protected void createGUI() {
            this.setSize(54);
            updateGUI();
        }

        @Override
        public void updateGUI() {
            if (conditions != null) this.fillBox(0, 9, 6, conditions.stream().map(DA_item::getItem).toList());
        }

        @Override
        public boolean guiClickEvent(@NotNull InventoryClickEvent event) {
            DA_item da = DA_item.get(event.getCurrentItem());
            if (da != null) { DungeonMaster.getOrNew((Player) event.getWhoClicked()).giveItem(da); }
            return false;
        }

        private final ItemStack item;
        public @Nullable ItemStack toItem() { return item; }

        @Override
        public UUID getId() { return id; }
    }

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

    private Inventory gui = null;
    protected Manageable aboveManageable = null;
    protected Manageable underManageable = null;
    private ItemStack name_sign = null;
    private String name;

    public boolean is_being_renamed = false;
    private boolean renameable;

    public Manageable() { this("Manageable", true); }

    public Manageable(String name) { this(name, true); }

    public Manageable(String name, boolean renameable) {
        this.name = name;
        genNameSign();
        this.renameable = renameable;
        createGUI();
        instances.add(this);
    }

    public final ItemStack genNameSign() {
        this.name_sign = newItemStack(Material.OAK_HANGING_SIGN, name != null ? (ChatColor.RESET + ChatColor.WHITE.toString() + name) : "null", List.of(ChatColor.GRAY + "Click to rename"), ITEM_RENAME, true);
        return name_sign;
    }
    public void setNameSign(int index) { this.setItem(index, name_sign == null ? genNameSign() : name_sign); }

    @Contract(pure = true)
    public static @Nullable Manageable get(Inventory inventory) {
        for (Manageable m : instances)
            if (m.gui == inventory)
                return m;
        return null;
    }

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
        this.updateGUI();
        if (gui != null) {
            dm.setCurrentManageable(this);
            dm.openInventory(gui);
        }
        else {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StringBuilder sb = new StringBuilder("An error occurred when attempting to open gui of Manageable \"" + this.name + "\", gui is null:");
            for (StackTraceElement stackTraceElement : stackTrace)
                if (stackTraceElement.getClassName().startsWith("org.hexils.dnarch"))
                    sb.append('\n').append("\tat ").append(stackTraceElement.getClassName()).append("::").append(stackTraceElement.getMethodName()).append(" on line ").append(stackTraceElement.getLineNumber());
            sb.append('\n').append("Since you're seeing this error, please report this at https://github.com/Hexanilix/Dungeon-Anarchy-vv1.20.2/issues with a screenshot/copy of this message, and if possible a screenshot of the affected item");
            log(Level.SEVERE, sb.toString());
            dm.sendMessage(ER + "An error occurred when attempting to open gui of " + this.name);
        }
    }

    public String getName() { return name; }

    public final void setName(String s) {
        this.name = s;
        setSize(ls);
    }

    public final void setRenameable(boolean bool) { this.renameable = bool; }
    public void rename(@NotNull DungeonMaster dm) { rename(dm, null); }
    public void rename(@NotNull DungeonMaster dm, Runnable onRename) {
        if (renameable) {
            is_being_renamed = true;
            dm.closeInventory();
            GeneralListener.confirmWithPlayer(dm.p, ChatColor.AQUA + "Please type in the new name for \"" + this.name + "\" or 'cancel':", s -> {
                if (s.equalsIgnoreCase("cancel")) {
                    dm.sendMessage(W + "Cancelled.");
                    return true;
                }
                String oldn = name;
                name = s.replace(" ", "_");
                genNameSign();
                createGUI();
                updateGUI();
                if (dm.isManaging() && dm.getCurrentManageable() == this) manage(dm);
                if (onRename != null) onRename.run();
                is_being_renamed = false;
                dm.sendMessage(OK + "Renamed \"" + oldn + "\" to \"" + name + "\"!");
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

    protected abstract void createGUI();

    public void updateGUI() {}

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
            if (gui == null || !ln.equals(name))
                gui = org.hetils.mpdl.InventoryUtil.newInv(size, name);
            else if (ls != size) {
                Inventory i = org.hetils.mpdl.InventoryUtil.newInv(size, name);
                for (int j = 0; j < Math.min(size, gui.getSize()); j++)
                    i.setItem(j, gui.getItem(j));
                gui = i;
            }
            ls = size;
            ln = name;
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

    //<editor-fold defaultstate="collapsed" desc="Inventory (gui) delegations">
    public final int getSize() {
        return gui.getSize();
    }

    public final boolean contains(@NotNull Material material) throws IllegalArgumentException {
        return gui.contains(material);
    }

    @Nullable
    public final ItemStack getItem(int index) {
        return gui.getItem(index);
    }

    public final int getMaxStackSize() {
        return gui.getMaxStackSize();
    }

    public final void clear() {
        gui.clear();
    }

    public final void setStorageContents(@NotNull ItemStack[] items) throws IllegalArgumentException {
        gui.setStorageContents(items);
    }

    @NotNull
    public final HashMap<Integer, ? extends ItemStack> all(@Nullable ItemStack item) {
        return gui.all(item);
    }

    @NotNull
    public final List<HumanEntity> getViewers() {
        return gui.getViewers();
    }

    public final void setMaxStackSize(int size) {
        gui.setMaxStackSize(size);
    }

    public final int first(@NotNull ItemStack item) {
        return gui.first(item);
    }

    @NotNull
    public final ListIterator<ItemStack> iterator() {
        return gui.iterator();
    }

    @NotNull
    public final ListIterator<ItemStack> iterator(int index) {
        return gui.iterator(index);
    }

    @NotNull
    public final HashMap<Integer, ItemStack> addItem(@NotNull ItemStack... items) throws IllegalArgumentException {
        return gui.addItem(items);
    }

    public final boolean contains(@NotNull Material material, int amount) throws IllegalArgumentException {
        return gui.contains(material, amount);
    }

    @Nullable
    public final InventoryHolder getHolder() {
        return gui.getHolder();
    }

    public final int first(@NotNull Material material) throws IllegalArgumentException {
        return gui.first(material);
    }

    @Contract("null -> false")
    public final boolean contains(@Nullable ItemStack item) {
        return gui.contains(item);
    }

    public final void setItem(int index, @Nullable ItemStack item) {
        gui.setItem(index, item);
    }

    @NotNull
    public final HashMap<Integer, ItemStack> removeItem(@NotNull ItemStack... items) throws IllegalArgumentException {
        return gui.removeItem(items);
    }

    public final void remove(@NotNull Material material) throws IllegalArgumentException {
        gui.remove(material);
    }

    @Contract("null, _ -> false")
    public final boolean contains(@Nullable ItemStack item, int amount) {
        return gui.contains(item, amount);
    }

    public final Spliterator<ItemStack> spliterator() {
        return gui.spliterator();
    }

    public final int firstEmpty() {
        return gui.firstEmpty();
    }

    public final void forEach(Consumer<? super ItemStack> action) {
        gui.forEach(action);
    }

    @Nullable
    public final Location getLocation() {
        return gui.getLocation();
    }

    public final boolean isEmpty() {
        return gui.isEmpty();
    }

    public final void clear(int index) {
        gui.clear(index);
    }

    @NotNull
    public final HashMap<Integer, ? extends ItemStack> all(@NotNull Material material) throws IllegalArgumentException {
        return gui.all(material);
    }

    @NotNull
    public final ItemStack[] getStorageContents() {
        return gui.getStorageContents();
    }

    @NotNull
    public final ItemStack[] getContents() {
        return gui.getContents();
    }

    @Contract("null, _ -> false")
    public final boolean containsAtLeast(@Nullable ItemStack item, int amount) {
        return gui.containsAtLeast(item, amount);
    }

    public final void remove(@NotNull ItemStack item) {
        gui.remove(item);
    }

    public final void setContents(@NotNull ItemStack[] items) throws IllegalArgumentException {
        gui.setContents(items);
    }
    //</editor-fold>
}
