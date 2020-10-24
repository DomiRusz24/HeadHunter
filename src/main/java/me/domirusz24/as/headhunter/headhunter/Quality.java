package me.domirusz24.as.headhunter.headhunter;

import org.bukkit.ChatColor;

import java.util.HashMap;

public class Quality {

    public static HashMap<Integer, Quality> allQualities = new HashMap<>();
    private static int rngNumber = 0;

    public static HashMap<Integer, Quality> getAllQualities() {
        return allQualities;
    }


    public static int getRngNumber() {
        return rngNumber;
    }

    private String name;
    private int tier;
    private int chance;
    private String tierSign;

    public Quality(String name, int tier, String tierSign, int chance) {
        this.name = name;
        this.tier = tier;
        this.tierSign = tierSign;
        this.chance = rngNumber + chance;
        rngNumber = this.chance;
        allQualities.put(tier, this);
    }

    public String getQualityName() {
        return ChatColor.translateAlternateColorCodes('&', name);
    }

    public int getChance() {
        return chance;
    }

    public int getTier() {
        return tier;
    }

    public String getTierSign() {
        return tierSign;
    }
}
