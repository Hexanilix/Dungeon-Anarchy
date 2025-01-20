package org.hexils.dnarch;

import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.entity.*;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.*;
import org.bukkit.map.MapView;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.hetils.jgl17.Pair;
import org.hetils.mpdl.PluginThread;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.hetils.mpdl.ItemUtil.newItemStack;

public class DungeonMaster {
    public static final Set<DungeonMaster> dms = new HashSet<>();
    public static @NotNull DungeonMaster getOrNew(Player p) {
        for (DungeonMaster m : dms)
            if (m.p == p)
                return m;
        return new DungeonMaster(p);
    }

    public static Set<UUID> permittedPlayers = new HashSet<>();


    private final Pair<Location, Location> selected_area = new Pair<>();
    private final Pair<Location, Location> raw_selected_area = new Pair<>();
    private final List<Block> slb = new ArrayList<>();
    private double ppm = 5;
    private SelectThread select_thread = new SelectThread();
    private Dungeon current_dungeon = null;
    private boolean build_mode = false;
    private Manageable current_manageable;
    public final Player p;

    public DungeonMaster(Player p) {
        this.p = p;
        dms.add(this);
        this.select_thread.start();
    }

    public static boolean isPermitted(@NotNull Player p) { return p.isOp() || isPermitted(p.getUniqueId()); }
    public static boolean isPermitted(UUID id) { return permittedPlayers.contains(id); }

    public boolean setSelectionA(Block b) {
        Location key = b != null ? b.getLocation() : null;
        if (key != null && key.equals(raw_selected_area.key()))
            return false;
        else {
            raw_selected_area.setKey(key);
            if (key != null && raw_selected_area.value() != null && key.getWorld() != raw_selected_area.value().getWorld())
                raw_selected_area.setValue(null);
            selected_area.set(org.hetils.mpdl.LocationUtil.toMaxMin(raw_selected_area));
            return true;
        }
    }
    public boolean setSelectionB(Block b) {
        Location value = b != null ? b.getLocation() : null;
        if (value != null && value.equals(raw_selected_area.value()))
            return false;
        else {
            raw_selected_area.setValue(value);
            if (value != null && raw_selected_area.key() != null && value.getWorld() != raw_selected_area.key().getWorld())
                raw_selected_area.setKey(null);
            selected_area.set(org.hetils.mpdl.LocationUtil.toMaxMin(raw_selected_area));
            return true;
        }
    }
    public Location getSelectionA() { return getSelectedArea().key(); }
    public Location getSelectionB() { return getSelectedArea().value(); }
    public void clearSelectionA() {
        raw_selected_area.setKey(null);
        selected_area.set(org.hetils.mpdl.LocationUtil.toMaxMin(raw_selected_area));
    }
    public void clearSelectionB() {
        raw_selected_area.setValue(null);
        selected_area.set(org.hetils.mpdl.LocationUtil.toMaxMin(raw_selected_area));
    }
    public Pair<Location, Location> getSelectedArea() { return new Pair<>(selected_area.key() != null ? selected_area.key().clone().add(1, 1, 1) : null, selected_area.value()); }
    public void clearSelection() {
        this.raw_selected_area.setKey(null);
        this.raw_selected_area.setValue(null);
        this.selected_area.setKey(null);
        this.selected_area.setValue(null);
    }
    public boolean hasAreaSelected() { return selected_area.key() != null && selected_area.value() != null; }

    public List<Block> getSelectedBlocks() {
        List<Block> l = new ArrayList<>();
        l.addAll(slb);
        l.addAll(getSelectionBlocks());
        return l;
    }
    public boolean selectBlock(Block b) {
        if (this.isEditing()) {
            if (!slb.contains(b)) {
                if (!this.current_dungeon.contains(b.getLocation())) {
                    p.sendMessage(W + "Block is out of dungeon bounds!");
                } else {
                    if (!slb.isEmpty() && slb.get(0).getWorld() != b.getWorld())
                        slb.clear();
                    slb.add(b);
                    return true;
                }
            }
        }
        return false;
    }
    public boolean deselectBlock(Block b) { return slb.remove(b); }
    public void deselectBlocks() {
        this.slb.clear();
        this.clearSelection();
    }

    public List<Block> getSelectionBlocks() {
        List<Block> c = new ArrayList<>();
        if (raw_selected_area.key() != null && raw_selected_area.value() != null) {
            World w = raw_selected_area.key().getWorld();
            int xm = (int) Math.max(raw_selected_area.key().getX(), raw_selected_area.value().getX());
            int ym = (int) Math.max(raw_selected_area.key().getY(), raw_selected_area.value().getY());
            int zm = (int) Math.max(raw_selected_area.key().getZ(), raw_selected_area.value().getZ());
            int mx = (int) Math.min(raw_selected_area.key().getX(), raw_selected_area.value().getX());
            int my = (int) Math.min(raw_selected_area.key().getY(), raw_selected_area.value().getY());
            int mz = (int) Math.min(raw_selected_area.key().getZ(), raw_selected_area.value().getZ());
            for (int x = mx; x <= xm; x++)
                for (int y = my; y <= ym; y++)
                    for (int z = mz; z <= zm; z++) {
                        Block b = w.getBlockAt(x, y, z);
                        if (current_dungeon.contains(b.getLocation())) c.add(b);
                    }
        }
        return c;
    }

    public boolean isEditing() { return current_dungeon != null; }
    public void setCurrentDungeon(Dungeon dungeon) {
        if (dungeon != null && dungeon != this.current_dungeon) {
            dungeon.addEditor(this);
            this.current_dungeon = dungeon;
            this.sendMessage(DungeonMaster.Sender.CREATOR, OK + "Editing dungeon " + current_dungeon.getName());
        }
    }
    public Dungeon getCurrentDungeon() { return current_dungeon; }

    public void giveItem(DAItem a) { if (a != null) this.p.getInventory().addItem(a.getItem()); }

    public boolean inBuildMode() { return build_mode && current_dungeon != null; }
    public void setBuildMode(boolean b) { this.build_mode = b; }

    public boolean hasBlocksSelected() { return !slb.isEmpty(); }

    public boolean isManaging() { return current_manageable != null; }
    public void setCurrentManageable(Manageable manageable) { this.current_manageable = manageable; }
    public Manageable getCurrentManageable() { return current_manageable; }

    private final class SelectThread extends PluginThread {
        private final Map<Object, SectionSelect> selects = new HashMap<>();
        public void addSelect(Object o, SectionSelect select) {
            SectionSelect sl = selects.get(o);
            if (sl != null) sl.halt();
            selects.put(o, select);
            select.start();
        }
        public boolean clearSelection(Object o) {
            SectionSelect sl = selects.get(o);
            if (sl != null)
                if (sl.isRunning()) {
                    sl.halt();
                    return true;
                }
            return false;
        }
        public void clearSelected() {
            selects.values().forEach(PluginThread::halt);
            selects.clear();
        }

