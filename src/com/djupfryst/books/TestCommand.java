package com.djupfryst.books;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.djupfryst.books.general.Doc;
import com.djupfryst.books.general.Log;
import com.djupfryst.books.general.MinecraftFont;
import com.djupfryst.books.general.StringUtils;

/**
 * @author iiMaXii
 */
public class TestCommand implements CommandExecutor {

	private int[] getInterval(int[] interval, int num) {
		int entries = (interval[1] - interval[0]) / 10;

		if ((interval[1] - interval[0]) % 10 != 0) {
			entries += 1;
		}

		return new int[] { interval[0] + entries * num, interval[0] + entries * (num + 1) };
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !((Player) sender).hasPermission("books.test")) {
			sender.sendMessage(ChatColor.RED + "I'm sorry, I can't let you do that.");
			return true;
		}

		char[] knownCharacters = { '!', ',', '.', ':', ';', 'i', '|', '¡', '\'', 'l', 'ì', 'í', ' ', 'I', 't', '[', ']', '×', 'ï', '"', '(', ')', '*', '<', '>', 'f', 'k', '{', '}', '#', '$', '%',
				'&', '+', '-', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '=', '?', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
				'V', 'W', 'X', 'Y', 'Z', '\\', '^', '_', 'a', 'b', 'c', 'd', 'e', 'g', 'h', 'j', 'm', 'n', 'o', 'p', 'q', 'r', 's', 'u', 'v', 'w', 'x', 'y', 'z', '£', 'ª', '«', '¬', 'º', '»', '¼',
				'½', '¿', 'Ä', 'Å', 'Æ', 'Ç', 'É', 'Ñ', 'Ö', 'Ø', 'Ü', 'à', 'á', 'â', 'ä', 'å', 'æ', 'ç', 'è', 'é', 'ê', 'ë', 'î', 'ñ', 'ò', 'ó', 'ô', 'ö', 'ø', 'ù', 'ú', 'û', 'ü', 'ÿ', 'ƒ', '@',
				'~', '®', '⌂' };

		if (args.length == 0) {
			return false;
		} else if (args[0].equalsIgnoreCase("print")) {
			sender.sendMessage(knownCharacters.length + " known characters.");

			StringBuilder b = new StringBuilder();
			int currentLineLength = 0;
			for (int i = 0; i < knownCharacters.length; i++) {
				int currentCharacterLength = MinecraftFont.getWidth(knownCharacters[i]);

				if (currentCharacterLength == 0) {
					sender.sendMessage(knownCharacters[i] + ".length == 0");
				}

				if (currentLineLength + currentCharacterLength <= MinecraftFont.CHAT_WINDOW_WIDTH) {
					b.append(knownCharacters[i]);
					currentLineLength += currentCharacterLength;
				} else {
					sender.sendMessage("The following line is " + currentLineLength + " pixels wide:");
					sender.sendMessage(b.toString());

					b = new StringBuilder().append(knownCharacters[i]);
					currentLineLength = currentCharacterLength;
				}
			}
			sender.sendMessage("The following line is " + currentLineLength + " pixels wide:");
			sender.sendMessage(b.toString());
		} else if (args[0].equalsIgnoreCase("find")) {

			int[] interval = { 0, 0xFFFF };
			for (int i = 1; i < args.length; i++) {
				int num;
				try {
					num = Integer.parseInt(args[i]);
				} catch (NumberFormatException e) {
					sender.sendMessage("error");
					return true;
				}
				interval = getInterval(interval, num);
			}

			Log.info("[" + interval[0] + "," + interval[1] + ")");

			for (int i = 0; i < 10; i++) {
				int[] currentInterval = getInterval(interval, i);
				StringBuilder b = new StringBuilder();
				for (int j = currentInterval[0]; j < currentInterval[1]; j++) {
					char currentChar = (char) j;
					if (!StringUtils.contains(knownCharacters, currentChar)) {
						b.append(currentChar);
					}
				}

				sender.sendMessage("#" + i + ":\"" + b.toString() + "\"" + " [" + currentInterval[0] + ", " + currentInterval[1] + ")");
			}
			/*
			 * StringBuilder b = new StringBuilder(); for (int i = 0; i <
			 * 0xFFFFFF; i++) { char currentChar = (char) i; if
			 * (!StringUtils.contains(knownCharacters, currentChar)) {
			 * b.append(currentChar); } }
			 * 
			 * sender.sendMessage('"' + b.toString() + '"');
			 */
		} else if (args[0].matches("^\\d+$")) {
			int page = Integer.parseInt(args[0]);
			int[] interval = Doc.calculateInterval(knownCharacters.length, page);

			if (interval == null) {
				sender.sendMessage("Known characters (Page " + page + " of " + Doc.calculateNumberOfPages(knownCharacters.length) + ")");
			} else {
				sender.sendMessage("Known characters (Page " + page + " of " + Doc.calculateNumberOfPages(knownCharacters.length) + ")");
				for (int i = interval[0]; i < interval[1]; i++) {
					sender.sendMessage((int) knownCharacters[i] + ": \"" + knownCharacters[i] + "\"" + " (" + MinecraftFont.getWidth(knownCharacters[i]) + ")");
				}
			}
		} else if (args[0].equalsIgnoreCase("bold")) {
			sender.sendMessage("asdf" + "|");
			sender.sendMessage(ChatColor.ITALIC + "asdf" + ChatColor.RESET + "|");
			sender.sendMessage(ChatColor.STRIKETHROUGH + "asdf" + ChatColor.RESET + "|");
			sender.sendMessage(ChatColor.UNDERLINE + "asdf" + ChatColor.RESET + "|");
			sender.sendMessage(ChatColor.BOLD + "asdf" + ChatColor.RESET + "|");
		} else if (args[0].equalsIgnoreCase("codes")) {
			for (ChatColor chatColor : ChatColor.values()) {
				sender.sendMessage(String.valueOf(chatColor).replace(ChatColor.COLOR_CHAR, '\0') + ": " + chatColor + "asdf");
			}
		} else {
			return false;
		}

		return true;
	}
}
