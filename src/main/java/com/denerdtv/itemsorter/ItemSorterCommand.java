package com.denerdtv.itemsorter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

import static com.denerdtv.itemsorter.ItemSorter.SERVER_PREFIX;
import static org.bukkit.ChatColor.*;

public class ItemSorterCommand implements Listener, CommandExecutor, TabCompleter {
    private final ItemSorter is;

    public ItemSorterCommand() {
        this.is = ItemSorter.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase(ItemSorter.COMMAND)) {
            Player p = (Player) sender;

            if (args.length == 1 && args[0].equals("on")) {
                this.is.setTrackerEnabled(true);
                p.sendMessage(SERVER_PREFIX + "ItemSorter is now turned " + GREEN + "ON");
                return true;
            } else if (args.length == 1 && args[0].equals("off")) {
                this.is.setTrackerEnabled(false);
                p.sendMessage(SERVER_PREFIX + "ItemSorter is now turned " + RED + "OFF");
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("visible")) {
                is.setVisible(true);
                p.sendMessage(SERVER_PREFIX + "ItemSorter is now " + GREEN + "VISIBLE");
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("hide")) {
                is.setVisible(false);
                p.sendMessage(SERVER_PREFIX + "ItemSorter is now " + RED + "HIDDEN");
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
                int inputs = ItemSorter.getInstance().getSignListener().getInputs().size();
                int materialCount = ItemSorter.getInstance().getFrameListener().getMaterials().size();
                int outputCount = ItemSorter.getInstance().getFrameListener().getOutputCount();

                p.sendMessage(SERVER_PREFIX + "ItemSorter is tracking " + inputs + " input chests.");
                p.sendMessage(SERVER_PREFIX + "ItemSorter is tracking " + outputCount + " output chests with " + materialCount + " different material.");
            } else {
                p.sendMessage(SERVER_PREFIX + ("Usage: ItemSorter <" + RED + "on|off|visible|hide" + RESET + ">").replaceAll("\\|", RESET + "|" + RED));
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> cmds = new ArrayList<>();
        Player p = (Player) sender;

        // Generate TAB complete for AutoReplant
        if (cmd.getName().equalsIgnoreCase(ItemSorter.COMMAND)) {
            generateEnabledTabComplete(cmds, p);
            generateVisibilityTabComplete(cmds);
            generateStatusTabComplete(cmds);
        }

        // Filter entries based on already typed arguments
        filterBasedOnHints(args, cmds);

        return cmds;
    }

    private void generateStatusTabComplete(List<String> cmds) {
        cmds.add("status");
    }

    private void generateVisibilityTabComplete(List<String> cmds) {
        if (!is.getVisible()) cmds.add("visible");
        if (is.getVisible()) cmds.add("hide");
    }

    private void generateEnabledTabComplete(List<String> cmds, Player p) {
        boolean off = is.getTrackedEnabled();

        if (!off) cmds.add("on");
        if (off) cmds.add("off");
    }

    private void filterBasedOnHints(String[] args, List<String> cmds) {
        if (args.length >= 1) {
            for (int i = 0; i < cmds.size(); i++) {
                if (!cmds.get(i).startsWith(args[0])) {
                    cmds.remove(i--);
                }
            }
        }
    }
}
