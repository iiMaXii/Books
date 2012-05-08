package com.djupfryst.books.general;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.djupfryst.books.general.StringUtils;

/**
 * Write to the log in a convenient way.
 * 
 * @author iiMaXii
 * 
 */
public class Log {
	private final static Logger LOGGER = Logger.getLogger("Minecraft");

	public static Logger getLogger() {
		return LOGGER;
	}

	public static void dump(File file, String string) {
		info("Dumping " + file.toString());
		LOGGER.info("================================");
		LOGGER.info(string);
		LOGGER.info("================================");
	}

	public static void info(String message) {
		LOGGER.info("[Books] " + message);
	}

	public static void warning(String message) {
		LOGGER.warning("[Books] " + message);
	}

	public static void severe(String message) {
		LOGGER.severe("[Books] " + message);
	}

	/**
	 * Log a command
	 */
	public static void command(CommandSender player, Command command, String[] args) {
		String argMerged = StringUtils.merge(args, " ");

		info(player.getName() + " executed /" + command.getName() + " " + argMerged);
	}

	public static void commandDenied(CommandSender player, Command command, String[] args) {
		String argMerged = StringUtils.merge(args, " ");

		info(player.getName() + " was denied to execute /" + command.getName() + " " + argMerged);
	}
}
