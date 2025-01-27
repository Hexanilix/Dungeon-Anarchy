package org.hexils.dnarch.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.*;
import org.hexils.dnarch.Dungeon;
import org.hexils.dnarch.DungeonMaster;
import org.hexils.dnarch.Trigger;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public final class DungeonCreatorCommandExecutor implements CommandExecutor {

    public static final HashMap<Class<? extends DAItem>, Function<String[], List<String>>> tabCompletions = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String s = execute(sender, command, label, args);
        if (s != null) {

            sender.sendMessage("[DA] " + s);
        }
        return true;
    }

    public static ChatColor IF = ChatColor.AQUA;
    public static ChatColor OK = ChatColor.GREEN;
    public static ChatColor W = ChatColor.YELLOW;
    public static ChatColor ER = ChatColor.RED;

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
        DungeonMaster dm = DungeonMaster.get(p);
        String r = null;
        //TODO figure out [DA]
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
                return OK + "Set 1st position to " + org.hetils.mpdl.location.LocationUtil.toReadableFormat(dm.getSelectionA());
            }
            case "pos2" -> {
                if (args.length > 1) {
                    if (args.length > 3) {
                        dm.setSelectionB(p.getWorld().getBlockAt(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3])));
                    } else return ER + "Please provide x, y, z coordinates!";
                } else {
                    dm.setSelectionB(p.getLocation().getBlock());
                }
                return OK + "Set 2nd position to " + org.hetils.mpdl.location.LocationUtil.toReadableFormat(dm.getSelectionB());
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
                    String[] i_args = args.length < 3 ? new String[0] : Arrays.copyOfRange(args, 3, args.length);
                    if (args[1].equalsIgnoreCase("dungeon")) {
                        if (dm.isEditing()) return ER + "You can't create dungeon while editing!";
                        if (dm.hasAreaSelected()) {
                            Dungeon d;
                            Dungeon.Section si = Dungeon.getIntersectedSection(dm.getSelectedArea());
                            if (si == null) {
                                if (args.length >= 3) {
                                    try {
                                        d = new Dungeon(dm.getUniqueId(), args.length == 3 ? args[2] : String.join("_", Arrays.copyOfRange(args, 2, args.length)), dm.getSelectedArea());
                                    } catch (Dungeon.DuplicateNameException ignore) {
                                        return ER + "Dungeon " + args[2] + " already exists.";
                                    } catch (Dungeon.DungeonIntersectViolation e) {
                                        throw new RuntimeException(e);
                                    }
                                } else d = Dungeon.create(dm.getUniqueId(), dm.getSelectedArea());
                                dm.clearSelection();
                                d.showDungeonFor(dm);
                                dm.sendMessage(DungeonMaster.Sender.CREATOR, OK + "Created new dungeon " + d.getName());
                                dm.editDungeon(d);
                            } else
                                return ER + "Cannot create dungeon, selection intersects sector \"" + si.getName() + "\" in dungeon \"" + si.getDungeon().getDungeonInfo().display_name;
                        } else
                            dm.sendError(DungeonMaster.Sender.CREATOR, "You must select a section to create a dungeon");
                    } else {
                        if (dm.isEditing()) {
                            switch (args[1].toLowerCase()) {
                                case "section" -> {
                                    return dm.getCurrentDungeon().commandNewSection(dm, Arrays.copyOfRange(args, 2, args.length));
                                }
                                case "action", "condition" -> {
                                    if (args.length == 2) return ER + "Please specify the type!";
                                    else {
                                        Type t = Type.get(args[2]);
                                        if (t != null) dm.giveItem(DAItem.commandNew(t, dm, i_args));
                                        else return ER + "Unknown type " + ChatColor.ITALIC + args[2].toLowerCase();
                                    }
                                }
                                case "trigger" -> dm.giveItem(dm.getCurrentDungeon().newTrigger());
                                default -> dm.sendError(DungeonMaster.Sender.CREATOR, "Unknown type " + args[1]);
                            }
                        } else switch (args[1].toLowerCase()) {
                            case "section", "action", "condition", "trigger" -> dm.sendError(DungeonMaster.Sender.CREATOR, "You must be editing a dungeon to create elements!");
                            default -> dm.sendError(DungeonMaster.Sender.CREATOR, "Unknown argument " + ChatColor.ITALIC + args[1]);
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
                                dm.editDungeon(null);
                            }
                            d.attemptRemove(dm);
                        } else {
                            if (dm.isEditing()) d.attemptRemove(dm);
                            else return W + "You're currently not editing a dungeon";
                        }
                    }
                    case "section" -> {
                        Dungeon.Section s = d.getSection(p);
                        if (args.length >= 3) {
                            s = d.getSection(args[2]);
                            if (s == null) return ER + "No section named \"" + args[2] + "\"";
                        } else if (s == null) return W + "You're currently not in a section.";
                        dm.promptRemove(s);
                    }
                    case "item" -> {
                        DAItem da = DAItem.get(dm.getInventory().getItemInMainHand());
                        if (da != null) {
                            dm.promptRemove(da);
                        } else return ER + "You're currently not holding a DA item";
                    }
                }
            }
            case "save" -> {
                if (!dm.isEditing()) dm.sendWarning(DungeonMaster.Sender.CREATOR, "You must be currently editing a dungeon to save it");
                else {
                    dm.getCurrentDungeon().save();
                    dm.sendMessage(DungeonMaster.Sender.CREATOR, OK + "Saved dungeon " + dm.getCurrentDungeon().getName());
                }
            }
            case "run" -> {
                if (!dm.isEditing()) return W + "You must be currently editing a dungeon to run elements";
                DAItem da = DAItem.get(p.getInventory().getItemInMainHand());
                if (da instanceof Action a)
                    a.trigger();
                else if (da instanceof Trigger t) {
                  t.getActions().forEach(Action::trigger);
                } else dm.sendWarning(DungeonMaster.Sender.CREATOR, "Please hold a executable item");
            }
            case "reset" -> {
                if (!dm.isEditing()) return W + "You must be currently editing a dungeon to reset elements";
                DAItem da = DAItem.get(p.getInventory().getItemInMainHand());
                if (da instanceof Action a)
                    a.reset();
                else if (da instanceof Trigger t)
                    t.getActions().forEach(Action::reset);
                else dm.sendWarning(DungeonMaster.Sender.CREATOR, "Please hold a resetable item");
            }
            case "rename" -> {
                if (!dm.isEditing()) return ER + "You must be editing a dungeon to rename elements";
                else {
                    Dungeon d = dm.getCurrentDungeon();
                    if (args.length == 1) {
                        DAItem a = DAItem.get(p.getInventory().getItemInMainHand());
                        if (a != null) a.rename(p);
                        else {
                            Dungeon.Section s = d.getSection(p);
                            if (s != null) s.rename(p);
                            else d.rename(p);
                        }
                    }
                    else switch (args[1]) {
                        case "dungeon" -> d.rename(p);
                        case "section" -> {
                            Dungeon.Section s = d.getSection(p);
                            if (args.length >= 3) {
                                s = d.getSection(args[2]);
                                if (s == null) return ER + "No section named \"" + args[2] + "\"";
                            } else if (s == null) return W + "You're currently not in a section.";
                            s.rename(p);
                        }
                        case "item" -> {
                            DAItem a = DAItem.get(p.getInventory().getItemInMainHand());
                            if (a != null) a.rename(p);
                            else dm.sendWarning(DungeonMaster.Sender.CREATOR, "Please hold a renameable item");
                        }
                        default -> dm.sendError(DungeonMaster.Sender.CREATOR, "Unknown argument " + args[1]);
                    }
                }
            }
            case "manage" -> {
                Dungeon d = dm.getCurrentDungeon();
                if (!dm.isEditing()) {
                    d = Dungeon.get(dm);
                    if (d != null) {
                        dm.editDungeon(d);
                        dm.manage(d);
                    } else return W + "You must be editing a dungeon to manage elements!";
                } else {
                    if (args.length == 1) dm.manage(d);
                    else switch (args[1]) {
                        case "dungeon" -> dm.manage(d);
                        case "section" -> {
                            Dungeon.Section s = d.getSection(p);
                            if (args.length >= 3) {
                                s = d.getSection(args[2]);
                                if (s == null)
                                    dm.sendError(DungeonMaster.Sender.CREATOR, "No section named \"" + args[2] + "\"");
                            } else if (s == null)
                                dm.sendWarning(DungeonMaster.Sender.CREATOR, "You're currently not in a section.");
                            dm.manage(s);
                        }
                        case "item" -> {
                            DAItem da = DAItem.get(p.getInventory().getItemInMainHand());
                            if (da != null) dm.manage(da);
                            else dm.sendWarning(DungeonMaster.Sender.CREATOR, "Please hold a managable item!");
                        }
                        default -> dm.sendError(DungeonMaster.Sender.CREATOR, "Unknown type " + args[1]);
                    }
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
                    dm.editDungeon(d);
                    d.showDungeonFor(dm);
                } else dm.sendWarning(DungeonMaster.Sender.CREATOR, "You're already editing dungeon \"" + dm.getCurrentDungeon().getName() + "\"!");
            }
            case "build" -> {
                if (!dm.isEditing()) return W + "You must be editing a dungeon to enable build mode";
                dm.setBuildMode(!dm.inBuildMode());
                if (!dm.inBuildMode()) {
                    dm.getCurrentDungeon().save();
                    dm.sendMessage(DungeonMaster.Sender.CREATOR, OK + "Saved dungeon");
                }
                dm.sendInfo(DungeonMaster.Sender.CREATOR, "You are " + (dm.inBuildMode() ? "now" : "no longer") + " in build mode.");
            }
            case "exit" -> {
                if (dm.isEditing()) {
                    Dungeon d = dm.getCurrentDungeon();
                    d.save();
                    d.removeViewer(dm);
                    d.removeEditor(dm);
                    dm.editDungeon(null);
                    dm.sendInfo(DungeonMaster.Sender.CREATOR, "Finished and saved dungeon " + d.getName());
                } else dm.sendWarning(DungeonMaster.Sender.CREATOR, "You're not editing a dungeon");
            }
            case "show" -> {
                Dungeon d = dm.getCurrentDungeon();
                if (args.length > 1) {
                    d = Dungeon.get(args[1]);
                    if (d == null) return W + "No dungeon named " + args[1];
                } else if (d == null) {
                    d = Dungeon.get(p.getLocation());
                    if (d == null) return W + "No dungeon to show.";
                }
                d.showDungeonFor(dm);
                return OK + "Showing dungeon " + dm.getCurrentDungeon().getDungeonInfo().display_name;
            }
            case "hide" -> {
                if (dm.isEditing()) {
                    dm.getCurrentDungeon().removeViewer(dm);
                    return IF + "Hid " + dm.getCurrentDungeon().getDungeonInfo().display_name + " selections";
                } else {
                    dm.hideSelections();
                    return IF + "Hid all current selections";
                }
            }
            case "tp" -> {
                //TODO figure this out
                if (dm.isEditing()) {
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
                        default -> dm.sendError(DungeonMaster.Sender.CREATOR, "Unknown argument " + args[1]);
                    }
                }
            }
            default -> dm.sendError(DungeonMaster.Sender.CREATOR, "Unknown argument " + args[0]);
        }
        return r;
    }

    public static @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> s = new ArrayList<>();
        if (sender instanceof ConsoleCommandSender console) {
            return s;
        }
        Player p = (Player) sender;
        DungeonMaster dm = DungeonMaster.get(p);
        if (args.length <= 1) {
            s.addAll(List.of("wand", "pos1", "pos2", "hide", "show", "create", "delete", "deselect", "edit", "tp"));
            if (dm.isEditing()) {
                s.addAll(List.of("run", "reset", "rename", "save", "exit", "build"));
            }
            if (dm.isEditing() || Dungeon.get(p) != null) s.add("manage");
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
                            if (at != null) {
                                Function<String[], List<String>> f = tabCompletions.get(at.getDAClass());
                                if (f != null) {
                                    s = f.apply(Arrays.copyOfRange(args, 3, args.length));
                                    if (s == null) s = new ArrayList<>();
                                }
                                return s;
                            }
                        } else switch (args[1].toLowerCase()) {
                            case "action" -> { return Arrays.stream(Type.values()).filter(t -> t.isAction() && t.isCreatable()).map(e -> e.name().toLowerCase()).toList(); }
                            case "condition" -> { return Arrays.stream(Type.values()).filter(t -> t.isCondition() && t.isCreatable()).map(e -> e.name().toLowerCase()).toList(); }
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

    public static List<String> getDungeonNames() { return Dungeon.dungeons.stream().map(DAManageable::getName).toList(); }
    public static List<String> getSectionNames(Dungeon d) {  return d == null ? List.of() : d.getSections().stream().map(DAManageable::getName).toList(); }

    public static final class tab implements TabCompleter {
        @Override
        public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
            return complete(sender, command, label, args).stream().filter(s -> s.toLowerCase().startsWith(args[args.length-1].toLowerCase())).toList();
        }
    }
}
