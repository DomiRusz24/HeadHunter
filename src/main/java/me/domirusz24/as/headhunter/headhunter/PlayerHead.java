package me.domirusz24.as.headhunter.headhunter;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PlayerHead {

    public static String name;
    public static List<String> description = new ArrayList<>();
    private static Random random = new Random();

    public static List<String> getDescription() {
        return description;
    }

    public static String getName() {
        return name;
    }

    public static Quality getRandomQuality() {
        int i = random.nextInt(Quality.getRngNumber() - 1) + 1;
        for (Integer tier : Quality.getAllQualities().keySet()) {
            if (Quality.getAllQualities().get(tier).getChance() > i) {
                return Quality.getAllQualities().get(tier);
            }
        }
        return Quality.getAllQualities().get(1);
    }

    private ItemStack playerHead;
    private NBTItem nbtItem;

    public PlayerHead(Player target, Player player) {
        playerHead = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        nbtItem = new NBTItem(playerHead);
        setTier(getRandomQuality().getTier());
        setTarget(target.getName());
        setKiller(player.getName());
        new BukkitRunnable() {
            @Override
            public void run() {
                int id = HeadHunter.plugin.getLatestID(target.getName());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        setID(id);
                        updateHead();
                        player.getInventory().addItem(playerHead);
                    }
                }.runTask(HeadHunter.plugin);
            }
        }.runTaskAsynchronously(HeadHunter.plugin);
    }

    private PlayerHead(ItemStack playerHead, int tier, int id, String target, String killer) {
        this.playerHead = playerHead;
        this.nbtItem = new NBTItem(playerHead);
        setTier(tier);
        setTarget(target);
        setKiller(killer);
        setID(id);
        updateHead();
    }

    private String applyPlaceHolders(String s) {
        return (ChatColor.translateAlternateColorCodes('&', s
                .replace("%target%", getTarget())
                .replace("%id%", String.valueOf(getId()))
                .replace("%quality%", getQuality().getQualityName()))
                .replace("%tier%", String.valueOf(getTier()))
                .replace("%player%", getKiller()))
                .replace("%tiersign%", getQuality().getTierSign());
    }

    public void upgradeHead() {
        Quality quality = Quality.getAllQualities().get(getTier() + -1);
        if (quality != null) {
            setTier(getTier() + -1);
            updateHead();
        }
    }

    public void updateHead() {
        saveToItem();
        playerHead.setType(Material.SKULL_ITEM);
        playerHead.setDurability((short) 3);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        meta.setDisplayName(applyPlaceHolders(getName()));
        ArrayList<String> list = new ArrayList<>();
        for (String s : getDescription()) {
            list.add(applyPlaceHolders(s));
        }
        meta.setLore(list);
        meta.setOwner(getTarget());
        playerHead.setItemMeta(meta);
    }

    public int getTier() {
        return (int) (double) nbtItem.getDouble("head_tier");
    }

    public ItemStack getPlayerHead() {
        return playerHead;
    }

    public Quality getQuality() {
        return Quality.getAllQualities().get(getTier());
    }

    public int getId() {
        return (int) (double) nbtItem.getDouble("head_id");
    }

    public String getKiller() {
        return nbtItem.getString("head_killer");
    }

    public String getTarget() {
        return nbtItem.getString("head_target");
    }

    public void saveToItem() {
        nbtItem.applyNBT(playerHead);
    }

    public void setTier(int tier) {
        nbtItem.setDouble("head_tier", (double) tier);
    }

    public void setID(int id) {
        nbtItem.setDouble("head_id", (double) id);
    }

    public void setKiller(String killer) {
        nbtItem.setString("head_killer", killer);
    }

    public void setTarget(String target) {
        nbtItem.setString("head_target", target);
    }

    public static PlayerHead getFromItem(ItemStack item) {
        if (item.getItemMeta() == null || !item.getType().equals(Material.SKULL_ITEM) || item.getDurability() != (short) 3) {
            return null;
        }
        NBTItem nbtItem = new NBTItem(item);
        if (nbtItem.hasKey("head_tier")) {
            int id = (int) (double) nbtItem.getDouble("head_id");
            int tier = (int) (double) nbtItem.getDouble("head_tier");
            String killer = nbtItem.getString("head_killer");
            String target = nbtItem.getString("head_target");
            return new PlayerHead(item, tier, id, target, killer);
        } else {
            return null;
        }

    }

}
