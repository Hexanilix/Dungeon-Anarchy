package org.hexils.dnarch.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.*;
import org.hexils.dnarch.dungeon.Dungeon;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.items.Trigger;
import org.hexils.dnarch.items.Type;
import org.hexils.dnarch.items.actions.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


import static org.hexils.dnarch.commands.DungeonCommandExecutor.IF;

public final class DungeonCreatorCommandExecutor implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String s = execute(sender, command, label, args);
        if (s != null) sender.sendMessage(s);
        return true;
    }

    public static ChatColor ER = ChatColor.RED;
    public static ChatColor OK = ChatColor.GREEN;
    public static ChatColor W = ChatColor.YELLOW;

    public static @Nullable String execute(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            //TODO dc usage message
            return null;
        }
        if (sender instanceof ConsoleCommandSender console) {
            return null;
        }
        Player p = (Player) sender;
        if (!sender.isOp()) return ER + "You cannot use this command!";
        DungeonMaster dm = DungeonMaster.getOrNew(p);
        String r = null;
        switch (args[0].toLowerCase()) {
            case "wand" -> p.getInventory().addItem(Main.wand);
            case "pos1" -> {
                if (args.length > 1) {
                    if (args.length > 3) {
                        dm.setSelectionA(p.getWorld().getBlockAt(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3])));
                    } else return ER + "Please provide x, y, z coordinates!";
                } else {
                    dm.setSelectionA(p.getLocation().getBlock());
                }
                return OK + "Set 1st position to " + org.hetils.mpdl.LocationUtil.toReadableFormat(dm.getSelectionA());
            }
            case "pos2" -> {
                if (args.length > 1) {
                    if (args.length > 3) {
                        dm.setSelectionB(p.getWorld().getBlockAt(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3])));
                    } else return ER + "Please provide x, y, z coordinates!";
                } else {
                    dm.setSelectionB(p.getLocation().getBlock());
                }
                return OK + "Set 2nd position to " + org.hetils.mpdl.LocationUtil.toReadableFormat(dm.getSelectionB());
            }
            case "deselect" -> {
                if (args.length == 1) {
                    dm.deselectBlocks();
                    dm.clearSelection();
                } else {
                    if (args[1].equalsIgnoreCase("blocks")) dm.deselectBlocks();
                    else if (args[1].equalsIgnoreCase("selection")) dm.clearSelection();
                }
            }
            case "create" -> {
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("dungeon")) {
                        if (dm.hasAreaSelected()) {
                            Dungeon d;
                            Dungeon.Section si = Dungeon.getIntersectedSection(dm.getSelectedArea());
                            if (si == null) {
                                if (args.length >= 3) {
                                    try {
                                        d = new Dungeon(dm.getUniqueId(), args.length == 3 ? args[2] : String.join("_", Arrays.copyOfRange(args, 2, args.length)), dm.getSelectedArea());
                                    } catch (Dungeon.DuplicateNameException ignore) {
                                        return ER + "Dungeon " + args[2] + " already exists.";
                                    } catch (Dungeon.DungeonIntersectViolation e) { throw new RuntimeException(e); }
                                } else d = Dungeon.create(dm.getUniqueId(), dm.getSelectedArea());
                                dm.clearSelection();
                                dm.setCurrentDungeon(d);
                                p.sendMessage(OK + "Created new dungeon " + d.getName());
                            } else return ER + "Cannot create dungeon, selection intersects sector \"" + si.getName() + "\" in dungeon \"" + si.getDungeon().getDungeonInfo().display_name;
                        } else p.sendMessage(ER + "You must select a section to create a dungeon");
                    } else {
                        if (dm.isEditing()) {
                            switch (args[1].toLowerCase()) {
                                case "section" -> { return dm.getCurrentDungeon().commandNewSection(dm, Arrays.copyOfRange(args, 2, args.length)); }
                                case "action"  -> {
                                    if (args.length == 2) return ER + "Please specify the action type!";
                                    else {
                                        Type t = Type.get(args[2]);
                                        if (t != null && t.isAction()) dm.giveItem(DA_item.commandNew(t, dm, Arrays.copyOfRange(args, 3, args.length)));
                                        else return ER + "Unknown type " + ChatColor.ITALIC + args[2].toLowerCase();
                                    }
                                }
                                case "condition" -> {
                                    if (args.length == 2) return ER + "Please specify the action type!";
                                    else {
                                        Type t = Type.get(args[2]);
                                        if (t != null && t.isCondition()) dm.giveItem(DA_item.commandNew(t, dm, Arrays.copyOfRange(args, 3, args.length)));
                                        else return ER + "Unknown type " + ChatColor.ITALIC + args[2].toLowerCase();
                                    }
                                }
                                case "trigger" -> p.getInventory().addItem(new Trigger().getItem());
                                default -> p.sendMessage(ER + "Unknown type " + args[1]);
                            }
                        } else switch (args[1].toLowerCase()) {
                            case "section", "action", "condition", "trigger" -> p.sendMessage(ER + "You must be currently editing a dungeon to create elements");
                            default -> p.sendMessage(ER + "Unknown argument " + ChatColor.ITALIC + args[1]);
                        }
                    }
                } else return (ER + "Please specify what to create!");
            }
            case "delete" -> {
                Dungeon d = dm.getCurrentDungeon();
                if (args.length == 1) {
                    if (d != null) d.attemptRemove(dm);
                } else switch (args[1]) {
                    case "dungeon" -> {
                        if (args.length >= 3) {
                            d = Dungeon.get(args[2]);
                            if (d == null) return ER + "No dungeon named \"" + args[2] + "\"";
                            if (dm.getCurrentDungeon() == d) {
                                d.removeViewer(dm);
                                dm.setCurrentDungeon(null);
                            }
                            d.delete();
                        } else {
                            if (dm.isEditing()) d.attemptRemove(dm);
                            else return W + "You're currently not editing any dungeon to remove";
                        }
                    }
                    case "section" -> {
                        Dungeon.Section s = d.getSection(p);
                        if (args.length >= 3) {
                            s = d.getSection(args[2]);
                            if (s == null) return ER + "No section named \"" + args[2] + "\"";
                        } else if (s == null) return W + "You're currently not in a section.";
                        s.attemptRemove(dm);
                    }
                }
            }
            case "save" -> {
                if (!dm.isEditing()) p.sendMessage(W + "You must be currently editing a dungeon to save it");
                else {
                    dm.getCurrentDungeon().save();
                    p.sendMessage(OK + "Saved dungeon " + dm.getCurrentDungeon().getName());
                }
            }
            case "run" -> {
                if (!dm.isEditing()) return W + "You must be currently editing a dungeon to run elements";
                DA_item da = DA_item.get(p.getInventory().getItemInMainHand());
                if (da instanceof Action a)
                    a.trigger();
                else if (da instanceof Trigger t) {
                  t.actions.forEach(Action::trigger);
                } else p.sendMessage(W + "Please hold a executable item");
            }
            case "reset" -> {
                if (!dm.isEditing()) return W + "You must be currently editing a dungeon to reset elements";
                DA_item da = DA_item.get(p.getInventory().getItemInMainHand());
                if (da instanceof Action a)
                    a.reset();
                else if (da instanceof Trigger t)
                    t.actions.forEach(Action::reset);
                else p.sendMessage(W + "Please hold a resetable item");
            }
            case "rename" -> {
                if (!dm.isEditing()) return ER + "You must be editing a dungeon to rename elements";
                else {
                    Dungeon d = dm.getCurrentDungeon();
                    if (args.length == 1) {
                        DA_item a = DA_item.get(p.getInventory().getItemInMainHand());
                        if (a != null) a.rename(dm);
                        else {
                            Dungeon.Section s = d.getSection(p);
                            if (s != null) s.rename(dm);
                            else d.rename(dm);
                        }
                    }
                    else switch (args[1]) {
                        case "dungeon" -> d.rename(dm);
                        case "section" -> {
                            Dungeon.Section s = d.getSection(p);
                            if (args.length >= 3) {
                                s = d.getSection(args[2]);
                                if (s == null) return ER + "No section named \"" + args[2] + "\"";
                            } else if (s == null) return W + "You're currently not in a section.";
                            s.rename(dm);
                        }
                        case "item" -> {
                            DA_item a = DA_item.get(p.getInventory().getItemInMainHand());
                            if (a != null) a.rename(dm);
                            else p.sendMessage(W + "Please hold a renameable item");
                        }
                        default -> p.sendMessage(ER + "Unknown argument " + args[1]);
                    }
                }
            }
            case "manage" -> {
                if (!dm.isEditing()) return W + "You must be editing a dungeon to manage elements!";
                Dungeon d = dm.getCurrentDungeon();
                if (args.length == 1) d.manage(dm);
                else switch (args[1]) {
                    case "dungeon" -> d.manage(dm);
                    case "section" -> {
                        Dungeon.Section s = d.getSection(p);
                        if (args.length >= 3) {
                            s = d.getSection(args[2]);
                            if (s == null) return ER + "No section named \"" + args[2] + "\"";
                        } else if (s == null) return W + "You're currently not in a section.";
                        s.manage(dm);
                    }
                    case "item" -> {
                        DA_item da = DA_item.get(p.getInventory().getItemInMainHand());
                        if (da != null) da.manage(dm);
                        else p.sendMessage(W + "Please hold a managable item!");
                    }
                    default -> p.sendMessage(ER + "Unknown type " + args[1]);
                }
            }
            case "edit" -> {
                if (!dm.isEditing()) {
                    Dungeon d;
                    if (args.length == 2) {
                        d = Dungeon.get(args[1]);
                        if (d == null) return ER + "No dungeon named \"" + args[1] + "\"";
                    } else {
                        d = Dungeon.get(p.getLocation());
                        if (d == null) return ER + "You're currently not in a dungeon, please go into the desired dungeon or specify one.";
                    }
                    dm.setCurrentDungeon(d);
                    d.showDungeonFor(dm);
                    return (OK + "Editing dungeon " + d.getDungeonInfo().display_name);
                } else p.sendMessage(W + "You're already editing dungeon \"" + dm.getCurrentDungeon().getName() + "\"!");
            }
            case "build" -> {
                if (!dm.isEditing()) return W + "You must be editing a dungeon to enable build mode";
                dm.setBuildMode(!dm.inBuildMode());
                if (!dm.inBuildMode()) {
                    dm.getCurrentDungeon().save();
                    p.sendMessage(OK + "Saved dungeon");
                }
                p.sendMessage(IF + "You are " + (dm.inBuildMode() ? "now" : "no longer") + " in build mode.");
            }
            case "exit" -> {
                if (dm.isEditing()) {
                    Dungeon d = dm.getCurrentDungeon();
                    d.save();
                    d.removeViewer(dm);
                    d.removeEditor(dm);
                    dm.setCurrentDungeon(null);
                    p.sendMessage(IF + "Finished and saved dungeon " + d.getName());
                } else p.sendMessage(W + "You're not editing a dungeon");
            }
            case "show" -> {
                if (dm.isEditing()) {
                    dm.getCurrentDungeon().showDungeonFor(dm);
                    p.sendMessage(OK + "Showing dungeon " + dm.getCurrentDungeon().getDungeonInfo().display_name);
                } else {
                    Dungeon d = Dungeon.get(p.getLocation());
                    if (d != null) d.showDungeonFor(dm);
                    else p.sendMessage(W + "No dungeon to show.");
                }
            }
            case "hide" -> {
                if (dm.isEditing()) dm.getCurrentDungeon().removeViewer(dm);
                p.sendMessage(IF + "Hid " + dm.getCurrentDungeon().getDungeonInfo().display_name + " selections");
            }
            case "teleport" -> {
                if (!dm.isEditing()) return ER + "You must be editing a dungeon to teleport!";
                else {
                    Dungeon d = dm.getCurrentDungeon();
                    if (args.length == 1) dm.teleport(d.getMains().getCenter());
                    else switch (args[1]) {
                        case "dungeon" -> dm.teleport(d.getMains().getCenter());
                        case "section" -> {
                            Dungeon.Section s = d.getSection(p);
                            if (args.length >= 3) {
                                s = d.getSection(args[2]);
                                if (s == null) return ER + "No section named \"" + args[2] + "\"";
                            } else if (s == null) return W + "You're currently not in a section.";
                            dm.teleport(s.getCenter());
                        }
                        default -> p.sendMessage(ER + "Unknown argument " + args[1]);
                    }
                }
            }
            default -> p.sendMessage(ER + "Unknown argument " + args[0]);
        }
        return r;
    }

    public interface TabCompleteRunnable { List<String> complete(); }
    public static final Map<Type, TabCompleteRunnable> acTabMap;
    static {
        acTabMap = new HashMap<>();
        acTabMap.put(Type.MODIFY_BLOCK, () -> Arrays.stream(ModifyBlock.ModType.values()).map(Enum::name).toList());
    }

    public static @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        final List<String> s = new ArrayList<>();
        if (sender instanceof ConsoleCommandSender console) {
            return s;
        }
        Player p = (Player) sender;
        DungeonMaster dm = DungeonMaster.getOrNew(p);
        if (args.length <= 1) {
            s.addAll(List.of("wand", "pos1", "pos2", "hide", "show", "create", "delete", "deselect"));
            if (dm.isEditing()) s.addAll(List.of("run", "reset", "rename", "save", "manage", "exit", "build"));
            else s.addAll(List.of("edit"));
        } else switch (args[0].toLowerCase()) {
            case "pos1", "pos2" -> {
                Block b = p.getTargetBlockExact(50);
                Location l;
                if (b == null) l = p.getLocation();
                else l = b.getLocation();
                s.add(String.valueOf(switch (args.length) {
                    case 2 -> (int) l.getX();
                    case 3 -> (int) l.getY();
                    case 4 -> (int) l.getZ();
                    default -> "";
                }));
            }
            case "create" -> {
                if (dm.isEditing()) {
                    if (args.length == 2) {
                        s.addAll(List.of("section", "action", "trigger", "condition"));
                    } else {
                        if (args.length >= 4) {
                            Type at = Type.get(args[2]);
                            if (at != null) return DA_item.getTabCompleteFor(at, Arrays.copyOfRange(args, 3, args.length));
                        } else switch (args[1].toLowerCase()) {
                            case "action" -> { return Arrays.stream(Type.values()).filter(Type::isAction).map(e -> e.name().toLowerCase()).toList(); }
                            case "condition" -> { return Arrays.stream(Type.values()).filter(Type::isCondition).map(e -> e.name().toLowerCase()).toList(); }
                        }
                    }
                } else if (args.length == 2) s.add("dungeon");
            }
            case "delete" -> {
                if (args.length == 2) {
                    s.addAll(List.of("dungeon", "section", "item"));
                } else switch (args[1]) {
                    case "dungeon" -> { if (args.length == 3) return getDungeonNames(); }
                    case "section" -> { if (args.length == 3) return getSectionNames(dm.getCurrentDungeon()); }
                    case "item" -> {

                    }
                }
            }
            case "manage", "rename" -> {
                if (!dm.isEditing()) return s;
                if (args.length == 2) s.addAll(List.of("dungeon", "section", "item"));
                else switch (args[1]) {
                    case "dungeon" -> { if (args.length == 3) return getDungeonNames(); }
                    case "section" -> { if (args.length == 3) return getSectionNames(dm.getCurrentDungeon()); }
                    case "item" -> {

                    }
                }
            }
            case "teleport" -> {
                if (!dm.isEditing()) return s;
                if (args.length == 2) s.addAll(List.of("dungeon", "section"));
                else switch (args[1]) {
                    case "dungeon" -> { if (args.length == 3) return getDungeonNames(); }
                    case "section" -> { if (args.length == 3) return getSectionNames(dm.getCurrentDungeon()); }
                }
            }
            case "deselect" -> { if (args.length == 2) return List.of("blocks", "selection"); }
            case "edit", "show", "hide" -> { if (!dm.isEditing() && args.length == 2) return getDungeonNames(); }
        }
        return s;
    }

    public static List<String> getDungeonNames() { return Dungeon.dungeons.stream().map(Manageable::getName).toList(); }
    public static List<String> getSectionNames(@NotNull Dungeon d) { return d.getSections().stream().map(Manageable::getName).toList(); }

    public static final class tab implements TabCompleter {
        @Override
        public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
            return complete(sender, command, label, args).stream().filter(s -> s.toLowerCase().startsWith(args[args.length-1].toLowerCase())).toList();
        }
    }
}
