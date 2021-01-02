package com.denerdtv.itemsorter;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.denerdtv.itemsorter.ItemSorter.SERVER_PREFIX;
import static org.bukkit.ChatColor.GREEN;
import static org.bukkit.ChatColor.RESET;

public class ItemSorterFrameListener implements Listener {
    private final HashMap<Material, List<Location>> outputs = new HashMap<>();

    // Persistence
    private final File file;
    private final String configPath;
    private YamlConfiguration config;

    ItemSorterFrameListener() {
        this.configPath = ItemSorter.getInstance().getDataFolder() + "/outputs.yml";
        this.file = new File(configPath);

        this.config = YamlConfiguration.loadConfiguration(file);
        this.restore();
    }

    private void restore() {
        Bukkit.getConsoleSender().sendMessage("restoring");

        ConfigurationSection section = this.config.getConfigurationSection("outputs");

        if (section == null) {
            return;
        }

        Set<String> keys = section.getKeys(true);

        outputs.clear();

        for (String material : keys) {
            // Cast key and value
            List<?> locs = section.getObject(material, List.class);

            if (locs == null) {
                continue;
            }

            // Create list to add to map
            List<Location> locations = new LinkedList<>();

            outputs.put(Material.getMaterial(material), locations);

            for (Object loc : locs) {
                locations.add((Location) loc);
            }
        }
    }

    private void save() {
        Map<String, List<Location>> locs = new HashMap<>();

        for (Material material : this.outputs.keySet()) {
            locs.put(material.toString(), this.outputs.get(material));
        }

        this.config.set("outputs", locs);
        try {
            this.config.save(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onItemFrameAddItem(final PlayerInteractEntityEvent e) {
        // Don't track shit while disabled
        if (!ItemSorter.getInstance().getTrackedEnabled()) {
            return;
        }

        final Entity clicked = e.getRightClicked();
        EquipmentSlot hand = e.getHand();

        // Only check ItemFrames
        if (!(clicked instanceof ItemFrame)) {
            return;
        }

        ItemStack used = null;
        EntityEquipment equip = e.getPlayer().getEquipment();

        // Maybe using bare hands to rotate?
        if (equip == null) {
            return;
        }

        // Get correct item if OFF_HAND is used
        if (hand == EquipmentSlot.HAND) {
            used = e.getPlayer().getEquipment().getItemInMainHand();
        } else if (hand == EquipmentSlot.OFF_HAND) {
            used = e.getPlayer().getEquipment().getItemInOffHand();
        }

        if (used == null) {
            return;
        }

        final ItemFrame itemFrame = (ItemFrame) clicked;
        final ItemStack itemStack = itemFrame.getItem();
        final Block base = this.getBlockBehindItemFrame(itemFrame);

        // Check if it's on a chest
        if (base.getType() != Material.CHEST) {
            return;
        }

        // We are not placing an item
        if (itemStack.getType() != Material.AIR) {
            e.setCancelled(true);
            return;
        }

        // We are punching the frame
        if (used.getType() == Material.AIR) {
            return;
        }

        this.registerOutput(used.getType(), base.getLocation());
        this.save();

        String itemName = used.getType().toString();
//        int filterCount = this.outputs.get(used.getType()).size();
        int filterCount = this.getMaterialsFromOutput(base.getLocation());

        e.getPlayer().sendMessage(SERVER_PREFIX + " Added " + GREEN + itemName + RESET + " filter! This chest has " + filterCount + " filters");
    }

    @EventHandler
    public void onItemFrameBroken(final HangingBreakEvent e) {
        final Entity entity = e.getEntity();

        if (!(entity instanceof ItemFrame)) {
            return;
        }

        ItemFrame itemFrame = (ItemFrame) entity;
        Block behind = this.getBlockBehindItemFrame(itemFrame);

        // This is not a special ItemFrame
        if (behind.getType() != Material.CHEST) {
            return;
        }

        Material material = itemFrame.getItem().getType();

        this.unregisterOutput(material, behind.getLocation());
    }

    @EventHandler
    public void onItemFrameLoseItem(final EntityDamageByEntityEvent e) {
        final Entity entity = e.getEntity();

        if (!(entity instanceof ItemFrame)) {
            return;
        }

        ItemFrame itemFrame = (ItemFrame) entity;
        Block behind = this.getBlockBehindItemFrame(itemFrame);

        // This is not a special ItemFrame
        if (behind.getType() != Material.CHEST) {
            return;
        }

        Material material = itemFrame.getItem().getType();

        this.unregisterOutput(material, behind.getLocation());
    }

    private void registerOutput(Material material, Location location) {
        if (!this.outputs.containsKey(material)) {
            List<Location> locations = new LinkedList<Location>();
            this.outputs.put(material, locations);
        }

        this.outputs.get(material).add(location);
    }

    private void unregisterOutput(Material material, Location location) {
        List<Location> locations = this.outputs.get(material);

        if (locations == null) {
            return;
        }

        if (locations.remove(location)) {
            this.save();

//            int filterCount = this.outputs.get(material).size();
            int filterCount = this.getMaterialsFromOutput(location);
            Bukkit.broadcastMessage(SERVER_PREFIX + " Removed " + GREEN + material.toString() + RESET + " filter! This chest has " + filterCount + " filters");
        }
    }

    private Block getBlockBehindItemFrame(ItemFrame itemFrame) {
        final BlockFace backFacing = itemFrame.getFacing().getOppositeFace();

        return itemFrame.getLocation().getBlock().getRelative(backFacing);
    }

    public int getMaterialsFromOutput(Location location) {
        Set<Material> keys = this.outputs.keySet();

        int count = 0;
        for (Material m: keys) {
            List<Location> locations = this.outputs.get(m);

            if(locations.contains(location)) {
                count++;
            }
        }

        return count;
    }

    public List<Location> getLocations(Material material) {
        return this.outputs.get(material);
    }

    public Set<Material> getMaterials() {
        return this.outputs.keySet();
    }

    public int getOutputCount() {
        int count = 0;
        Collection<List<Location>> values = this.outputs.values();

        for (List<Location> locations : values) {
            count += locations.size();
        }

        return count;
    }
}
