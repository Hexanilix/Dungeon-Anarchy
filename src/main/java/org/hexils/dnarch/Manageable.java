package org.hexils.dnarch;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.hetils.jgl17.oodp.OODPExclude;
import org.hetils.mpdl.GeneralListener;
import org.hetils.mpdl.InventoryUtil;
import org.hetils.mpdl.NSK;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;
import static org.hexils.dnarch.Manageable.ItemListGUI.ITEM_LIST_GUI;
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
                    m.onInvClose();
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

        @EventHandler
        public void onInventoryClick(@NotNull InventoryClickEvent event) {
            Inventory opi = event.getInventory();
            if (event.getWhoClicked() instanceof Player p) {
                DungeonMaster dm = DungeonMaster.getOrNew(p);
                for (Manageable mg : Manageable.instances)
                    if (mg.isThisGUI(opi)) {
                        event.setCancelled(true);
                        ItemStack it = event.getCurrentItem();
                        if (DAItem.get(it) == mg)
                            return;
                        if (event.getAction() == InventoryAction.CLONE_STACK) {
                            DAItem da = DAItem.get(it);
                            if (da != null)
                                da.manage(dm, mg);
                        }
                        if (mg.guiClickEvent(event)) {
                            if (NSK.hasNSK(it, ITEM_RENAME)) {
                                Manageable m = Manageable.get(opi);
                                if (m == null) mg.rename(dm, () -> dm.openInventory(opi));
                                else mg.rename(dm, () -> mg.manage(dm));
                            }
                            if (NSK.hasNSK(it, ITEM_FIELD_VALUE)) mg.setField(dm, (String) NSK.getNSK(event.getCurrentItem(), ITEM_FIELD_VALUE));
                            if (NSK.hasNSK(it, ITEM_ACTION)) mg.doAction(dm, (String) NSK.getNSK(it, ITEM_ACTION), event);
                            if (NSK.hasNSK(it, ITEM_LIST_GUI)) ItemListGUI.get(UUID.fromString((String) NSK.getNSK(it, ITEM_LIST_GUI))).manage(dm, mg);
                        }
                    }
            }
        }
    }

    @OODPExclude
    protected Inventory gui = null;
    @OODPExclude
    protected Manageable aboveManageable = null;
    @OODPExclude
    protected Manageable underManageable = null;
    @OODPExclude
    private ItemStack name_sign = null;
    private Getter<String> name;
    @OODPExclude
    public boolean is_being_renamed = false;
    private boolean renameable;
    private final String og_name;
    public static int DEF_SIZE = 54;
    @OODPExclude
    private boolean default_gui = true;

    public Manageable() { this(()->"Manageable", true, DEF_SIZE); }
    public Manageable(boolean renameable) { this(()->"Manageable", renameable, DEF_SIZE); }
    public Manageable(boolean renameable, int size) { this(()->"Manageable", renameable, size); }

    public Manageable(String name) { this(()->name, true, DEF_SIZE); }
    public Manageable(String name, boolean renameable) { this(()->name, renameable, DEF_SIZE); }
    public Manageable(String name, boolean renameable, int size) { this(()->name, renameable, size); }

    public Manageable(Getter<String> name) { this(name, true, DEF_SIZE); }
    public Manageable(Getter<String> name, boolean renameable) { this(name, renameable, DEF_SIZE); }
    public Manageable(@NotNull Getter<String> name, boolean renameable, int size) {
        this.name = name;
        this.renameable = renameable;
        setSize(size);
        this.og_name = name.get();
        instances.add(this);
    }

    public String getOGName() { return og_name; }

    public Inventory getGui() { return gui; }

    public final ItemStack genNameSign() {
        this.name_sign = newItemStack(Material.OAK_HANGING_SIGN, name.get() != null ? (ChatColor.RESET + ChatColor.WHITE.toString() + name.get()) : "null", List.of(ChatColor.GRAY + "Click to rename"), ITEM_RENAME, true);
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
        if (gui == null || default_gui) {
            this.createGUI();
            default_gui = false;
        }
        if (gui == null) {
            this.setSize(DEF_SIZE);
            default_gui = true;
        }
        this.updateGUI();
        if (gui != null) {
            dm.setCurrentManageable(this);
            dm.openInventory(gui);
        } else {
            aboveManageable = null;
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StringBuilder sb = new StringBuilder("An error occurred when attempting to open gui of Manageable \"" + this.name.get() + "\", gui is null:");
            for (StackTraceElement stackTraceElement : stackTrace)
                if (stackTraceElement.getClassName().startsWith("org.hexils") || stackTraceElement.getClassName().startsWith("org.hetils"))
                    sb.append('\n').append("\tat ").append(stackTraceElement.getClassName()).append("::").append(stackTraceElement.getMethodName()).append(" on line ").append(stackTraceElement.getLineNumber());
            sb.append('\n').append("Since you're seeing this error, please report this at https://github.com/Hexanilix/Dungeon-Anarchy-vv1.20.2/issues with a screenshot/copy of this message, and if possible a screenshot of the affected item");
            log(Level.SEVERE, sb.toString());
            dm.sendMessage(ER + "An error occurred when attempting to open gui of " + this.name.get());
        }
    }

    public String getName() { return name.get(); }

    public final void setName(String s) { setName(()->s); }
    public final void setName(Getter<String> g) {
        this.name = g;
        setSize(size);
    }

    public final void setRenameable(boolean bool) { this.renameable = bool; }
    public void rename(@NotNull DungeonMaster dm) { rename(dm, null); }
    public void rename(@NotNull DungeonMaster dm, Runnable onRename) {
        if (renameable) {
            is_being_renamed = true;
            dm.closeInventory();
            GeneralListener.confirmWithPlayer(dm.p, ChatColor.AQUA + "Please type in the new name for \"" + this.name.get() + "\" or 'cancel':", s -> {
                if (s.equalsIgnoreCase("cancel")) {
                    dm.sendMessage(W + "Cancelled.");
                    return true;
                }
                String oldn = name.get();
                String nn = s.replace(" ", "_");
                name = ()->nn;
                genNameSign();
                createGUI();
                updateGUI();
                if (dm.isManaging() && dm.getCurrentManageable() == this) manage(dm);
                if (onRename != null) onRename.run();
                is_being_renamed = false;
                dm.sendMessage(OK + "Renamed \"" + oldn + "\" to \"" + name.get() + "\"!");
                return true;
            }, () -> {
                is_being_renamed = false;
                dm.sendMessage(W + "Cancelled.");
            });
        } else dm.sendMessage(W + "This object isn't renameable.");
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

    public void onInvClose() {}

    @OODPExclude
    private int size = 0;
    @OODPExclude
    private String ln = "";
    protected final void setSize(int size) {
        if (size != 0) {
            String genned = name.get();
            if (gui == null || !ln.equals(genned))
                gui = org.hetils.mpdl.InventoryUtil.newInv(size, genned);
            else if (this.size != size) {
                Inventory i = org.hetils.mpdl.InventoryUtil.newInv(size, genned);
                for (int j = 0; j < Math.min(size, gui.getSize()); j++)
                    i.setItem(j, gui.getItem(j));
                gui = i;
            }
            this.size = size;
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
        public static NSK<String, String> ITEM_LIST_GUI = new NSK<>(new NamespacedKey(Main.plugin, "open_item_list_gui"), PersistentDataType.STRING);

        @OODPExclude
        private final Getter<Set<? extends DAItem>> items;
        @OODPExclude
        private final UUID id;

        public ItemListGUI(String name, DAItem... items) { this(() -> name, null, () -> Set.of(items)); }
        public ItemListGUI(String name, Set<? extends DAItem> items) { this(() -> name, null, () -> items); }
        public ItemListGUI(String name, ItemGenerator item_gen, DAItem... items) { this(() -> name, item_gen, () -> Set.of(items)); }
        public ItemListGUI(String name, ItemGenerator item_gen, Set<? extends DAItem> items) { this(() -> name, item_gen, () -> items); }
        public ItemListGUI(Getter<String> name, DAItem... items) { this(name, null, () -> Set.of(items)); }
        public ItemListGUI(Getter<String> name, Set<? extends DAItem> items) { this(name, null, () -> items); }
        public ItemListGUI(Getter<String> name, ItemGenerator item_gen, DAItem... items) { this(name, item_gen, () -> Set.of(items)); }
        public ItemListGUI(Getter<String> name, ItemGenerator item_gen, Getter<Set<? extends DAItem>> items) {
            super(name);
            this.id = UUID.randomUUID();
            this.item_gen = item_gen != null ? item_gen : () -> newItemStack(
                    Material.BARREL,
                    ChatColor.GREEN + name.get(),
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
    protected final int getSize() {
        return gui.getSize();
    }

    @Nullable
    protected final ItemStack getItem(int index) {
        return gui.getItem(index);
    }

    protected final int getMaxStackSize() {
        return gui.getMaxStackSize();
    }

    protected final void setStorageContents(@NotNull ItemStack[] items) throws IllegalArgumentException {
        gui.setStorageContents(items);
    }

    @NotNull
    protected final List<HumanEntity> getViewers() {
        return gui.getViewers();
    }

    protected final void setMaxStackSize(int size) {
        gui.setMaxStackSize(size);
    }

    @NotNull
    protected final HashMap<Integer, ItemStack> addItem(@NotNull ItemStack... items) throws IllegalArgumentException {
        return gui.addItem(items);
    }


    protected final void setItem(int index, @Nullable ItemStack item) {
        gui.setItem(index, item);
    }

    @NotNull
    protected final HashMap<Integer, ItemStack> removeItem(@NotNull ItemStack... items) throws IllegalArgumentException {
        return gui.removeItem(items);
    }

    protected final int firstEmpty() {
        return gui.firstEmpty();
    }

    protected final boolean isEmpty() {
        return gui.isEmpty();
    }

    protected final void clear(int index) {
        gui.clear(index);
    }

    @NotNull
    protected final ItemStack[] getStorageContents() {
        return gui.getStorageContents();
    }

    @NotNull
    protected final ItemStack[] getContents() {
        return gui.getContents();
    }

    protected final void remove(@NotNull ItemStack item) {
        gui.remove(item);
    }

    protected final void setContents(@NotNull ItemStack[] items) throws IllegalArgumentException {
        gui.setContents(items);
    }
    //</editor-fold>
}
