package org.hexils.dnarch.commands;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.hexils.dnarch.Main.log;

public final class da_cmd implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return true;
        }
        if (sender instanceof ConsoleCommandSender console) {
            return true;
        }
        Player p = (Player) sender;
        switch (args[0]) {
//            case "test" -> {
//                Sign s = new Sign() {
//                    final org.bukkit.block.data.type.Sign sd = (org.bukkit.block.data.type.Sign) Bukkit.createBlockData(Material.SPRUCE_SIGN);
//                    final Material mat = Material.SPRUCE_SIGN;
//                    final DyeColor color = DyeColor.BLACK;
//                    final String[] lines = new String[4];
//
//                    @Contract(value = " -> new", pure = true)
//                    @Override
//                    public @NotNull String @NotNull [] getLines() {
//                        return new String[0];
//                    }
//
//                    @Override
//                    public @NotNull String getLine(int i) throws IndexOutOfBoundsException {
//                        return "";
//                    }
//
//                    @Override
//                    public void setLine(int i, @NotNull String s) throws IndexOutOfBoundsException {
//
//                    }
//
//                    @Override
//                    public boolean isEditable() {
//                        return true;
//                    }
//
//                    @Override
//                    public void setEditable(boolean b) {
//
//                    }
//
//                    @Override
//                    public boolean isWaxed() {
//                        return false;
//                    }
//
//                    @Override
//                    public void setWaxed(boolean b) {
//
//                    }
//
//                    @Override
//                    public boolean isGlowingText() {
//                        return false;
//                    }
//
//                    @Override
//                    public void setGlowingText(boolean b) {
//
//                    }
//
//                    @Override
//                    public @NotNull DyeColor getColor() {
//                        return color;
//                    }
//
//                    @Override
//                    public void setColor(@NotNull DyeColor dyeColor) {
//
//                    }
//
//                    @Override
//                    public @NotNull SignSide getSide(@NotNull Side side) {
//                        return new SignSide() {
//                            @Override
//                            public @NotNull String[] getLines() {
//                                return lines;
//                            }
//
//                            @Override
//                            public @NotNull String getLine(int i) throws IndexOutOfBoundsException {
//                                return lines[i];
//                            }
//
//                            @Override
//                            public void setLine(int i, @NotNull String s) throws IndexOutOfBoundsException {
//
//                            }
//
//                            @Override
//                            public boolean isGlowingText() {
//                                return false;
//                            }
//
//                            @Override
//                            public void setGlowingText(boolean b) {
//
//                            }
//
//                            @Override
//                            public @Nullable DyeColor getColor() {
//                                return color;
//                            }
//
//                            @Override
//                            public void setColor(@UndefinedNullability("defined by subclass") DyeColor dyeColor) {
//
//                            }
//                        };
//                    }
//
//                    @Override
//                    public @NotNull PersistentDataContainer getPersistentDataContainer() {
//                        return null;
//                    }
//
//                    @Override
//                    public @NotNull Block getBlock() {
//                        return p.getLocation().getBlock();
//                    }
//
//                    @Override
//                    public @NotNull MaterialData getData() {
//                        try {
//                            return mat.getData().newInstance();
//                        } catch (InstantiationException | IllegalAccessException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//
//                    @Override
//                    public @NotNull BlockData getBlockData() {
//                        return sd;
//                    }
//
//                    @Override
//                    public @NotNull BlockState copy() {
//                        return null;
//                    }
//
//                    @Override
//                    public @NotNull Material getType() {
//                        return mat;
//                    }
//
//                    @Override
//                    public byte getLightLevel() {
//                        return 0;
//                    }
//
//                    @Override
//                    public @NotNull World getWorld() {
//                        return p.getWorld();
//                    }
//
//                    @Override
//                    public int getX() {
//                        return 0;
//                    }
//
//                    @Override
//                    public int getY() {
//                        return 0;
//                    }
//
//                    @Override
//                    public int getZ() {
//                        return 0;
//                    }
//
//                    @Override
//                    public @NotNull Location getLocation() {
//                        return p.getLocation();
//                    }
//
//                    @Override
//                    public @Nullable Location getLocation(@Nullable Location location) {
//                        return null;
//                    }
//
//                    @Override
//                    public @NotNull Chunk getChunk() {
//                        return p.getLocation().getChunk();
//                    }
//
//                    @Override
//                    public void setData(@NotNull MaterialData materialData) {
//
//                    }
//
//                    @Override
//                    public void setBlockData(@NotNull BlockData blockData) {
//
//                    }
//
//                    @Override
//                    public void setType(@NotNull Material material) {
//
//                    }
//
//                    @Override
//                    public boolean update() {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean update(boolean b) {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean update(boolean b, boolean b1) {
//                        return false;
//                    }
//
//                    @Override
//                    public byte getRawData() {
//                        return 0;
//                    }
//
//                    @Override
//                    public void setRawData(byte b) {
//
//                    }
//
//                    @Override
//                    public boolean isPlaced() {
//                        return true;
//                    }
//
//                    @Override
//                    public void setMetadata(@NotNull String s, @NotNull MetadataValue metadataValue) {
//
//                    }
//
//                    @Override
//                    public @NotNull List<MetadataValue> getMetadata(@NotNull String s) {
//                        return List.of();
//                    }
//
//                    @Override
//                    public boolean hasMetadata(@NotNull String s) {
//                        return false;
//                    }
//
//                    @Override
//                    public void removeMetadata(@NotNull String s, @NotNull Plugin plugin) {
//
//                    }
//                };
//                Block b = p.getTargetBlockExact(5);
//                BlockData pm = b.getBlockData();
//                b.setBlockData(Bukkit.createBlockData(Material.SPRUCE_SIGN), true);
//
//                Sign s = (Sign) b.getState().copy();
//                BlockData bd = s.getBlockData();
//                bd.
//                s.setBlockData(bd);
//                p.openSign(s);
//            }\
        }
        return true;
    }


    public static final class tab implements TabCompleter {

        @Override
        public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            List<String> s = new ArrayList<>();
            return s;
        }
    }
}
