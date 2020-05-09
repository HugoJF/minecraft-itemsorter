package com.denerdtv.itemsorter;

import com.denerdtv.CommandSignDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.denerdtv.itemsorter.ItemSorter.SERVER_PREFIX;

public class ItemSorterSignListener implements CommandSignDefinition {
    private final Set<Location> inputs = new HashSet<>();

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

        Location location = this.getBaseLocationForSign(e.getBlock());

        if (location != null) {
            this.inputs.add(location);
            this.save();
        }
    }

    public void onSignBreak(BlockBreakEvent e) {
        e.getPlayer().sendMessage(SERVER_PREFIX + " Unregistered input chest!");

        Location location = this.getBaseLocationForSign(e.getBlock());

        if (location != null) {
            this.inputs.remove(location);
            this.save();
        }
    }

    public void onSignPhysics(BlockPhysicsEvent e) {
        if (e.getBlock().getType() != Material.AIR) {
            return;
        }

        Location location = this.getBaseLocationForSign(e.getBlock());

        if (location != null) {
            Bukkit.broadcastMessage(SERVER_PREFIX + " Unregistered input chest!");
            this.inputs.remove(location);
            this.save();
        }
    }

    public Location getBaseLocationForSign(Block block) {
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