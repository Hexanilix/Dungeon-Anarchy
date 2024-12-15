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
    public static final NSK MODIFIABLE = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-modifiable"), PersistentDataType.BOOLEAN);
    public static final NSK ITEM_FIELD_VALUE = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-field_value"), PersistentDataType.STRING);
    public static final NSK ITEM_ACTION = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-action"), PersistentDataType.STRING);
    public static final NSK SIGN_CHANGEABLE = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-sign_changeable"), PersistentDataType.BOOLEAN);
    public static final NSK ITEM_RENAME = new NSK(new NamespacedKey("dungeon_anarchy", "gui_item-prompt_rename"), PersistentDataType.BOOLEAN);

    public static void setField(ItemStack i, String field, String value) {
        NSK.setNSK(i, ITEM_FIELD_VALUE, field + ":" + value);
    }

    public static void setGuiAction(ItemStack i, String action) {
        NSK.setNSK(i, ITEM_ACTION, action);
    }

    public static void setGuiAction(ItemStack i, String action, String... args) {
        NSK.setNSK(i, ITEM_ACTION, action + ":" + String.join(" ", args));
    }
    
    public static Collection<Manageable> instances = new ArrayList<>();

    protected void fillBox(int i, int i1, int i2, List<ItemStack> list) { InventoryUtil.fillBox(gui, i, i1, i2, list); }
    protected void fillBox(int i, int i1, int i2, ItemStack... items) { InventoryUtil.fillBox(gui, i, i1, i2, items); }
    protected void fillBox(int i, int i1, int i2, ItemStack item) { InventoryUtil.fillBox(gui, i, i1, i2, item); }
    protected void addToBox(int i, int i1, int i2, ItemStack item) { InventoryUtil.addToBox(gui, i, i1, i2, item); }
    protected ItemStack[] getBox(int i, int i1, int i2) { return InventoryUtil.getBox(gui, i, i1, i2); }

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
                    break;
                }
        }
    }

    private Inventory gui = null;
    protected Manageable aboveManageable = null;
    protected Manageable underManageable = null;
    private ItemStack name_sign = null;
    private String name = "Manageable";

    public Manageable() { instances.add(this); }

    public Manageable(String name) {
        this.name = name;
        genNameSign();
        instances.add(this);
    }

    public ItemStack genNameSign() {
        this.name_sign = newItemStack(Material.OAK_HANGING_SIGN, name != null ? (ChatColor.RESET + ChatColor.WHITE.toString() + name) : "null", List.of(ChatColor.GRAY + "Click to rename"), ITEM_RENAME, true);
        return name_sign;
    }
    public void setNameSign(int index) {
        this.setItem(index, name_sign == null ? genNameSign() : name_sign);
    }

    @Contract(pure = true)
    public static @Nullable Manageable get(Inventory inventory) {
        for (Manageable m : instances)
            if (m.gui == inventory)
                return m;
        return null;
    }

    public boolean isThisGUI(Inventory inv) { return inv == gui; }

    public final void manage(@NotNull DungeonMaster dm) {
        if (gui == null)
            this.createGUI();
        this.updateGUI();
        if (gui != null) dm.openInventory(gui);
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
    public final void manage(@NotNull DungeonMaster dm, @NotNull Manageable da) {
        da.underManageable = this;
        aboveManageable = da;
        dm.setCurrentManageable(this);
        this.manage(dm);
    }

    public String getName() {
        return name;
    }

    public void setName(String s) {
        this.name = s;
        setSize(ls);
    }

    public boolean is_being_renamed = false;
    public void rename(@NotNull DungeonMaster dm) {
        rename(dm, null);
    }

    public void rename(@NotNull DungeonMaster dm, Runnable onRename) {
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
    protected void setSize(int size) {
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

    public void delete() {
        instances.remove(this);
        gui = null;
        System.gc();
    }


    public int getSize() {
        return gui.getSize();
    }

    public boolean contains(@NotNull Material material) throws IllegalArgumentException {
        return gui.contains(material);
    }

    @Nullable
    public ItemStack getItem(int index) {
        return gui.getItem(index);
    }

    public int getMaxStackSize() {
        return gui.getMaxStackSize();
    }

    public void clear() {
        gui.clear();
    }

    public void setStorageContents(@NotNull ItemStack[] items) throws IllegalArgumentException {
        gui.setStorageContents(items);
    }

    @NotNull
    public HashMap<Integer, ? extends ItemStack> all(@Nullable ItemStack item) {
        return gui.all(item);
    }

    @NotNull
    public List<HumanEntity> getViewers() {
        return gui.getViewers();
    }

    public void setMaxStackSize(int size) {
        gui.setMaxStackSize(size);
    }

    public int first(@NotNull ItemStack item) {
        return gui.first(item);
    }

    @NotNull
    public ListIterator<ItemStack> iterator() {
        return gui.iterator();
    }

    @NotNull
    public ListIterator<ItemStack> iterator(int index) {
        return gui.iterator(index);
    }

    @NotNull
    public HashMap<Integer, ItemStack> addItem(@NotNull ItemStack... items) throws IllegalArgumentException {
        return gui.addItem(items);
    }

    public boolean contains(@NotNull Material material, int amount) throws IllegalArgumentException {
        return gui.contains(material, amount);
    }

    @Nullable
    public InventoryHolder getHolder() {
        return gui.getHolder();
    }

    public int first(@NotNull Material material) throws IllegalArgumentException {
        return gui.first(material);
    }

    @Contract("null -> false")
    public boolean contains(@Nullable ItemStack item) {
        return gui.contains(item);
    }

    public void setItem(int index, @Nullable ItemStack item) {
        gui.setItem(index, item);
    }

    @NotNull
    public HashMap<Integer, ItemStack> removeItem(@NotNull ItemStack... items) throws IllegalArgumentException {
        return gui.removeItem(items);
    }

    public void remove(@NotNull Material material) throws IllegalArgumentException {
        gui.remove(material);
    }

    @Contract("null, _ -> false")
    public boolean contains(@Nullable ItemStack item, int amount) {
        return gui.contains(item, amount);
    }

    public Spliterator<ItemStack> spliterator() {
        return gui.spliterator();
    }

    public int firstEmpty() {
        return gui.firstEmpty();
    }

    public void forEach(Consumer<? super ItemStack> action) {
        gui.forEach(action);
    }

    @Nullable
    public Location getLocation() {
        return gui.getLocation();
    }

    public boolean isEmpty() {
        return gui.isEmpty();
    }

    public void clear(int index) {
        gui.clear(index);
    }

    @NotNull
    public HashMap<Integer, ? extends ItemStack> all(@NotNull Material material) throws IllegalArgumentException {
        return gui.all(material);
    }

    @NotNull
    public ItemStack[] getStorageContents() {
        return gui.getStorageContents();
    }

    @NotNull
    public ItemStack[] getContents() {
        return gui.getContents();
    }

    @Contract("null, _ -> false")
    public boolean containsAtLeast(@Nullable ItemStack item, int amount) {
        return gui.containsAtLeast(item, amount);
    }

    public void remove(@NotNull ItemStack item) {
        gui.remove(item);
    }

    public void setContents(@NotNull ItemStack[] items) throws IllegalArgumentException {
        gui.setContents(items);
    }
}