        private Particle msp = Particle.COMPOSTER;
        SectionSelect secs = new SectionSelect(getSelectedArea(), msp);
        Location lk = getSelectedArea().key();
        Location lv = getSelectedArea().value();
        @Override
        public void run() {
            secs.start();
            try {
                while (r) {
                    slb.forEach(b -> selectBlock(b.getLocation(), 5));
                    Thread.sleep(100);
                    Pair<Location, Location> area = getSelectedArea();
                    if (area.key() != lk || area.value() != lv) {
                        lk = area.key();
                        lv = area.value();
                        secs.setSec(area);
                    }
                }
            } catch (InterruptedException ignore) {}
        }
    }
    public void selectBlock(@NotNull Location l, int ppm) {
        double x = (int) l.getX();
        double y = (int) l.getY();
        double z = (int) l.getZ();
        p.spawnParticle(Particle.COMPOSTER, x+1, y+1, z+1, 1, 0, 0, 0, 0);
        for (double i = 0; i < 1; i+= 1d/ppm) {
            p.spawnParticle(Particle.COMPOSTER, x+i, y, z, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+i, y+1, z, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+i, y, z+1, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+i, y+1, z+1, 1, 0, 0, 0, 0);

            p.spawnParticle(Particle.COMPOSTER, x, y, z+i, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x, y+1, z+i, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+1, y, z+i, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+1, y+1, z+i, 1, 0, 0, 0, 0);

            p.spawnParticle(Particle.COMPOSTER, x, y+i, z, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x, y+i, z+1, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+1, y+i, z, 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.COMPOSTER, x+1, y+i, z+1, 1, 0, 0, 0, 0);
        }
    }

    private class SectionSelect extends PluginThread {
        private Particle par;
        public SectionSelect(Pair<Location, Location> sec) {
            setSec(sec);
            this.par = Particle.COMPOSTER;
        }
        public SectionSelect(Pair<Location, Location> sec, Particle par) {
            setSec(sec);
            this.par = par;
        }
        double xm = 0;
        double ym = 0;
        double zm = 0;
        double mx = 0;
        double my = 0;
        double mz = 0;
        double xd = 0;
        double yd = 0;
        double zd = 0;
        @Override
        public void run() {
            try {
                while (r) {
                    double ppmc = 1d / ppm;
                    for (double i = 0; i < xd; i += ppmc) {
                        p.spawnParticle(par, mx + i, my, mz, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + i, my, mz + zd, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + i, my + yd, mz + zd, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + i, my + yd, mz, 1, 0, 0, 0, 0);
                    }
                    for (double i = 0; i < yd; i += ppmc) {
                        p.spawnParticle(par, mx, my + i, mz, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + xd, my + i, mz, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + xd, my + i, mz + zd, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx, my + i, mz + zd, 1, 0, 0, 0, 0);
                    }
                    for (double i = 0; i < zd; i += ppmc) {
                        p.spawnParticle(par, mx, my, mz + i, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + xd, my, mz + i, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx + xd, my + yd, mz + i, 1, 0, 0, 0, 0);
                        p.spawnParticle(par, mx, my + yd, mz + i, 1, 0, 0, 0, 0);
                    }
                    Thread.sleep(100);
                }
            } catch (InterruptedException ignore) {}
        }

        public void setSec(Pair<Location, Location> sec) {
            if (sec != null && sec.key() != null && sec.value() != null) {
                xm = Math.max(sec.key().getX(), sec.value().getX());
                ym = Math.max(sec.key().getY(), sec.value().getY());
                zm = Math.max(sec.key().getZ(), sec.value().getZ());
                mx = Math.min(sec.key().getX(), sec.value().getX());
                my = Math.min(sec.key().getY(), sec.value().getY());
                mz = Math.min(sec.key().getZ(), sec.value().getZ());
                xd = xm - mx;
                yd = ym - my;
                zd = zm - mz;
            } else {
                xm = 0;
                ym = 0;
                zm = 0;
                mx = 0;
                my = 0;
                mz = 0;
                xd = 0;
                yd = 0;
                zd = 0;
            }
        }

        public void setPar(Particle par) { this.par = par; }
    }
    public void select(Object o, Pair<Location, Location> sec) { this.select_thread.addSelect(o, new SectionSelect(sec)); }
    public void select(Object o, Pair<Location, Location> sec, Particle p) { this.select_thread.addSelect(o, new SectionSelect(sec, p)); }
    public boolean hideSelection(Object o) { return select_thread.clearSelection(o); }
    public void hideSelections() { select_thread.clearSelected(); }

    public void promptRemove(Manageable m) {
        Manageable mg = new Manageable() {

            @Override
            protected void createGUI() {
                setSize(27);
                setItem(4, newItemStack(Material.PAPER, "Are you sure you want to remove:"));
                ItemStack di;
                if (m instanceof DAItem da) di = da.getItem();
                else di = newItemStack(Material.GREEN_CONCRETE, m.getName());
                setItem(13, di);
                setItem(11, newItemStack(Material.GREEN_CONCRETE, ChatColor.GREEN + "Yes", ITEM_ACTION, "delete"));
                setItem(15, newItemStack(Material.RED_CONCRETE, ChatColor.RED + "No", ITEM_ACTION, "cancel"));
            }

            @Override
            protected void action(DungeonMaster dm, String action, String[] args, InventoryClickEvent event) {
                if (Objects.equals(action, "delete")) {
                    if (DAItem.get(DungeonMaster.this.getInventory().getItemInMainHand()) == m) DungeonMaster.this.getInventory().setItemInMainHand(null);
                    m.delete();
                }
                DungeonMaster.this.closeInventory();
            }
        };
        mg.manage(this, m);
    }


    public void sendMessage(@Nullable UUID sender, @NotNull String... messages) { p.sendMessage(sender, messages); }
    public void sendMessage(@Nullable UUID sender, @NotNull String message) { p.sendMessage(sender, message); }
    public void sendMessage(@NotNull String... messages) { p.sendMessage(messages); }

    public static ChatColor IF = ChatColor.AQUA;
    public static ChatColor OK = ChatColor.GREEN;
    public static ChatColor W = ChatColor.YELLOW;
    public static ChatColor ER = ChatColor.RED;

    public enum Sender {
        CREATOR(ChatColor.GOLD),
        DEBUG(ChatColor.LIGHT_PURPLE);

        private ChatColor color;
        Sender(ChatColor c) { color = c; }

        @Override
        public @NotNull String toString() {
            return this.name().charAt(0) + this.name().toLowerCase().substring(1);
        }
    }

    public static String ABREV = "DA";

    @Contract(pure = true)
    public static @NotNull String getMessage(@NotNull String message) { return ("["+ABREV+"] " + ChatColor.RESET + message); }
    @Contract(pure = true)
    public static @NotNull String getMessage(Sender s, @NotNull String message) { return ((s != null ? s.color + "["+ABREV+"->" + s + "] " : "["+ABREV+"] ") + ChatColor.RESET + message); }
    public static @NotNull String getMessage(Object s, @NotNull String message) { return ((s != null ? "["+ABREV+"->" + s.getClass().getName() + "] " : "["+ABREV+"->Unknown] ") + ChatColor.RESET + message); }

    public void sendMessage(String message) { p.sendMessage(getMessage(message)); }
    public void sendMessage(Sender s, String message) { p.sendMessage(getMessage(s, message)); }
    public void sendInfo(@NotNull String message) { p.sendMessage(getMessage(IF + message)); }
    public void sendWarning(@NotNull String message) { p.sendMessage(getMessage(W + message)); }
    public void sendError(@NotNull String message) { p.sendMessage(getMessage(ER + message)); }
    public void sendInfo(Sender s, @NotNull String message) { p.sendMessage(getMessage(s, IF + message)); }
    public void sendWarning(Sender s, @NotNull String message) { p.sendMessage(getMessage(s, W + message)); }
    public void sendError(Sender s, @NotNull String message) { p.sendMessage(getMessage(s, ER + message)); }

    //<editor-fold defaultstate="collapsed" desc="Player (p) delegations">
    @NotNull
    public String getName() {
        return p.getName();
    }

    public boolean isOp() {
        return p.isOp();
    }

    public boolean isOnline() {
        return p.isOnline();
    }

    public int getStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {
        return p.getStatistic(statistic, entityType);
    }

    public long getPlayerTime() {
        return p.getPlayerTime();
    }

