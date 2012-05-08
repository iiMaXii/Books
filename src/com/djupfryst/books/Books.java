package com.djupfryst.books;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import com.djupfryst.books.general.Log;
import com.djupfryst.books.library.Library;
import com.djupfryst.books.library.actions.ActionList;

/**
 * @author iiMaXii
 */
public class Books extends JavaPlugin {
	public final static ChatColor COLOR = ChatColor.DARK_GREEN;

	private Library library;

	public void onEnable() {
		if (getServer().getPluginManager().getPlugin("BookWorm") != null) {
			Log.severe("The plugin BookWorm was detected, to prevent any damage Books will now be disabled");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		// Load configuration
		Config config = new Config(this);
		ActionList.setLimit(config.getUndoLimit());

		// Load library
		library = new Library(getDataFolder());
		library.load();

		// Register commands
		BooksCommand commandExecutor = new BooksCommand(library);
		getCommand("w").setExecutor(commandExecutor);
		getCommand("bookstest").setExecutor(new TestCommand());

		// Register tasks
		getServer().getPluginManager().registerEvents(new BooksListener(library), this);
		getServer().getPluginManager().registerEvents(library, this);
		// Copy recipe
		getServer().getPluginManager().registerEvents(new CraftCopyListener(this, config), this);

		// Cache cleaner
		getServer().getScheduler().scheduleSyncRepeatingTask(this, library, 20 * 60 * 20, 20 * 60 * 20);
	}

	public void onDisable() {
		if (library != null)
			library.save();
	}

}
