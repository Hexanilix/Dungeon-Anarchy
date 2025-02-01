package org.hexils.dnarch.commands;

import org.hetils.mpdl.command.Command;

import java.util.*;
import java.util.function.Function;

public final class DungeonCreatorCommandExecutor extends Command {
    public DungeonCreatorCommandExecutor(String command) {
        super(command);
    }

    public DungeonCreatorCommandExecutor(String command, boolean console_executable) {
        super(command, console_executable);
    }

//    public static final HashMap<Class<? extends DAItem>, Function<String[], List<String>>> tabCompletions = new HashMap<>();
//
//    @Override
//    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
//        execute(sender, command, label, args);
//        return true;
//    }
//
//    public interface RunPlayerSubcommand { void run(Player p, @NotNull String @NotNull [] args); }
//    public interface RunDMSubcommand extends RunPlayerSubcommand {
//        void run(DungeonMaster dm, @NotNull String @NotNull [] args);
//        @Override
//        default void run(Player p, @NotNull String @NotNull [] args) { run(DungeonMaster.get(p), args); }
//    }
//
//    public static final HashMap<String, RunDMSubcommand> player_executors = new HashMap<>();
//    public static final HashMap<String, String> player_executor_help = new HashMap<>();
//
//    public static void addSubcommand(String sub, String desc, RunDMSubcommand dms) { addSubcommand(sub, "", desc, dms); }
//    public static void addSubcommand(String sub, String args, String description, RunDMSubcommand dms) {
//        player_executors.put(sub, dms);
//        player_executor_help.put(sub, description);
//    }
//
//
//
//    public static @Nullable void execute(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
//        if (args.length == 0) {
//            //TODO dc usage message
//            return;
//        }
//        if (sender instanceof ConsoleCommandSender console) {
//            return;
//        }
//        if (!sender.isOp()) WrappedPlayer.get((Player) sender).sendError("You cannot use this command!");
//        DungeonMaster dm = DungeonMaster.get((Player) sender);
//        RunDMSubcommand dms = player_executors.get(args[0]);
//        if (dms != null) dms.run(dm, Arrays.copyOfRange(args, 1, args.length));
//        else dm.sendError(DungeonMaster.Sender.CREATOR, "Unknown argument " + args[0]);
//    }
//
//    public static @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
//        List<String> s = new ArrayList<>();
//        if (sender instanceof ConsoleCommandSender console) {
//            return s;
//        }
//        Player p = (Player) sender;
//        DungeonMaster dm = DungeonMaster.get(p);
//        if (args.length <= 1) {
//            s.addAll(List.of("wand", "pos1", "pos2", "hide", "show", "create", "delete", "deselect", "edit", "tp"));
//            if (dm.isEditing()) {
//                s.addAll(List.of("run", "reset", "rename", "save", "exit", "build"));
//            }
//            if (dm.isEditing() || Dungeon.get(p) != null) s.add("manage");
//        } else switch (args[0].toLowerCase()) {
//            case "pos1", "pos2" -> {
//                Block b = p.getTargetBlockExact(50);
//                Location l;
//                if (b == null) l = p.getLocation();
//                else l = b.getLocation();
//                s.add(String.valueOf(switch (args.length) {
//                    case 2 -> (int) l.getX();
//                    case 3 -> (int) l.getY();
//                    case 4 -> (int) l.getZ();
//                    default -> "";
//                }));
//            }
//            case "create" -> {
//                if (dm.isEditing()) {
//                    if (args.length == 2) {
//                        s.addAll(List.of("section", "action", "trigger", "condition"));
//                    } else {
//                        if (args.length >= 4) {
//                            Type at = Type.get(args[2]);
//                            if (at != null) {
//                                Function<String[], List<String>> f = tabCompletions.get(at.getDAClass());
//                                if (f != null) {
//                                    s = f.apply(Arrays.copyOfRange(args, 3, args.length));
//                                    if (s == null) s = new ArrayList<>();
//                                }
//                                return s;
//                            }
//                        } else switch (args[1].toLowerCase()) {
//                            case "action" -> { return Arrays.stream(Type.values()).filter(t -> t.isAction() && t.isCreatable()).map(e -> e.name().toLowerCase()).toList(); }
//                            case "condition" -> { return Arrays.stream(Type.values()).filter(t -> t.isCondition() && t.isCreatable()).map(e -> e.name().toLowerCase()).toList(); }
//                        }
//                    }
//                } else if (args.length == 2) s.add("dungeon");
//            }
//            case "delete" -> {
//                if (args.length == 2) {
//                    s.addAll(List.of("dungeon", "section", "item"));
//                } else switch (args[1]) {
//                    case "dungeon" -> { if (args.length == 3) return getDungeonNames(); }
//                    case "section" -> { if (args.length == 3) return getSectionNames(dm.getCurrentDungeon()); }
//                    case "item" -> {
//
//                    }
//                }
//            }
//            case "manage", "rename" -> {
//                if (!dm.isEditing()) return s;
//                if (args.length == 2) s.addAll(List.of("dungeon", "section", "item"));
//                else switch (args[1]) {
//                    case "dungeon" -> { if (args.length == 3) return getDungeonNames(); }
//                    case "section" -> { if (args.length == 3) return getSectionNames(dm.getCurrentDungeon()); }
//                    case "item" -> {
//
//                    }
//                }
//            }
//            case "teleport" -> {
//                if (!dm.isEditing()) return s;
//                if (args.length == 2) s.addAll(List.of("dungeon", "section"));
//                else switch (args[1]) {
//                    case "dungeon" -> { if (args.length == 3) return getDungeonNames(); }
//                    case "section" -> { if (args.length == 3) return getSectionNames(dm.getCurrentDungeon()); }
//                }
//            }
//            case "deselect" -> { if (args.length == 2) return List.of("blocks", "selection"); }
//            case "edit", "show", "hide" -> { if (!dm.isEditing() && args.length == 2) return getDungeonNames(); }
//        }
//        return s;
//    }
//
//
//
//    public static final class tab implements TabCompleter {
//        @Override
//        public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
//            return complete(sender, command, label, args).stream().filter(s -> s.toLowerCase().startsWith(args[args.length-1].toLowerCase())).toList();
//        }
//    }
}