    public float getExp() {
        return p.getExp();
    }

    public void openSign(@NotNull Sign sign) {
        p.openSign(sign);
    }

    public void stopAllSounds() {
        p.stopAllSounds();
    }

    public int getMaxFireTicks() {
        return p.getMaxFireTicks();
    }

    @Nullable
    public Location getBedSpawnLocation() {
        return p.getBedSpawnLocation();
    }

    @Deprecated
    public void setMaxHealth(double health) {
        p.setMaxHealth(health);
    }

    @NotNull
    public <T extends Projectile> T launchProjectile(@NotNull Class<? extends T> projectile) {
        return p.launchProjectile(projectile);
    }

    @NotNull
    public Map<String, Object> serialize() {
        return p.serialize();
    }

    @NotNull
    public ItemStack getItemOnCursor() {
        return p.getItemOnCursor();
    }

    @NotNull
    public List<Block> getLineOfSight(@Nullable Set<Material> transparent, int maxDistance) {
        return p.getLineOfSight(transparent, maxDistance);
    }

    public void setMetadata(@NotNull String metadataKey, @NotNull MetadataValue newMetadataValue) {
        p.setMetadata(metadataKey, newMetadataValue);
    }

    @NotNull
    @Deprecated
    public ItemStack getItemInHand() {
        return p.getItemInHand();
    }

    public void setGliding(boolean gliding) {
        p.setGliding(gliding);
    }

    public void setRemoveWhenFarAway(boolean remove) {
        p.setRemoveWhenFarAway(remove);
    }

    public boolean isSilent() {
        return p.isSilent();
    }

    public void sendBlockDamage(@NotNull Location loc, float progress, @NotNull Entity source) {
        p.sendBlockDamage(loc, progress, source);
    }

    public boolean teleport(@NotNull Entity destination, @NotNull PlayerTeleportEvent.TeleportCause cause) {
        return p.teleport(destination, cause);
    }

    public void removeAttachment(@NotNull PermissionAttachment attachment) {
        p.removeAttachment(attachment);
    }

    @Nullable
    public InventoryView openMerchant(@NotNull Merchant merchant, boolean force) {
        return p.openMerchant(merchant, force);
    }

