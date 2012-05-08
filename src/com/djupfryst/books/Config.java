package com.djupfryst.books;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;

import com.djupfryst.books.general.Log;

/**
 * @author iiMaXii
 */
public class Config {
	private int undoLimit;
	private boolean craftCopy;

	public Config(Books books) {
		if (!new File(books.getDataFolder(), "config.yml").exists()) {
			Log.info("Creating new configuration file");
			books.saveResource("config.yml", false);
		}

		FileConfiguration config = books.getConfig();

		// Add defaults
		config.addDefault("undoLimit", 20);
		config.addDefault("craftCopy", true);

		// Load configuration to memory
		undoLimit = config.getInt("undoLimit");
		if (undoLimit < 0) {
			Log.warning("Configuration error, undoLimit must be a positive integer or zero. Assuming \"0\".");
			undoLimit = 0;
		}

		craftCopy = config.getBoolean("craftCopy");
	}

	/**
	 * Get the undo limit
	 * 
	 * @return Positive integer or zero
	 */
	public int getUndoLimit() {
		return undoLimit;
	}

	/**
	 * Is crafting copies of books enables
	 * 
	 * @return true if enabled, otherwise false
	 */
	public boolean craftCopy() {
		return craftCopy;
	}
}
