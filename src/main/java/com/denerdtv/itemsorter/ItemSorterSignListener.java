package com.denerdtv.itemsorter;

import com.denerdtv.CommandSignDefinition;
import com.denerdtv.CommandSignManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.denerdtv.itemsorter.ItemSorter.SERVER_PREFIX;

public class ItemSorterSignListener implements CommandSignDefinition {
    private final Set<Location> inputs = new HashSet<>();

    private CommandSignManager manager;

    // Persistence
    private final File file;
    private final String configPath;
    private YamlConfiguration config;

    ItemSorterSignListener() {
        this.configPath = ItemSorter.getInstance().getDataFolder() + "/inputs.yml";
        this.file = new File(configPath);

        this.config = YamlConfiguration.loadConfiguration(file);
        this.restore();
    }

    public void restore() {
        List<?> locations = this.config.getList("inputs");

        if (locations == null) {
            return;
        }

        inputs.clear();

        for (Object s : locations) {
            this.inputs.add((Location) s);
        }
    }

    public void save() {
        List<Location> locations = new ArrayList<>(this.inputs);
        this.config.set("inputs", locations);

        try {
            this.config.save(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean handlesEvent(SignChangeEvent e) {
        String line = e.getLine(0);

        if (line == null) {
            return false;
        }

        if (!line.equalsIgnoreCase("[ItemSorter]")) {
            return false;
        }

        return true;
    }

    public void onSignCreate(SignChangeEvent e) {
        e.getPlayer().sendMessage(SERVER_PREFIX + " Registered input chest!");

        Location location = this.getBaseLocationFromSign(e.getBlock());

        if (location != null) {
            this.inputs.add(location);
            this.save();
        }
    }

    public void onSignBreak(BlockBreakEvent e) {
        e.getPlayer().sendMessage(SERVER_PREFIX + " Unregistered input chest!");

        Location location = this.getBaseLocationFromSign(e.getBlock());

        if (location != null) {
            this.inputs.remove(location);
            this.save();
        }
    }

    public void onSignPhysics(BlockPhysicsEvent e) {
        Location location = this.findBaseLocationFromLocation(e.getBlock().getLocation());

        if (location != null) {
            Bukkit.broadcastMessage(SERVER_PREFIX + " Unregistered input chest!");
            this.inputs.remove(location);
            this.save();
        } else {
            Bukkit.broadcastMessage("Could not find location from physics event");
        }
    }

    public Location findBaseLocationFromLocation(Location location) {
        List<Vector> vectors = Arrays.asList(
                new Vector(1, 0, 0),
                new Vector(-1, 0, 0),
                new Vector(0, 0, 1),
                new Vector(0, 0, -1)
        );

        for (Vector v : vectors) {
            Location loc = location.clone().add(v);

            if (this.inputs.contains(loc)) {
                return loc;
            }
        }

        return null;
    }

    public Location getBaseLocationFromSign(Block block) {
        BlockData blockData = block.getBlockData();

        if (!(blockData instanceof WallSign)) {
            return null;
        }

        Directional directional = (Directional) blockData;
        BlockFace opposite = directional.getFacing().getOppositeFace();
        Block relative = block.getRelative(opposite);

        return relative.getLocation();
    }

    public Set<Location> getInputs() {
        return this.inputs;
    }
}
