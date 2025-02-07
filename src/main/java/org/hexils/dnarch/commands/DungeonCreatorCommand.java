package org.hexils.dnarch.commands;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hetils.mpdl.Manageable;
import org.hetils.mpdl.command.Command;
import org.hexils.dnarch.*;
import org.hexils.dnarch.items.EntitySpawn;
import org.hexils.dnarch.items.Type;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DungeonCreatorCommand extends Command {
    public DungeonCreatorCommand() { super("dc"); }

    public static List<String> getSectionNames(Dungeon d) {  return d == null ? List.of() : d.getSections().stream().map(DAManageable::getName).toList(); }

    public static List<String> getDungeonNames() { return Dungeon.dungeons.stream().map(DAManageable::getName).toList(); }

    private static class PosCom extends Command {
        private final boolean f;
        public PosCom(String s, boolean f) { super(s); this.f = f; }

        @Override
        public boolean execute(CommandSender sender, String @NotNull [] args) {
            setSelectionPoint(DungeonMaster.get(sender), args, f);
            return true;
        }

        static void setSelectionPoint(DungeonMaster dm, @NotNull String @NotNull [] args, boolean a) {
            //TODO make this a bit more advanced
            Block block;
            if (args.length > 0) {
                if (args.length > 2) {
                    block = dm.getWorld().getBlockAt(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                } else {
                    dm.sendError("Please provide valid x, y, z coordinates!");
                    return;
                }
            } else block = dm.getLocation().getBlock();
            if (a) dm.setSelectionA(block);
            else dm.setSelectionB(block);
            dm.sendInfo(DungeonMaster.Sender.CREATOR, "Set " + (a ? "1st" : "2nd") + " position to " + org.hetils.mpdl.location.LocationUtil.toReadableFormat(dm.getSelectionA()));
        }
    }
    private static class TelepCom extends Command {
        TelepCom(String s) { super(s); }

        @Override
        public boolean execute(CommandSender sender, String @NotNull [] args) {
            DungeonMaster dm = DungeonMaster.get(sender);
            //TODO figure this out
            if (dm.isEditing()) {
                Dungeon d = dm.getCurrentDungeon();
                if (args.length == 0) dm.teleport(d.getMains().getCenter());
                else switch (args[0]) {
                    case "dungeon" -> dm.teleport(d.getMains().getCenter());
                    case "section" -> {
                        Dungeon.Section s = d.getSection(dm);
                        if (args.length >= 2) {
                            s = d.getSection(args[1]);
                            if (s == null) dm.sendError("No section named \"" + args[1] + "\"");
                        } else if (s == null) dm.sendWarning(DungeonMaster.Sender.CREATOR, "You're currently not in a section.");
                        dm.teleport(s.getCenter());
                    }
                    default -> dm.sendError(DungeonMaster.Sender.CREATOR, "Unknown argument " + args[1]);
                }
            }
            return true;
        }
    }

    {
        TabCompleteFunction getDGNames = (s, args) -> getDungeonNames();
        TabCompleteFunction dsi = (s, a) -> List.of("dungeon", "section", "item");

        this.addSubCommand(new Command("wand") {
            { helpmsg = "Gives the player a wand, which is used to select block and create selections to create items, dungeon and sections"; }
            @Override
            public boolean execute(CommandSender sender, String @NotNull [] args) {
                DungeonMaster.get(sender).giveItem(Main.wand);
                return true;
            }
        });

        Command create = new Command("create") {
            @Override
            public boolean execute(CommandSender sender, String @NotNull [] args) {
                DungeonMaster dm = DungeonMaster.get(sender);
                switch (args[0].toLowerCase()) {
                    case "section", "action", "condition", "trigger" -> dm.sendError(DungeonMaster.Sender.CREATOR, "You must be editing a dungeon to create elements!");
                    default -> {
                        return super.execute(sender, args);
                    }
                }
                return true;
            }
        };
        create.addSubCommand(
                new Command("dungeon") {
                    { helpmsg = "Create a dungeon with a main sector, called \"Main_Sector\" respectively, using the user selected area as bounds for said section"; }
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get((Player) sender);
                        if (dm.isEditing()) {
                            dm.sendError("You can't create dungeons while editing!");
                            return true;
                        }
                        if (dm.hasAreaSelected()) {
                            Dungeon d;
                            Dungeon.Section si = Dungeon.getIntersectedSection(dm.getSelectedArea());
                            if (si == null) {
                                if (args.length >= 2) {
                                    try {
                                        d = new Dungeon(dm.getUniqueId(), args.length == 2 ? args[1] : String.join("_", Arrays.copyOfRange(args, 1, args.length)), dm.getSelectedArea());
                                    } catch (Dungeon.DuplicateNameException ignore) {
                                        dm.sendError(DungeonMaster.Sender.CREATOR, "Dungeon " + args[1] + " already exists.");
                                        return true;
                                    } catch (Dungeon.DungeonIntersectViolation e) {
                                        throw new RuntimeException(e);
                                    }
                                } else d = Dungeon.create(dm.getUniqueId(), dm.getSelectedArea());
                                dm.clearSelection();
                                d.showDungeonFor(dm);
                                dm.sendMessage(DungeonMaster.Sender.CREATOR, "Created new dungeon " + d.getName());
                                dm.editDungeon(d);
                            } else dm.sendError("Cannot create dungeon, selection intersects sector \"" + si.getName() + "\" in dungeon \"" + si.getDungeon().getDungeonInfo().display_name);
                        } else dm.sendError(DungeonMaster.Sender.CREATOR, "You must select a section to create a dungeon");
                        return true;
                    }
                },
                new Command("section") {
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get((Player) sender);
                        if (dm.hasAreaSelected()) {
                            if (dm.isEditing()) {
                                Dungeon.Section is = Dungeon.getIntersectedSection(dm.getSelectedArea());
                                if (is == null || dm.getCurrentDungeon().getSections().contains(is)) {
                                    Dungeon d = dm.getCurrentDungeon();
                                    Dungeon.Section s;
                                    if (args.length > 0) s = d.newSection(dm.getSelectedArea(), args[0]);
                                    else s = d.newSection(dm.getSelectedArea());
                                    dm.clearSelection();
                                    dm.sendPass(DungeonMaster.Sender.CREATOR, "Created \"" + d.getName() + "\" section \"" + s.getName() + "\"");
                                } else dm.sendError(DungeonMaster.Sender.CREATOR, "Cannot create section, selection intersects sector \"" + is.getName() + "\" in dungeon \"" + is.getDungeon().getDungeonInfo().display_name);
                            } else dm.sendError(DungeonMaster.Sender.CREATOR, "You must be currently editing a dungeon to create sections!");
                        } else dm.sendError(DungeonMaster.Sender.CREATOR, "You need to first select an area to create a section!");
                        return true;
                    }
                },
                new Command("action") {
                @Override
                public boolean execute(CommandSender sender, String @NotNull [] args) {
                    DungeonMaster dm = DungeonMaster.get((Player) sender);
                    if (args.length == 0) dm.sendError("Please specify the type!");
                    else {
                        Type t = Type.get(args[0]);
                        if (t != null) dm.giveItem(DAItem.commandNew(t, dm, Arrays.copyOfRange(args, 1, args.length)));
                        else dm.sendError(DungeonMaster.Sender.CREATOR, "Unknown type " + ChatColor.ITALIC + args[0].toLowerCase());
                    }
                    return true;
                }
            },
                new Command("entity") {
                @Override
                public boolean execute(CommandSender sender, String @NotNull [] args) {
                    DungeonMaster dm = DungeonMaster.get(sender);
                    EntitySpawn es = (EntitySpawn) Type.ENTITY_SPAWN.create(dm, args);
                    es.doAction(dm, "edit", null);
                    return true;
                }
            }
        );
        this.addSubCommand(
                create,
                new PosCom("pos1", true),
                new PosCom("pos2", false),
                new TelepCom("tp"),
                new TelepCom("teleport"),
                new Command("deselect", (s, a)->List.of("blocks", "section")) {
                    { helpmsg = "Deselects any selected blocks or area"; }
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get(sender);
                        if (args.length == 0) {
                            dm.deselectBlocks();
                            dm.clearSelection();
                        } else {
                            if (args[0].equalsIgnoreCase("blocks")) dm.deselectBlocks();
                            else if (args[0].equalsIgnoreCase("selection")) dm.clearSelection();
                        }
                        return true;
                    }
                },
                new Command("delete") {
                    {
                        addSubCommand(
                                new Command("dungeon", getDGNames) {
                                    @Override
                                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                                        DungeonMaster dm = DungeonMaster.get(sender);
                                        Dungeon d = dm.getCurrentDungeon();
                                        if (args.length >= 1) {
                                            d = Dungeon.get(args[0]);
                                            if (d == null) dm.sendError("No dungeon named \"" + args[2] + "\"");
                                            if (dm.getCurrentDungeon() == d) {
                                                d.removeViewer(dm);
                                                dm.editDungeon(null);
                                            }
                                            d.attemptRemove(dm);
                                        } else {
                                            if (dm.isEditing()) d.attemptRemove(dm);
                                            else dm.sendWarning(DungeonMaster.Sender.CREATOR, "You're currently not editing a dungeon");
                                        }
                                        return false;
                                    }
                                    },
                                new Command("section", (s, args)-> {
                                    DungeonMaster dm = DungeonMaster.get(s);
                                    return dm.isEditing() ? dm.getCurrentDungeon().getSections().stream().map(Manageable::getName).toList() : new ArrayList<>();
                                }) {
                                    @Override
                                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                                        DungeonMaster dm = DungeonMaster.get(sender);
                                        Dungeon d = dm.getCurrentDungeon();
                                        Dungeon.Section s = d.getSection(dm);
                                        if (args.length >= 1) {
                                            s = d.getSection(args[0]);
                                            if (s == null) dm.sendError("No section named \"" + args[0] + "\"");
                                        } else if (s == null) dm.sendWarning(DungeonMaster.Sender.CREATOR, "You're currently not in a section.");
                                        dm.promptRemove(s);
                                        return super.execute(sender, args);
                                    }
                                },
                                new Command("item") {
                                    @Override
                                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                                        DungeonMaster dm = DungeonMaster.get(sender);
                                        DAItem da = DAItem.get(dm.getInventory().getItemInMainHand());
                                        if (da != null) {
                                            dm.promptRemove(da);
                                        } else dm.sendError("You're currently not holding a DA item");
                                        return true;
                                    }
                                }
                                );
                    }
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get(sender);
                        DAItem da = DAItem.get(dm.getInventory().getItemInMainHand());
                        if (da != null) {
                            dm.promptRemove(da);
                        } else {
                            if (dm.isEditing()) {
                                Dungeon d = dm.getCurrentDungeon();
                                Dungeon.Section s = d.getSection(dm);
                                if (s != null) dm.promptRemove(s);
                                else d.attemptRemove(dm);
                            }
                        }
                        return true;
                    }
                },
                new Command("save") {
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get(sender);
                        if (!dm.isEditing()) dm.sendWarning(DungeonMaster.Sender.CREATOR, "You must be currently editing a dungeon to save it");
                        else {
                            dm.getCurrentDungeon().save();
                            dm.sendMessage(DungeonMaster.Sender.CREATOR, "Saved dungeon " + dm.getCurrentDungeon().getName());
                        }
                        return true;
                    }
                },
                new Command("run") {
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get(sender);
                        if (!dm.isEditing()) dm.sendWarning(DungeonMaster.Sender.CREATOR, "You must be currently editing a dungeon to run elements");
                        DAItem da = DAItem.get(dm.getInventory().getItemInMainHand());
                        if (da instanceof Action a)
                            a.trigger();
                        else if (da instanceof Trigger t) {
                            t.getActions().forEach(Action::trigger);
                        } else dm.sendWarning(DungeonMaster.Sender.CREATOR, "Please hold a executable item");
                        return true;
                    }
                },
                new Command("reset") {
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get(sender);
                        if (!dm.isEditing()) dm.sendWarning(DungeonMaster.Sender.CREATOR, "You must be currently editing a dungeon to reset elements");
                        DAItem da = DAItem.get(dm.getInventory().getItemInMainHand());
                        if (da instanceof Action a)
                            a.reset();
                        else if (da instanceof Trigger t)
                            t.getActions().forEach(Action::reset);
                        else dm.sendWarning(DungeonMaster.Sender.CREATOR, "Please hold a resetable item");
                        return true;
                    }
                },
                new Command("rename") {
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get(sender);
                        if (!dm.isEditing()) dm.sendError("You must be editing a dungeon to rename elements");
                        else {
                            Dungeon d = dm.getCurrentDungeon();
                            if (args.length == 0) {
                                DAItem a = DAItem.get(dm.getInventory().getItemInMainHand());
                                if (a != null) a.rename(dm);
                                else {
                                    Dungeon.Section s = d.getSection(dm);
                                    if (s != null) s.rename(dm);
                                    else d.rename(dm);
                                }
                            }
                            else switch (args[0]) {
                                case "dungeon" -> d.rename(dm);
                                case "section" -> {
                                    Dungeon.Section s = d.getSection(dm);
                                    if (args.length >= 2) {
                                        s = d.getSection(args[1]);
                                        if (s == null) dm.sendError("No section named \"" + args[1] + "\"");
                                        return true;
                                    } else if (s == null) {
                                        dm.sendWarning(DungeonMaster.Sender.CREATOR, "You're currently not in a section.");
                                        return true;
                                    }
                                    s.rename(dm);
                                }
                                case "item" -> {
                                    DAItem a = DAItem.get(dm.getInventory().getItemInMainHand());
                                    if (a != null) a.rename(dm);
                                    else dm.sendWarning(DungeonMaster.Sender.CREATOR, "Please hold a renameable item");
                                }
                                default -> dm.sendError(DungeonMaster.Sender.CREATOR, "Unknown argument " + args[1]);
                            }
                        }
                        return true;
                    }
                },
                new Command("manage") {
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get(sender);
                        Dungeon d = dm.getCurrentDungeon();
                        if (!dm.isEditing()) {
                            d = Dungeon.get(dm);
                            if (d != null) {
                                dm.editDungeon(d);
                                dm.manage(d);
                            } else dm.sendWarning(DungeonMaster.Sender.CREATOR, "You must be editing a dungeon to manage elements!");
                        } else {
                            if (args.length == 0) {
                                Dungeon.Section s = dm.getSection();
                                if (s != null) dm.manage(s);
                                else dm.manage(d);
                            }
                            else switch (args[0]) {
                                case "dungeon" -> dm.manage(d);
                                case "section" -> {
                                    Dungeon.Section s = d.getSection(dm);
                                    if (args.length >= 2) {
                                        s = d.getSection(args[1]);
                                        if (s == null)
                                            dm.sendError(DungeonMaster.Sender.CREATOR, "No section named \"" + args[1] + "\"");
                                    } else if (s == null)
                                        dm.sendWarning(DungeonMaster.Sender.CREATOR, "You're currently not in a section.");
                                    dm.manage(s);
                                }
                                case "item" -> {
                                    DAItem da = DAItem.get(dm.getInventory().getItemInMainHand());
                                    if (da != null) dm.manage(da);
                                    else dm.sendWarning(DungeonMaster.Sender.CREATOR, "Please hold a managable item!");
                                }
                                default -> dm.sendError(DungeonMaster.Sender.CREATOR, "Unknown type " + args[0]);
                            }
                        }
                        return true;
                    }
                },
                new Command("edit", getDGNames) {
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get(sender);
                        if (!dm.isEditing()) {
                            Dungeon d;
                            if (args.length == 1) {
                                d = Dungeon.get(args[0]);
                                if (d == null) {
                                    dm.sendError("No dungeon named \"" + args[0] + "\"");
                                    return true;
                                }
                            } else {
                                d = Dungeon.get(dm.getLocation());
                                if (d == null) {
                                    dm.sendError("You're currently not in a dungeon, please go into the desired dungeon or specify one.");
                                    return true;
                                }
                            }
                            dm.editDungeon(d);
                            d.showDungeonFor(dm);
                        } else dm.sendWarning(DungeonMaster.Sender.CREATOR, "You're already editing dungeon \"" + dm.getCurrentDungeon().getName() + "\"!");
                        return true;
                    }
                },
                new Command("build") {
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get(sender);
                        if (!dm.isEditing()) dm.sendWarning(DungeonMaster.Sender.CREATOR, "You must be editing a dungeon to enable build mode");
                        else {
                            dm.setBuildMode(!dm.inBuildMode());
                            if (!dm.inBuildMode()) {
                                dm.getCurrentDungeon().save();
                                dm.sendPass(DungeonMaster.Sender.CREATOR, "Saved dungeon");
                            }
                            dm.sendInfo(DungeonMaster.Sender.CREATOR, "You are " + (dm.inBuildMode() ? "now" : "no longer") + " in build mode.");
                        }
                        return true;
                    }
                },
                new Command("exit") {
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get(sender);
                        if (dm.isEditing()) {
                            Dungeon d = dm.getCurrentDungeon();
                            d.save();
                            d.removeViewer(dm);
                            d.removeEditor(dm);
                            dm.editDungeon(null);
                            dm.sendInfo(DungeonMaster.Sender.CREATOR, "Finished and saved dungeon " + d.getName());
                        } else dm.sendWarning(DungeonMaster.Sender.CREATOR, "You're not editing a dungeon");
                        return true;
                    }
                },
                new Command("show") {
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get(sender);
                        Dungeon d = dm.getCurrentDungeon();
                        if (args.length > 0) {
                            d = Dungeon.get(args[0]);
                            if (d == null) {
                                dm.sendWarning(DungeonMaster.Sender.CREATOR, "No dungeon named " + args[0]);
                                return true;
                            }
                        } else if (d == null) {
                            d = Dungeon.get(dm.getLocation());
                            if (d == null) {
                                dm.sendWarning(DungeonMaster.Sender.CREATOR, "No dungeon to show.");
                                return true;
                            }
                        }
                        d.showDungeonFor(dm);
                        dm.sendPass(DungeonMaster.Sender.CREATOR, "Showing dungeon " + dm.getCurrentDungeon().getDungeonInfo().display_name);
                        return true;
                    }
                },
                new Command("hide") {
                    @Override
                    public boolean execute(CommandSender sender, String @NotNull [] args) {
                        DungeonMaster dm = DungeonMaster.get(sender);
                        if (dm.isEditing()) {
                            dm.getCurrentDungeon().removeViewer(dm);
                            dm.sendInfo(DungeonMaster.Sender.CREATOR, "Hid " + dm.getCurrentDungeon().getDungeonInfo().display_name + " selections");
                        } else {
                            dm.hideSelections();
                            dm.sendInfo(DungeonMaster.Sender.CREATOR, "Hid all current selections");
                        }
                        return true;
                    }
                }
        );
    }
}
