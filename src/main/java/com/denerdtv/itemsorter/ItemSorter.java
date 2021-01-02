package com.denerdtv.itemsorter;

import com.denerdtv.CommandSignManager;
import com.denerdtv.ParticleConfiguration;
import com.denerdtv.ParticleSystem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;

import static org.bukkit.ChatColor.*;

public class ItemSorter extends JavaPlugin implements CommandExecutor, Listener {
    // Constants
    public static final String NOTIFICATION = "notification";

    // Static variables
    public static final String COMMAND = "itemsorter";
    public static final String SERVER_PREFIX = BLACK + "[" + RED + "SORTER" + BLACK + "] " + RESET;
    private static ItemSorter instance = null;

    // Runtime
    private boolean enabled;
    private boolean visible;
    private ParticleSystem particleSystem;

    // References
    private CommandSignManager csm;
    private ItemSorterCommand isc;

    // Listeners
    private ItemSorterFrameListener ifl;
    private ItemSorterSignListener sl;

    // Stored references
    private PluginManager plugin;

    public ItemSorter() throws Exception {
        if (instance != null) {
            throw new Exception("Multiple singletons");
        } else {
            ItemSorter.instance = this;
        }
    }

    public static ItemSorter getInstance() {
        return ItemSorter.instance;
    }

    public void onEnable() {
        this.particleSystem = new ParticleSystem(Bukkit.getWorld("world"));

        this.plugin = Bukkit.getPluginManager();

        this.csm = new CommandSignManager();
        this.isc = new ItemSorterCommand();

        this.ifl = new ItemSorterFrameListener();
        this.sl = new ItemSorterSignListener();

        this.plugin.registerEvents(this, this);
        this.plugin.registerEvents(this.csm, this);
        this.plugin.registerEvents(this.ifl, this);

        this.csm.addDefinition(this.sl);

        particleSystem.addConfiguration(NOTIFICATION, ParticleConfiguration.create(Particle.VILLAGER_HAPPY).setAmount(15).setOffset(0.30));

        getCommand("itemsorter").setExecutor(this.isc);
        getCommand("itemsorter").setTabCompleter(this.isc);


        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::runTicks, 20L, 20L);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::showParticles, 20L, 20L);
    }


    private void runTicks() {
        Set<Location> inputs = this.sl.getInputs();

        for (Location location : inputs) {
            Chest input = this.getChestFromLocation(location);

            if (input == null) {
                continue;
            }

            Inventory inventory = input.getInventory();

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }

                List<Location> outputs = this.ifl.getLocations(item.getType());

                if (outputs == null || outputs.size() == 0) {
                    continue;
                }

                // TODO: undo me
                boolean found = false;
                int attempts = 0;

                while (!found && ++attempts < 10) {
                    int index = (int) Math.round(Math.random() * (outputs.size() - 1));

                    Location outputLocation = outputs.get(index);
                    Chest output = this.getChestFromLocation(outputLocation);

                    if (output == null) {
//                    Bukkit.broadcastMessage("There are no chests at output: " + outputLocation.toString());
                        continue;
                    }

                    output.getInventory().addItem(item.asOne());
                    inventory.removeItem(item.asOne());
                    found = true;
                }

                break;
            }
        }
    }

    private void showParticles() {
        if (!this.visible) {
            return;
        }

        // Inputs
        for (Location l : this.sl.getInputs()) {
            this.particleSystem.spawnCenter(NOTIFICATION, l);
        }

        // Outputs
        for (Material m : this.ifl.getMaterials()) {
            for (Location l : this.ifl.getLocations(m)) {
                this.particleSystem.spawnCenter(NOTIFICATION, l);
            }
        }
    }

    private Chest getChestFromLocation(Location location) {
        Block block = location.getBlock();

        if (block.getType() != Material.CHEST) {
            return null;
        }

        return (Chest) block.getState();
    }

    public void setTrackerEnabled(boolean b) {
        this.enabled = b;
    }

    public boolean getTrackedEnabled() {
        return this.enabled;
    }

    public void setVisible(boolean b) {
        this.visible = b;
    }

    public boolean getVisible() {
        return this.visible;
    }

    public ItemSorterSignListener getSignListener() {
        return this.sl;
    }

    public ItemSorterFrameListener getFrameListener() {
        return this.ifl;
    }
}