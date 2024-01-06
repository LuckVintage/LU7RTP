package com.lu7rtp.lu7rtp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

public class RTP extends JavaPlugin {

	private Map<String, Long> cooldowns = new HashMap<>();
	private FileConfiguration config;

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
				long secondsLeft = (cooldowns.get(player.getName()) / 1000) + 60 - (System.currentTimeMillis() / 1000);
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
		int godModeDuration = 30; // Duration of god mode in seconds

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
					// Temporarily give the player god mode
					player.setInvulnerable(true);

					// Teleport the player to the safe location
					player.teleport(aboveWater);

					// Schedule a task to remove god mode after a few seconds
					Bukkit.getScheduler().runTaskLater(this, () -> {
						if (player.isOnline()) {
							player.sendMessage(colorize(
									"&9&l[&6&lL&a&lU&e&l7&c&l RTP&9&l] &cYour teleport protection has expired!"));
							player.setInvulnerable(false);
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

	private String colorize(String message) {
		// Use Minecraft colour codes to add colour to the message
		return message.replace("&", "\u00A7");
	}
}