    public void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ) {
        p.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ);
    }

    public void setSaturation(float value) {
        p.setSaturation(value);
    }

    public void playSound(@NotNull Location location, @NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch) {
        p.playSound(location, sound, category, volume, pitch);
    }

    @Nullable
    public Block getTargetBlockExact(int maxDistance) {
        return p.getTargetBlockExact(maxDistance);
    }

    public boolean isGlowing() {
        return p.isGlowing();
    }

    public boolean isFlying() {
        return p.isFlying();
    }

    public int getExpToLevel() {
        return p.getExpToLevel();
    }

    @Deprecated
    public void hidePlayer(@NotNull Player player) {
        p.hidePlayer(player);
    }

    @NotNull
    public Location getLocation() {
        return p.getLocation();
    }

    public void playSound(@NotNull Entity entity, @NotNull String sound, @NotNull SoundCategory category, float volume, float pitch, long seed) {
        p.playSound(entity, sound, category, volume, pitch, seed);
    }

    @NotNull
    public Sound getDrinkingSound(@NotNull ItemStack itemStack) {
        return p.getDrinkingSound(itemStack);
    }

    public void sendHealthUpdate() {
        p.sendHealthUpdate();
    }

    public void setVelocity(@NotNull Vector velocity) {
        p.setVelocity(velocity);
    }

    public void loadData() {
        p.loadData();
    }

    public long getLastPlayed() {
        return p.getLastPlayed();
    }

    public boolean isCustomNameVisible() {
        return p.isCustomNameVisible();
    }

    public void playHurtAnimation(float yaw) {
        p.playHurtAnimation(yaw);
    }

    public void setHealthScale(double scale) throws IllegalArgumentException {
        p.setHealthScale(scale);
    }

    @Nullable
    public InventoryView openInventory(@NotNull Inventory inventory) {
        return p.openInventory(inventory);
    }

    @NotNull
    public BlockFace getFacing() {
        return p.getFacing();
    }

    public boolean isSprinting() {
        return p.isSprinting();
    }

    @Nullable
    public String getPlayerListHeader() {
        return p.getPlayerListHeader();
    }

    @Nullable
    public Sound getHurtSound() {
        return p.getHurtSound();
    }

    public void resetPlayerTime() {
        p.resetPlayerTime();
    }

    public void incrementStatistic(@NotNull Statistic statistic, int amount) throws IllegalArgumentException {
        p.incrementStatistic(statistic, amount);
    }

    public void setAI(boolean ai) {
        p.setAI(ai);
    }

    public boolean isPersistent() {
        return p.isPersistent();
    }

    public int getMaximumAir() {
        return p.getMaximumAir();
    }

    public void setNoDamageTicks(int ticks) {
        p.setNoDamageTicks(ticks);
    }

    public int getSaturatedRegenRate() {
        return p.getSaturatedRegenRate();
    }

    public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {
        p.decrementStatistic(statistic, material);
    }

    public void incrementStatistic(@NotNull Statistic statistic) throws IllegalArgumentException {
        p.incrementStatistic(statistic);
    }

    @NotNull
    public PlayerInventory getInventory() {
        return p.getInventory();
    }

    @Deprecated
    public boolean addPotionEffect(@NotNull PotionEffect effect, boolean force) {
        return p.addPotionEffect(effect, force);
    }

    public void setFallDistance(float distance) {
        p.setFallDistance(distance);
    }

    @NotNull
    public Sound getSwimHighSpeedSplashSound() {
        return p.getSwimHighSpeedSplashSound();
    }

    public void setWalkSpeed(float value) throws IllegalArgumentException {
        p.setWalkSpeed(value);
    }

    public void giveExp(int amount) {
        p.giveExp(amount);
    }

    public void playSound(@NotNull Location location, @NotNull Sound sound, float volume, float pitch) {
        p.playSound(location, sound, volume, pitch);
    }

    @NotNull
    public Set<UUID> getCollidableExemptions() { return p.getCollidableExemptions(); }

    public void showDemoScreen() {
        p.showDemoScreen();
    }

    public void playSound(@NotNull Location location, @NotNull String sound, @NotNull SoundCategory category, float volume, float pitch) {
        p.playSound(location, sound, category, volume, pitch);
    }

    public double getWidth() {
        return p.getWidth();
    }

    public void setFreezeTicks(int ticks) {
        p.setFreezeTicks(ticks);
    }

    @Nullable
    public Entity getVehicle() {
        return p.getVehicle();
    }

    @Deprecated
    public void setTexturePack(@NotNull String url) {
        p.setTexturePack(url);
    }

    public void resetPlayerWeather() {
        p.resetPlayerWeather();
    }

    public void setEnchantmentSeed(int seed) {
        p.setEnchantmentSeed(seed);
    }

    public void setTicksLived(int value) {
        p.setTicksLived(value);
    }

    public boolean isInWater() {
        return p.isInWater();
    }

    public boolean isBlocking() {
        return p.isBlocking();
    }

    public void setArrowCooldown(int ticks) {
        p.setArrowCooldown(ticks);
    }

    public boolean isInvisible() {
        return p.isInvisible();
    }

    public double getAbsorptionAmount() {
        return p.getAbsorptionAmount();
    }

    @NotNull
    public String getPlayerListName() {
        return p.getPlayerListName();
    }

    public void playSound(@NotNull Location location, @NotNull String sound, float volume, float pitch) {
        p.playSound(location, sound, volume, pitch);
    }

    @ApiStatus.Experimental
    @NotNull
    public Set<Player> getTrackedBy() {
        return p.getTrackedBy();
    }

    public boolean canBreatheUnderwater() {
        return p.canBreatheUnderwater();
    }

    @Nullable
    public EntityDamageEvent getLastDamageCause() {
        return p.getLastDamageCause();
    }

    @Nullable
    public GameMode getPreviousGameMode() {
        return p.getPreviousGameMode();
    }

    public boolean removeScoreboardTag(@NotNull String tag) {
        return p.removeScoreboardTag(tag);
    }

    public double getLastDamage() {
        return p.getLastDamage();
    }

    public int discoverRecipes(@NotNull Collection<NamespacedKey> recipes) {
        return p.discoverRecipes(recipes);
    }

    public void setUnsaturatedRegenRate(int ticks) {
        p.setUnsaturatedRegenRate(ticks);
    }

    public boolean isPermissionSet(@NotNull String name) {
        return p.isPermissionSet(name);
    }

    public void setStatistic(@NotNull Statistic statistic, @NotNull Material material, int newValue) throws IllegalArgumentException {
        p.setStatistic(statistic, material, newValue);
    }

    public int undiscoverRecipes(@NotNull Collection<NamespacedKey> recipes) {
        return p.undiscoverRecipes(recipes);
    }

    public boolean hasPermission(@NotNull String name) {
        return p.hasPermission(name);
    }

    public void setWhitelisted(boolean value) {
        p.setWhitelisted(value);
    }

    @NotNull
    public EntityType getType() {
        return p.getType();
    }

    public void kickPlayer(@Nullable String message) {
        p.kickPlayer(message);
    }

    public void sendSignChange(@NotNull Location loc, @Nullable String[] lines) throws IllegalArgumentException {
        p.sendSignChange(loc, lines);
    }

    public void setLevel(int level) {
        p.setLevel(level);
    }

    public void setSpectatorTarget(@Nullable Entity entity) {
        p.setSpectatorTarget(entity);
    }

    public boolean isInWorld() {
        return p.isInWorld();
    }

    public void setGravity(boolean gravity) {
        p.setGravity(gravity);
    }

    @NotNull
    public Set<String> getListeningPluginChannels() {
        return p.getListeningPluginChannels();
    }

    @NotNull
    public Set<NamespacedKey> getDiscoveredRecipes() {
        return p.getDiscoveredRecipes();
    }

    @NotNull
    public PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
        return p.addAttachment(plugin, name, value);
    }

    public boolean getCanPickupItems() {
        return p.getCanPickupItems();
    }

    public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) throws IllegalArgumentException {
        p.incrementStatistic(statistic, material, amount);
    }

    public boolean setWindowProperty(@NotNull InventoryView.Property prop, int value) {
        return p.setWindowProperty(prop, value);
    }

    public void setFlying(boolean value) {
        p.setFlying(value);
    }

    public void sendHealthUpdate(double health, int foodLevel, float saturation) {
        p.sendHealthUpdate(health, foodLevel, saturation);
    }

    @NotNull
    public PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        return p.addAttachment(plugin);
    }

    public boolean hasMetadata(@NotNull String metadataKey) {
        return p.hasMetadata(metadataKey);
    }

    public void playNote(@NotNull Location loc, @NotNull Instrument instrument, @NotNull Note note) {
        p.playNote(loc, instrument, note);
    }

    public void setSleepingIgnored(boolean isSleeping) {
        p.setSleepingIgnored(isSleeping);
    }

    public void setInvulnerable(boolean flag) {
        p.setInvulnerable(flag);
    }

    public boolean isHealthScaled() {
        return p.isHealthScaled();
    }

    @NotNull
    public PersistentDataContainer getPersistentDataContainer() {
        return p.getPersistentDataContainer();
    }

    public void sendExperienceChange(float progress) {
        p.sendExperienceChange(progress);
    }

    @Nullable
    public Player getKiller() {
        return p.getKiller();
    }

    public void setStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int newValue) {
        p.setStatistic(statistic, entityType, newValue);
    }

    public void setSprinting(boolean sprinting) {
        p.setSprinting(sprinting);
    }

    @Nullable
    public Player getPlayer() {
        return p.getPlayer();
    }

    public void setCustomChatCompletions(@NotNull Collection<String> completions) {
        p.setCustomChatCompletions(completions);
    }

    @NotNull
    public GameMode getGameMode() {
        return p.getGameMode();
    }

    public void setExhaustion(float value) {
        p.setExhaustion(value);
    }

    public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, @Nullable T data) {
        p.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, data);
    }

    public void setPlayerWeather(@NotNull WeatherType type) {
        p.setPlayerWeather(type);
    }

    public boolean addPassenger(@NotNull Entity passenger) {
        return p.addPassenger(passenger);
    }

    @ApiStatus.Experimental
    public void setVisibleByDefault(boolean visible) {
        p.setVisibleByDefault(visible);
    }

    public void addCustomChatCompletions(@NotNull Collection<String> completions) {
        p.addCustomChatCompletions(completions);
    }

    @Nullable
    public Firework fireworkBoost(@NotNull ItemStack fireworkItemStack) {
        return p.fireworkBoost(fireworkItemStack);
    }

    public void updateCommands() {
        p.updateCommands();
    }

    @Nullable
    public String getCustomName() {
        return p.getCustomName();
    }

    public void playSound(@NotNull Location location, @NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch, long seed) {
        p.playSound(location, sound, category, volume, pitch, seed);
    }

    public boolean isEmpty() {
        return p.isEmpty();
    }

    @Nullable
    public InetSocketAddress getAddress() {
        return p.getAddress();
    }

    public <T> void setMemory(@NotNull MemoryKey<T> memoryKey, @Nullable T memoryValue) {
        p.setMemory(memoryKey, memoryValue);
    }

    public void sendBlockChanges(@NotNull Collection<BlockState> blocks) {
        p.sendBlockChanges(blocks);
    }

    @NotNull
    public Set<String> getScoreboardTags() {
        return p.getScoreboardTags();
    }

    @ApiStatus.Experimental
    @NotNull
    public Entity copy(@NotNull Location to) {
        return p.copy(to);
    }

    @Nullable
    public InventoryView openEnchanting(@Nullable Location location, boolean force) {
        return p.openEnchanting(location, force);
    }

    public boolean isRiptiding() {
        return p.isRiptiding();
    }

    public boolean getAllowFlight() {
        return p.getAllowFlight();
    }

    public boolean isVisualFire() {
        return p.isVisualFire();
    }

    @NotNull
    public String getDisplayName() {
        return p.getDisplayName();
    }

    public boolean hasPlayedBefore() {
        return p.hasPlayedBefore();
    }

    public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {
        p.incrementStatistic(statistic, material);
    }

    public boolean hasLineOfSight(@NotNull Entity other) {
        return p.hasLineOfSight(other);
    }

    public void stopSound(@NotNull Sound sound, @Nullable SoundCategory category) {
        p.stopSound(sound, category);
    }

    public boolean breakBlock(@NotNull Block block) {
        return p.breakBlock(block);
    }

    public void setPlayerListFooter(@Nullable String footer) {
        p.setPlayerListFooter(footer);
    }

    public double getEyeHeight(boolean ignorePose) {
        return p.getEyeHeight(ignorePose);
    }

    @Nullable
    public RayTraceResult rayTraceBlocks(double maxDistance) {
        return p.rayTraceBlocks(maxDistance);
    }

    @NotNull
    public Sound getFallDamageSoundSmall() {
        return p.getFallDamageSoundSmall();
    }

    public void sendPluginMessage(@NotNull Plugin source, @NotNull String channel, @NotNull byte[] message) {
        p.sendPluginMessage(source, channel, message);
    }

    public int getClientViewDistance() {
        return p.getClientViewDistance();
    }

    public boolean setLeashHolder(@Nullable Entity holder) {
        return p.setLeashHolder(holder);
    }

    @Deprecated
    public void showPlayer(@NotNull Player player) {
        p.showPlayer(player);
    }

    public boolean isDead() {
        return p.isDead();
    }

    public void stopSound(@NotNull Sound sound) {
        p.stopSound(sound);
    }

    public <T> void playEffect(@NotNull Location loc, @NotNull Effect effect, @Nullable T data) {
        p.playEffect(loc, effect, data);
    }

    public void setResourcePack(@NotNull String url, @Nullable byte[] hash) {
        p.setResourcePack(url, hash);
    }

    @NotNull
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return p.getEffectivePermissions();
    }

    public boolean isSneaking() {
        return p.isSneaking();
    }

    public int getCooldown(@NotNull Material material) {
        return p.getCooldown(material);
    }

    public int getRemainingAir() {
        return p.getRemainingAir();
    }

    @NotNull
    public EntityCategory getCategory() {
        return p.getCategory();
    }

    @ApiStatus.Experimental
    public boolean canSee(@NotNull Entity entity) {
        return p.canSee(entity);
    }

    public void sendEquipmentChange(@NotNull LivingEntity entity, @NotNull Map<EquipmentSlot, ItemStack> items) {
        p.sendEquipmentChange(entity, items);
    }

    @Nullable
    public InventoryView openMerchant(@NotNull Villager trader, boolean force) {
        return p.openMerchant(trader, force);
    }

    public int getFoodLevel() {
        return p.getFoodLevel();
    }

    @NotNull
    public World getWorld() {
        return p.getWorld();
    }

    public int getEntityId() {
        return p.getEntityId();
    }

    public void setLastDamage(double damage) {
        p.setLastDamage(damage);
    }

    @NotNull
    public Sound getSwimSound() {
        return p.getSwimSound();
    }

    public float getAttackCooldown() {
        return p.getAttackCooldown();
    }

    public void setItemOnCursor(@Nullable ItemStack item) {
        p.setItemOnCursor(item);
    }

    public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count, double offsetX, double offsetY, double offsetZ, @Nullable T data) {
        p.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, data);
    }

    public void damage(double amount) {
        p.damage(amount);
    }

    public void wakeup(boolean setSpawnLocation) {
        p.wakeup(setSpawnLocation);
    }

    public double getHealth() {
        return p.getHealth();
    }

    public void setStatistic(@NotNull Statistic statistic, int newValue) throws IllegalArgumentException {
        p.setStatistic(statistic, newValue);
    }

    public void setCollidable(boolean collidable) {
        p.setCollidable(collidable);
    }

    public double getHealthScale() {
        return p.getHealthScale();
    }

    @Nullable
    public Location getLastDeathLocation() {
        return p.getLastDeathLocation();
    }

    public void setBedSpawnLocation(@Nullable Location location) {
        p.setBedSpawnLocation(location);
    }

    public void setMaximumAir(int ticks) {
        p.setMaximumAir(ticks);
    }

    @ApiStatus.Experimental
    @Nullable
    public EntitySnapshot createSnapshot() {
        return p.createSnapshot();
    }

    public void sendBlockDamage(@NotNull Location loc, float progress) {
        p.sendBlockDamage(loc, progress);
    }

    @Nullable
    public WorldBorder getWorldBorder() {
        return p.getWorldBorder();
    }

    public void sendMap(@NotNull MapView map) {
        p.sendMap(map);
    }

    public long getPlayerTimeOffset() {
        return p.getPlayerTimeOffset();
    }

    public boolean hasPotionEffect(@NotNull PotionEffectType type) {
        return p.hasPotionEffect(type);
    }

    public void setExp(float exp) {
        p.setExp(exp);
    }

    @Nullable
    @Contract("null -> null; !null -> !null")
    public Location getLocation(@Nullable Location loc) {
        return p.getLocation(loc);
    }

    public boolean performCommand(@NotNull String command) {
        return p.performCommand(command);
    }

    @Deprecated
    public void playEffect(@NotNull Location loc, @NotNull Effect effect, int data) {
        p.playEffect(loc, effect, data);
    }

    public void sendTitle(@Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut) {
        p.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    public void setSaturatedRegenRate(int ticks) {
        p.setSaturatedRegenRate(ticks);
    }

    public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) throws IllegalArgumentException {
        p.decrementStatistic(statistic, material, amount);
    }

    public boolean teleport(@NotNull Location location) {
        return p.teleport(location);
    }

    public int getMaximumNoDamageTicks() {
        return p.getMaximumNoDamageTicks();
    }

    public void setFireTicks(int ticks) {
        p.setFireTicks(ticks);
    }

    public void setCompassTarget(@NotNull Location loc) {
        p.setCompassTarget(loc);
    }

    public void resetTitle() {
        p.resetTitle();
    }

    public void abandonConversation(@NotNull Conversation conversation) {
        p.abandonConversation(conversation);
    }

    @Deprecated
    public boolean setPassenger(@NotNull Entity passenger) {
        return p.setPassenger(passenger);
    }

    @NotNull
    public <T extends Projectile> T launchProjectile(@NotNull Class<? extends T> projectile, @Nullable Vector velocity) {
        return p.launchProjectile(projectile, velocity);
    }

    @Nullable
    public AttributeInstance getAttribute(@NotNull Attribute attribute) {
        return p.getAttribute(attribute);
    }

    @NotNull
    public Pose getPose() {
        return p.getPose();
    }

    @NotNull
    public PlayerProfile getPlayerProfile() {
        return p.getPlayerProfile();
    }

    public void setSilent(boolean flag) {
        p.setSilent(flag);
    }

    public void swingMainHand() {
        p.swingMainHand();
    }

    @NotNull
    public List<Block> getLastTwoTargetBlocks(@Nullable Set<Material> transparent, int maxDistance) {
        return p.getLastTwoTargetBlocks(transparent, maxDistance);
    }

    public void removePotionEffect(@NotNull PotionEffectType type) {
        p.removePotionEffect(type);
    }

    public void acceptConversationInput(@NotNull String input) {
        p.acceptConversationInput(input);
    }

    public void setAbsorptionAmount(double amount) {
        p.setAbsorptionAmount(amount);
    }

    @Nullable
    public String getPlayerListFooter() {
        return p.getPlayerListFooter();
    }

    public boolean isHandRaised() {
        return p.isHandRaised();
    }

    @Nullable
    public Sound getDeathSound() {
        return p.getDeathSound();
    }

    public void hidePlayer(@NotNull Plugin plugin, @NotNull Player player) {
        p.hidePlayer(plugin, player);
    }

    public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        p.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    public void openBook(@NotNull ItemStack book) {
        p.openBook(book);
    }

    public void playSound(@NotNull Entity entity, @NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch) {
        p.playSound(entity, sound, category, volume, pitch);
    }

    public void setGlowing(boolean flag) {
        p.setGlowing(flag);
    }

    @Nullable
    @Deprecated
    public Entity getShoulderEntityLeft() {
        return p.getShoulderEntityLeft();
    }

    @Deprecated
    public void resetMaxHealth() {
        p.resetMaxHealth();
    }

    public boolean eject() {
        return p.eject();
    }

    public void sendBlockChange(@NotNull Location loc, @NotNull BlockData block) {
        p.sendBlockChange(loc, block);
    }

    public void sendEquipmentChange(@NotNull LivingEntity entity, @NotNull EquipmentSlot slot, @Nullable ItemStack item) {
        p.sendEquipmentChange(entity, slot, item);
    }

    public void setAllowFlight(boolean flight) {
        p.setAllowFlight(flight);
    }

    public void openSign(@NotNull Sign sign, @NotNull Side side) {
        p.openSign(sign, side);
    }

    public void stopSound(@NotNull SoundCategory category) {
        p.stopSound(category);
    }

    @Nullable
    public <T> T getMemory(@NotNull MemoryKey<T> memoryKey) {
        return p.getMemory(memoryKey);
    }

    @NotNull
    public Inventory getEnderChest() {
        return p.getEnderChest();
    }

    @Nullable
    @Deprecated
    public Entity getShoulderEntityRight() {
        return p.getShoulderEntityRight();
    }

    @NotNull
    public Sound getEatingSound(@NotNull ItemStack itemStack) {
        return p.getEatingSound(itemStack);
    }

    public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) {
        p.decrementStatistic(statistic, entityType, amount);
    }

    public boolean isInsideVehicle() {
        return p.isInsideVehicle();
    }

    public boolean isSleeping() {
        return p.isSleeping();
    }

    public float getFlySpeed() {
        return p.getFlySpeed();
    }

    public void setResourcePack(@NotNull String url, @Nullable byte[] hash, @Nullable String prompt, boolean force) {
        p.setResourcePack(url, hash, prompt, force);
    }

    public void setHealthScaled(boolean scale) {
        p.setHealthScaled(scale);
    }

    public void setSneaking(boolean sneak) {
        p.setSneaking(sneak);
    }

    @Nullable
    public InventoryView openWorkbench(@Nullable Location location, boolean force) {
        return p.openWorkbench(location, force);
    }

    public int getFreezeTicks() {
        return p.getFreezeTicks();
    }

    public boolean dropItem(boolean dropAll) {
        return p.dropItem(dropAll);
    }

    public boolean teleport(@NotNull Entity destination) {
        return p.teleport(destination);
    }

    @Deprecated
    public void playNote(@NotNull Location loc, byte instrument, byte note) {
        p.playNote(loc, instrument, note);
    }

    @Nullable
    public Block getTargetBlockExact(int maxDistance, @NotNull FluidCollisionMode fluidCollisionMode) {
        return p.getTargetBlockExact(maxDistance, fluidCollisionMode);
    }

    public void setCustomNameVisible(boolean flag) {
        p.setCustomNameVisible(flag);
    }

    @NotNull
    public Scoreboard getScoreboard() {
        return p.getScoreboard();
    }

    public int getPing() {
        return p.getPing();
    }

    public int getPortalCooldown() {
        return p.getPortalCooldown();
    }

    public int getTotalExperience() {
        return p.getTotalExperience();
    }

    public int getNoActionTicks() {
        return p.getNoActionTicks();
    }

    public boolean isWhitelisted() {
        return p.isWhitelisted();
    }

    public void decrementStatistic(@NotNull Statistic statistic, int amount) throws IllegalArgumentException {
        p.decrementStatistic(statistic, amount);
    }

    public void recalculatePermissions() {
        p.recalculatePermissions();
    }

    public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count, @Nullable T data) {
        p.spawnParticle(particle, location, count, data);
    }

    public boolean isValid() {
        return p.isValid();
    }

    public void setPlayerListName(@Nullable String name) {
        p.setPlayerListName(name);
    }

    @Nullable
    public EntityEquipment getEquipment() {
        return p.getEquipment();
    }

    public void setPersistent(boolean persistent) {
        p.setPersistent(persistent);
    }

    public void damage(double amount, @Nullable Entity source) {
        p.damage(amount, source);
    }

    public void setPlayerTime(long time, boolean relative) {
        p.setPlayerTime(time, relative);
    }

    public boolean isSwimming() {
        return p.isSwimming();
    }

    @Deprecated
    public void setItemInHand(@Nullable ItemStack item) {
        p.setItemInHand(item);
    }

    public boolean canSee(@NotNull Player player) {
        return p.canSee(player);
    }

    @NotNull
    public Vector getVelocity() {
        return p.getVelocity();
    }

    public int getExpCooldown() {
        return p.getExpCooldown();
    }

    public boolean isBanned() {
        return p.isBanned();
    }

    @NotNull
    public UUID getUniqueId() {
        return p.getUniqueId();
    }

    public boolean isAllowingServerListings() {
        return p.isAllowingServerListings();
    }

    public int getSleepTicks() {
        return p.getSleepTicks();
    }

    public void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count) {
        p.spawnParticle(particle, x, y, z, count);
    }

    public void playSound(@NotNull Location location, @NotNull String sound, @NotNull SoundCategory category, float volume, float pitch, long seed) {
        p.playSound(location, sound, category, volume, pitch, seed);
    }

    @NotNull
    public BoundingBox getBoundingBox() {
        return p.getBoundingBox();
    }

    @Nullable
    public BanEntry<PlayerProfile> ban(@Nullable String reason, @Nullable Duration duration, @Nullable String source, boolean kickPlayer) {
        return p.ban(reason, duration, source, kickPlayer);
    }

    public boolean isPlayerTimeRelative() {
        return p.isPlayerTimeRelative();
    }

    public void decrementStatistic(@NotNull Statistic statistic) throws IllegalArgumentException {
        p.decrementStatistic(statistic);
    }

    @ApiStatus.Experimental
    public void showEntity(@NotNull Plugin plugin, @NotNull Entity entity) {
        p.showEntity(plugin, entity);
    }

    public void saveData() {
        p.saveData();
    }

    public void setPlayerListHeaderFooter(@Nullable String header, @Nullable String footer) {
        p.setPlayerListHeaderFooter(header, footer);
    }

    public void setMaximumNoDamageTicks(int ticks) {
        p.setMaximumNoDamageTicks(ticks);
    }

    @NotNull
    public Location getEyeLocation() {
        return p.getEyeLocation();
    }

    public void sendExperienceChange(float progress, int level) {
        p.sendExperienceChange(progress, level);
    }

    public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count) {
        p.spawnParticle(particle, location, count);
    }

    public void sendSignChange(@NotNull Location loc, @Nullable String[] lines, @NotNull DyeColor dyeColor, boolean hasGlowingText) throws IllegalArgumentException {
        p.sendSignChange(loc, lines, dyeColor, hasGlowingText);
    }

    public int getStarvationRate() {
        return p.getStarvationRate();
    }

    public boolean addPotionEffect(@NotNull PotionEffect effect) {
        return p.addPotionEffect(effect);
    }

    @NotNull
    public AdvancementProgress getAdvancementProgress(@NotNull Advancement advancement) {
        return p.getAdvancementProgress(advancement);
    }

    public void setGameMode(@NotNull GameMode mode) {
        p.setGameMode(mode);
    }

    public void swingOffHand() {
        p.swingOffHand();
    }

    public void playSound(@NotNull Entity entity, @NotNull String sound, @NotNull SoundCategory category, float volume, float pitch) {
        p.playSound(entity, sound, category, volume, pitch);
    }

    public boolean isConversing() {
        return p.isConversing();
    }

    public void setDisplayName(@Nullable String name) {
        p.setDisplayName(name);
    }

    public boolean addScoreboardTag(@NotNull String tag) {
        return p.addScoreboardTag(tag);
    }

    @Deprecated
    public void sendTitle(@Nullable String title, @Nullable String subtitle) {
        p.sendTitle(title, subtitle);
    }

    @Nullable
    public ItemStack getItemInUse() {
        return p.getItemInUse();
    }

    public void setFoodLevel(int value) {
        p.setFoodLevel(value);
    }

    public boolean isLeashed() {
        return p.isLeashed();
    }

    public boolean hasAI() {
        return p.hasAI();
    }

    @Nullable
    public BanEntry<PlayerProfile> ban(@Nullable String reason, @Nullable Instant expires, @Nullable String source, boolean kickPlayer) {
        return p.ban(reason, expires, source, kickPlayer);
    }

    public void playSound(@NotNull Entity entity, @NotNull String sound, float volume, float pitch) {
        p.playSound(entity, sound, volume, pitch);
    }

    @NotNull
    public PistonMoveReaction getPistonMoveReaction() {
        return p.getPistonMoveReaction();
    }

    public void giveExpLevels(int amount) {
        p.giveExpLevels(amount);
    }

    public boolean discoverRecipe(@NotNull NamespacedKey recipe) {
        return p.discoverRecipe(recipe);
    }

    @ApiStatus.Experimental
    public void sendBlockUpdate(@NotNull Location loc, @NotNull TileState tileState) throws IllegalArgumentException {
        p.sendBlockUpdate(loc, tileState);
    }

    public <T> void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count, @Nullable T data) {
        p.spawnParticle(particle, x, y, z, count, data);
    }

    @NotNull
    public List<MetadataValue> getMetadata(@NotNull String metadataKey) {
        return p.getMetadata(metadataKey);
    }

    public void setLastDamageCause(@Nullable EntityDamageEvent event) {
        p.setLastDamageCause(event);
    }

    public int getArrowsInBody() {
        return p.getArrowsInBody();
    }

    @NotNull
    public Sound getFallDamageSoundBig() {
        return p.getFallDamageSoundBig();
    }

    @Nullable
    public BanEntry<PlayerProfile> ban(@Nullable String reason, @Nullable Date expires, @Nullable String source, boolean kickPlayer) {
        return p.ban(reason, expires, source, kickPlayer);
    }

    public boolean undiscoverRecipe(@NotNull NamespacedKey recipe) {
        return p.undiscoverRecipe(recipe);
    }

    public void setHealth(double health) {
        p.setHealth(health);
    }

    public boolean isPermissionSet(@NotNull Permission perm) {
        return p.isPermissionSet(perm);
    }

    @NotNull
    public InventoryView getOpenInventory() {
        return p.getOpenInventory();
    }

    public void sendRawMessage(@NotNull String message) {
        p.sendRawMessage(message);
    }

    public boolean isFrozen() {
        return p.isFrozen();
    }

    public boolean hasDiscoveredRecipe(@NotNull NamespacedKey recipe) {
        return p.hasDiscoveredRecipe(recipe);
    }

    public boolean hasPermission(@NotNull Permission perm) {
        return p.hasPermission(perm);
    }

    public float getSaturation() {
        return p.getSaturation();
    }

    public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {
        p.decrementStatistic(statistic, entityType);
    }

    public void playEffect(@NotNull EntityEffect type) {
        p.playEffect(type);
    }

    public void setRotation(float yaw, float pitch) {
        p.setRotation(yaw, pitch);
    }

    @NotNull
    public Sound getSwimSplashSound() {
        return p.getSwimSplashSound();
    }

    public void setWorldBorder(@Nullable WorldBorder border) {
        p.setWorldBorder(border);
    }

    public void setInvisible(boolean invisible) {
        p.setInvisible(invisible);
    }

    public void removeMetadata(@NotNull String metadataKey, @NotNull Plugin owningPlugin) {
        p.removeMetadata(metadataKey, owningPlugin);
    }

    @NotNull
    public Server getServer() {
        return p.getServer();
    }

    @Nullable
    public BanEntry<InetAddress> banIp(@Nullable String reason, @Nullable Duration duration, @Nullable String source, boolean kickPlayer) {
        return p.banIp(reason, duration, source, kickPlayer);
    }

    public void setResourcePack(@NotNull String url, @Nullable byte[] hash, boolean force) {
        p.setResourcePack(url, hash, force);
    }

    public void setVisualFire(boolean fire) {
        p.setVisualFire(fire);
    }

    public int getStatistic(@NotNull Statistic statistic) throws IllegalArgumentException {
        return p.getStatistic(statistic);
    }

    @ApiStatus.Internal
    public void updateInventory() {
        p.updateInventory();
    }

    @NotNull
    public Player.Spigot spigot() {
        return p.spigot();
    }

    @Nullable
    public BanEntry<InetAddress> banIp(@Nullable String reason, @Nullable Instant expires, @Nullable String source, boolean kickPlayer) {
        return p.banIp(reason, expires, source, kickPlayer);
    }

    public boolean hasGravity() {
        return p.hasGravity();
    }

    @Nullable
    public BanEntry<PlayerProfile> ban(@Nullable String reason, @Nullable Date expires, @Nullable String source) {
        return p.ban(reason, expires, source);
    }

    public boolean sleep(@NotNull Location location, boolean force) {
        return p.sleep(location, force);
    }

    public void setSwimming(boolean swimming) {
        p.setSwimming(swimming);
    }

    public void playSound(@NotNull Entity entity, @NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch, long seed) {
        p.playSound(entity, sound, category, volume, pitch, seed);
    }

    @Nullable
    public WeatherType getPlayerWeather() {
        return p.getPlayerWeather();
    }

    public void showPlayer(@NotNull Plugin plugin, @NotNull Player player) {
        p.showPlayer(plugin, player);
    }

    @ApiStatus.Experimental
    public boolean isVisibleByDefault() {
        return p.isVisibleByDefault();
    }

    public void removeCustomChatCompletions(@NotNull Collection<String> completions) {
        p.removeCustomChatCompletions(completions);
    }

    public void sendSignChange(@NotNull Location loc, @Nullable String[] lines, @NotNull DyeColor dyeColor) throws IllegalArgumentException {
        p.sendSignChange(loc, lines, dyeColor);
    }

    public void setLastDeathLocation(@Nullable Location location) {
        p.setLastDeathLocation(location);
    }

    public boolean removePassenger(@NotNull Entity passenger) {
        return p.removePassenger(passenger);
    }

    public int getLevel() {
        return p.getLevel();
    }

    public int getFireTicks() {
        return p.getFireTicks();
    }

    public boolean isSleepingIgnored() {
        return p.isSleepingIgnored();
    }

    public void setRemainingAir(int ticks) {
        p.setRemainingAir(ticks);
    }

    @NotNull
    public Block getTargetBlock(@Nullable Set<Material> transparent, int maxDistance) {
        return p.getTargetBlock(transparent, maxDistance);
    }

    public void setOp(boolean value) {
        p.setOp(value);
    }

    @Deprecated
    public void sendBlockChanges(@NotNull Collection<BlockState> blocks, boolean suppressLightUpdates) {
        p.sendBlockChanges(blocks, suppressLightUpdates);
    }

    public void sendRawMessage(@Nullable UUID sender, @NotNull String message) {
        p.sendRawMessage(sender, message);
    }

    public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) throws IllegalArgumentException {
        p.incrementStatistic(statistic, entityType, amount);
    }

    public boolean isInvulnerable() {
        return p.isInvulnerable();
    }

    public void setFlySpeed(float value) throws IllegalArgumentException {
        p.setFlySpeed(value);
    }

    @Nullable
    public RayTraceResult rayTraceBlocks(double maxDistance, @NotNull FluidCollisionMode fluidCollisionMode) {
        return p.rayTraceBlocks(maxDistance, fluidCollisionMode);
    }

    @Nullable
    public PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
        return p.addAttachment(plugin, name, value, ticks);
    }

    public void setExpCooldown(int ticks) {
        p.setExpCooldown(ticks);
    }

    @Deprecated
    public void sendBlockChange(@NotNull Location loc, @NotNull Material material, byte data) {
        p.sendBlockChange(loc, material, data);
    }

    public void setPlayerListHeader(@Nullable String header) {
        p.setPlayerListHeader(header);
    }

    @Nullable
    public PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
        return p.addAttachment(plugin, ticks);
    }

    @NotNull
    public SpawnCategory getSpawnCategory() {
        return p.getSpawnCategory();
    }

    public boolean getRemoveWhenFarAway() {
        return p.getRemoveWhenFarAway();
    }

    public void closeInventory() {
        p.closeInventory();
    }

    public void playSound(@NotNull Entity entity, @NotNull Sound sound, float volume, float pitch) {
        p.playSound(entity, sound, volume, pitch);
    }

    public void stopSound(@NotNull String sound) {
        p.stopSound(sound);
    }

    public void setResourcePack(@NotNull String url) {
        p.setResourcePack(url);
    }

    @NotNull
    public List<Entity> getNearbyEntities(double x, double y, double z) {
        return p.getNearbyEntities(x, y, z);
    }

    @NotNull
    public Sound getFallDamageSound(int fallHeight) {
        return p.getFallDamageSound(fallHeight);
    }

    @Nullable
    public BanEntry<InetAddress> banIp(@Nullable String reason, @Nullable Date expires, @Nullable String source, boolean kickPlayer) {
        return p.banIp(reason, expires, source, kickPlayer);
    }

    @Nullable
    public Entity getSpectatorTarget() {
        return p.getSpectatorTarget();
    }

    public boolean isGliding() {
        return p.isGliding();
    }

    public void setResourcePack(@NotNull String url, @Nullable byte[] hash, @Nullable String prompt) {
        p.setResourcePack(url, hash, prompt);
    }

    @NotNull
    public MainHand getMainHand() {
        return p.getMainHand();
    }

    public boolean hasCooldown(@NotNull Material material) {
        return p.hasCooldown(material);
    }

    public void attack(@NotNull Entity target) {
        p.attack(target);
    }

    public <T> void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, @Nullable T data) {
        p.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, data);
    }

    public void openInventory(@NotNull InventoryView inventory) {
        p.openInventory(inventory);
    }

    @NotNull
    public Entity getLeashHolder() throws IllegalStateException {
        return p.getLeashHolder();
    }

    @Nullable
    public BanEntry<PlayerProfile> ban(@Nullable String reason, @Nullable Instant expires, @Nullable String source) {
        return p.ban(reason, expires, source);
    }

    @ApiStatus.Experimental
    public void hideEntity(@NotNull Plugin plugin, @NotNull Entity entity) {
        p.hideEntity(plugin, entity);
    }

    public int getUnsaturatedRegenRate() {
        return p.getUnsaturatedRegenRate();
    }

    @Nullable
    @Deprecated
    public Entity getPassenger() {
        return p.getPassenger();
    }

    public void abandonConversation(@NotNull Conversation conversation, @NotNull ConversationAbandonedEvent details) {
        p.abandonConversation(conversation, details);
    }

    public long getFirstPlayed() {
        return p.getFirstPlayed();
    }

    public void stopSound(@NotNull String sound, @Nullable SoundCategory category) {
        p.stopSound(sound, category);
    }

    public double getEyeHeight() {
        return p.getEyeHeight();
    }

    @NotNull
    public List<Entity> getPassengers() {
        return p.getPassengers();
    }

    public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {
        p.incrementStatistic(statistic, entityType);
    }

    public void setArrowsInBody(int count) {
        p.setArrowsInBody(count);
    }

    @Deprecated
    public double getMaxHealth() {
        return p.getMaxHealth();
    }

    public void setScoreboard(@NotNull Scoreboard scoreboard) throws IllegalArgumentException, IllegalStateException {
        p.setScoreboard(scoreboard);
    }

    public void remove() {
        p.remove();
    }

    public void setBedSpawnLocation(@Nullable Location location, boolean force) {
        p.setBedSpawnLocation(location, force);
    }

    public float getFallDistance() {
        return p.getFallDistance();
    }

    public void setCustomName(@Nullable String name) {
        p.setCustomName(name);
    }

    @NotNull
    public Location getCompassTarget() {
        return p.getCompassTarget();
    }

    public void chat(@NotNull String msg) {
        p.chat(msg);
    }

    @Nullable
    public BanEntry<PlayerProfile> ban(@Nullable String reason, @Nullable Duration duration, @Nullable String source) {
        return p.ban(reason, duration, source);
    }

    @Deprecated
    public void setShoulderEntityLeft(@Nullable Entity entity) {
        p.setShoulderEntityLeft(entity);
    }

    public int getNoDamageTicks() {
        return p.getNoDamageTicks();
    }

    @Nullable
    public PotionEffect getPotionEffect(@NotNull PotionEffectType type) {
        return p.getPotionEffect(type);
    }

    public boolean leaveVehicle() {
        return p.leaveVehicle();
    }

    @Deprecated
    public void setShoulderEntityRight(@Nullable Entity entity) {
        p.setShoulderEntityRight(entity);
    }

    @NotNull
    public Location getBedLocation() {
        return p.getBedLocation();
    }

    public void setCanPickupItems(boolean pickup) {
        p.setCanPickupItems(pickup);
    }

    public boolean isClimbing() {
        return p.isClimbing();
    }

    public boolean teleport(@NotNull Location location, @NotNull PlayerTeleportEvent.TeleportCause cause) {
        return p.teleport(location, cause);
    }

    public boolean addPotionEffects(@NotNull Collection<PotionEffect> effects) {
        return p.addPotionEffects(effects);
    }

    public void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        p.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra);
    }

    public double getHeight() {
        return p.getHeight();
    }

    public int getTicksLived() {
        return p.getTicksLived();
    }

    public void sendBlockDamage(@NotNull Location loc, float progress, int sourceId) {
        p.sendBlockDamage(loc, progress, sourceId);
    }

    public float getWalkSpeed() {
        return p.getWalkSpeed();
    }

    public boolean isCollidable() {
        return p.isCollidable();
    }

    public float getExhaustion() {
        return p.getExhaustion();
    }

    public void setStarvationRate(int ticks) {
        p.setStarvationRate(ticks);
    }

    public void setTotalExperience(int exp) {
        p.setTotalExperience(exp);
    }

    public boolean beginConversation(@NotNull Conversation conversation) {
        return p.beginConversation(conversation);
    }

    public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count, double offsetX, double offsetY, double offsetZ) {
        p.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ);
    }

    public <T> void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, @Nullable T data) {
        p.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, data);
    }

    public int getMaxFreezeTicks() {
        return p.getMaxFreezeTicks();
    }

    @Deprecated
    public boolean isOnGround() {
        return p.isOnGround();
    }

    @ApiStatus.Experimental
    @NotNull
    public Entity copy() {
        return p.copy();
    }

    public void setPortalCooldown(int cooldown) {
        p.setPortalCooldown(cooldown);
    }

    public int getEnchantmentSeed() {
        return p.getEnchantmentSeed();
    }

    public void setCooldown(@NotNull Material material, int ticks) {
        p.setCooldown(material, ticks);
    }

    @NotNull
    public Collection<PotionEffect> getActivePotionEffects() {
        return p.getActivePotionEffects();
    }

    @NotNull
    public String getLocale() {
        return p.getLocale();
    }

    public int getStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {
        return p.getStatistic(statistic, material);
    }

    public void sendHurtAnimation(float yaw) {
        p.sendHurtAnimation(yaw);
    }

    public int getArrowCooldown() {
        return p.getArrowCooldown();
    }

    public void setNoActionTicks(int ticks) {
        p.setNoActionTicks(ticks);
    }
    //</editor-fold>
}
