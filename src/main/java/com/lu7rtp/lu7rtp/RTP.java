package com.lu7rtp.lu7rtp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

public class RTP extends JavaPlugin {

	private Map<String, Long> cooldowns = new HashMap<>();
	private FileConfiguration config;
	private BossBar godModeBossBar;

	// Helper method to load the config
	private void loadConfig() {
		saveDefaultConfig();
		config = getConfig();
	}

	@Override
	public void onEnable() {
		loadConfig(); // Load the config

		// Log successful enable
		getLogger().log(Level.INFO, "LU7 RTP plugin has been enabled!");

		// Initialize godModeBossBar
		godModeBossBar = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID);
		godModeBossBar.setVisible(false);

		// Start bStats
		if (config.getBoolean("enablebStats", true)) {
			int pluginId = 20666;
			Metrics metrics = new Metrics(this, pluginId);
			getLogger().log(Level.INFO,
					"bStats metrics has been enabled. To opt-out, change 'enablebStats' to false in config.yml.");
		}
	}

	@Override
	public void onDisable() {
		getLogger().info("LU7 RTP Plugin has been disabled!");
		removeAllBossBars();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can use this command!");
			return true;
		}

		Player player = (Player) sender;

		if (label.equalsIgnoreCase("rtp")) {
			if (cooldowns.containsKey(player.getName())) {
				long secondsLeft = (cooldowns.get(player.getName()) / 1000) + 65 - (System.currentTimeMillis() / 1000);
				if (secondsLeft > 0) {
					player.sendMessage(colorize("&9&l[&6&lL&a&lU&e&l7&c&l RTP&9&l] &cYou must wait " + secondsLeft
							+ " seconds before using RTP again."));
					return true;
				}
			}

			// Set a new cooldown
			cooldowns.put(player.getName(), System.currentTimeMillis());

			teleportRandomly(player);
			player.sendMessage(colorize("&9&l[&6&lL&a&lU&e&l7&c&l RTP&9&l] &aTeleported to a random location!"));

			return true;
		}

		return false;
	}

	private boolean isSafeLocation(Location location) {
		// Check if the location is in water or lava
		if (location.getBlock().isLiquid()) {
			return false;
		}

		// You can add more safety checks if needed

		return true;
	}

	private void teleportRandomly(final Player player) {
	    Random random = new Random();
	    int maxAttempts = 10; // Number of attempts to find a safe location
	    int godModeDuration = 60; // Duration of god mode in seconds

	    // Display the searching message
	    player.sendTitle(colorize("&aSearching for safe location..."), colorize("&fPlease wait"), 10, 40, 10);

	    for (int i = 0; i < maxAttempts; i++) {
	        double x = random.nextInt(40000) - 20000;
	        double z = random.nextInt(40000) - 20000;

	        Location randomLocation = new Location(Bukkit.getWorlds().get(0), x, 0, z);

	        // Load the chunks at the random location
	        randomLocation.getChunk().load();

	        // Check if the location is safe (e.g., not in water or lava)
	        if (isSafeLocation(randomLocation)) {
	            // Find a safe location above the water surface
	            Location aboveWater = findSafeLocationAboveWater(randomLocation);

	            if (aboveWater != null) {
	                // Ensure the player is not spawning underwater
	                if (isUnderwater(aboveWater)) {
	                    continue; // Skip this iteration and try another location
	                }

	                // Temporarily give the player god mode
	                player.setInvulnerable(true);

	                // Display the "Teleporting..." message with a delay
	                Bukkit.getScheduler().runTaskLater(this, () ->
	                        player.sendTitle(colorize("&aTeleporting..."), "", 10, 40, 10), 20); // Delay by 20 ticks (1 second)

	                // Create a boss bar for god mode countdown
	                createGodModeBossBar(player, godModeDuration);

	                // Teleport the player to the safe location
	                player.teleport(aboveWater);

	                // Schedule a task to remove god mode after a few seconds
	                Bukkit.getScheduler().runTaskLater(this, () -> {
	                    if (player.isOnline()) {
	                        player.sendMessage(colorize(
	                                "&9&l[&6&lL&a&lU&e&l7&c&l RTP&9&l] &cYour teleport protection has expired!"));
	                        player.setInvulnerable(false);

	                        // Remove the boss bar when god mode expires
	                        godModeBossBar.removeAll();
	                    }
	                }, godModeDuration * 20); // Convert seconds to ticks

	                return; // Ensure the method exits after teleporting the player
	            }
	        }
	    }

	    // If no safe location is found after the maximum attempts, inform the player
	    player.sendMessage(
	            colorize("&9&l[&6&lL&a&lU&e&l7&c&l RTP&9&l] &cUnable to find a safe location. Please try again."));
	}

	private boolean isUnderwater(Location location) {
	    // Check if the location is underwater
	    return location.getBlock().isLiquid();
	}


	private Location findSafeLocationAboveWater(Location location) {
		// Find a safe location above the water surface
		int maxAttempts = 10;
		for (int i = 0; i < maxAttempts; i++) {
			location.setY(location.getWorld().getHighestBlockYAt(location));
			if (isSafeLocation(location)) {
				return location;
			}
			location.add(0, 1, 0);
		}
		return null;
	}

	private void createGodModeBossBar(Player player, int duration) {
		// Remove any existing boss bars
		removeAllBossBars();

		// Create a new boss bar
		godModeBossBar = Bukkit.createBossBar(colorize("&6Teleport Protection: &c" + duration + "s &6remaining"),
				BarColor.YELLOW, BarStyle.SOLID);
		godModeBossBar.addPlayer(player);
		godModeBossBar.setVisible(true);

		// Update the boss bar every second
		BukkitTask task = new BukkitRunnable() {
			int remainingTime = duration;

			@Override
			public void run() {
				remainingTime--;

				if (!player.isOnline()) {
					// Player logged out, cancel the task
					this.cancel();
					removeAllBossBars();
					return;
				}

				if (remainingTime <= 0) {
					// Remove the boss bar when the duration is over
					removeAllBossBars();
					this.cancel();
				} else {
					// Update the boss bar title
					godModeBossBar.setTitle(colorize("&6Teleport Protection: &c" + remainingTime + "s &6remaining!"));
				}
			}
		}.runTaskTimer(this, 20L, 20L);
	}

	private void removeAllBossBars() {
		// Remove godModeBossBar if it's not null
		if (godModeBossBar != null) {
			godModeBossBar.removeAll();
		}
	}

	private String colorize(String message) {
		// Use Minecraft colour codes to add colour to the message
		return message.replace("&", "\u00A7");
	}
}
