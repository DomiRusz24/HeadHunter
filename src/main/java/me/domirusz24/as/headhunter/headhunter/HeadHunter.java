package me.domirusz24.as.headhunter.headhunter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class HeadHunter extends JavaPlugin implements Listener {

    private double chance;
    private List<String> messages;
    public static HeadHunter plugin;


    @Override
    public void onEnable() {
        plugin = this;
        System.out.println("HeadHunter has been enabled!");
        Bukkit.getPluginManager().registerEvents(this, this);
        reloadConfig();
        getConfig().options().copyDefaults(true);
        getConfig().addDefault("SQL.host", "localhost");
        getConfig().addDefault("SQL.port", 3306);
        getConfig().addDefault("SQL.database", "HeadHunter");
        getConfig().addDefault("SQL.username", "user");
        getConfig().addDefault("SQL.password", "pass");
        getConfig().addDefault("Chance", 100);
        getConfig().addDefault("Messages", Arrays.asList("&6&lDostales glowe gracza %target%", "&6&lZdobyles glowe gracza %target%", "&6&lUciales glowe graczowi %target%", "&6&lK.O. %target%"));
        getConfig().addDefault("Name", "&6&lGlowa gracza &b&l%target% &d&l(%tiersign%) &8#%id%");
        getConfig().addDefault("Description", "&cGlowa zdobyta przez gracza &e%player%//&6Stan %quality%//&aGenerated: &8#%id%");
        registerQuality(new Quality("&2&l>> Idealny <<", 1, "I", 5));
        registerQuality(new Quality("&a&l>> Dobry <<", 2, "II" ,  10));
        registerQuality(new Quality("&7&l>> Lekko Uszkodzony <<", 3, "III" ,  20));
        registerQuality(new Quality("&9&l>> Ubogi <<", 4, "IV" ,  25));
        registerQuality(new Quality("&4&l>> Uszkodzony <<", 5, "V" ,  40));
        saveConfig();
        saveDefaultConfig();
        chance = getConfig().getDouble("Chance", 100) * 100;
        PlayerHead.description = new ArrayList<>(Arrays.asList(getConfig().getString("Description").split("//")));
        PlayerHead.name = getConfig().getString("Name");
        messages = getConfig().getStringList("Messages");
        for (String str : getConfig().getConfigurationSection("Rarities").getKeys(false)) {
            String StringTier = str.substring(4);
            int tier;
            try {
                tier = Integer.parseInt(StringTier);
            } catch (NumberFormatException e) {
                System.out.println("Error: Couldn't create rarity: " + str);
                continue;
            }
            System.out.println("Registered tier " + tier + "!");
            createQuality(tier);
        }
        try {
            openConnection();
            execute("CREATE TABLE IF NOT EXISTS HeadID (" +
                    "player_name VARCHAR(20)," +
                    "latest_id MEDIUMINT(1000000)," +
                    "PRIMARY KEY (player_name)" +
                    ");");
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Nie połączono z SQL!");
            e.printStackTrace();
        }
        (new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.createStatement().execute("SELECT 1");
                    }
                } catch (SQLException e) {
                    try {
                        openConnection();
                    } catch (SQLException | ClassNotFoundException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).runTaskTimerAsynchronously(this, 60 * 20, 60 * 20);
    }

    public void registerQuality(Quality quality) {
        String path = "Rarities.Tier" + quality.getTier() + ".";
        getConfig().addDefault(path + "Name", quality.getQualityName());
        getConfig().addDefault(path + "TierSign", quality.getTier());
        getConfig().addDefault(path + "Chance", quality.getChance());
    }

    public Quality createQuality(int tier) {
        if (getConfig().contains( "Rarities.Tier" + tier)) {
            String path = "Rarities.Tier" + tier + ".";
            String name = getConfig().getString(path + "Name");
            String tierSign = getConfig().getString(path + "TierSign");
            int chance = getConfig().getInt(path + "Chance");
            return new Quality(name, tier, tierSign, chance);
        } else {
            return null;
        }
    }

    private Connection connection;

    private String host, database, username, password;
    private int port;

    public void openConnection() throws SQLException, ClassNotFoundException {
        host = getConfig().getString("SQL.host", "localhost");
        port = getConfig().getInt("SQL.port", 3306);
        database = getConfig().getString("SQL.database", "HeadHunter");
        username = getConfig().getString("SQL.username", "user");
        password = getConfig().getString("SQL.password", "pass");

        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + this.host+ ":" + this.port + "/" + this.database, this.username, this.password);
        }
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    public boolean execute(String sql) throws SQLException {
        return connection.createStatement().execute(sql);
    }

    public int getLatestID(String player) {
        int id = 1;
        try {
            ResultSet result = connection.createStatement().executeQuery("SELECT * FROM HeadID WHERE player_name = " + player + ";");
            if (!result.next()) {
                connection.createStatement().executeUpdate("INSERT INTO HeadID (player_name, latest_id) VALUES ('" + player +"',1);");
            } else {
                id = result.getInt("latest_id") + 1;
                connection.createStatement().executeUpdate("INSERT INTO HeadID (player_name, latest_id) VALUES ('" + player +"'," + id + ");");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
        return id;
    }

    @Override
    public void onDisable() {
        System.out.println("HeadHunter has been disabled!");
    }


    @EventHandler
    public void onKill(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType().equals(EntityType.PLAYER) && event.getEntity().getType().equals(EntityType.PLAYER)) {
            Player target = (Player) event.getEntity();
            if (target.getHealth() - event.getFinalDamage() <= 0) {
                Player player = (Player) event.getDamager();
                if (getChance(chance)) {
                    giveHeadTo(target, player);
                }
            }
        }
    }


    public void giveHeadTo(Player target, Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getMessages().replace("%player%", player.getName()).replace("%target%", target.getName())));
        new PlayerHead(target, player);
    }

    private boolean getChance(double chance) {
        return random.nextInt(10000) <= chance;
    }

    Random random = new Random();

    private String getMessages() {
        int i = random.nextInt(messages.size());
        return messages.get(i);
    }
}
