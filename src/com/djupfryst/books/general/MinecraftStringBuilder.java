package com.djupfryst.books.general;

import org.bukkit.ChatColor;

/**
 * @author iiMaXii
 */
public class MinecraftStringBuilder {
	private StringBuilder stringBuilder;
	private int pixelLength;
	private boolean isSpecial;
	private boolean isBold;

	private String formatting;

	public MinecraftStringBuilder() {
		stringBuilder = new StringBuilder();
		formatting = new String();
	}

	public MinecraftStringBuilder(String string) {
		this();
		append(string);
	}
	
	public MinecraftStringBuilder(MinecraftStringBuilder parent) {
		this();
		
		if (parent.formatting.contains(ChatColor.BOLD.toString())) {
			isBold = true;
		}
	}

	public void append(char character) {
		stringBuilder.append(character);

		if (character == ChatColor.COLOR_CHAR) {
			isSpecial = true;
		} else if (isSpecial) {
			// Is colour
			if (0x30 <= character && character <= 0x39 || 0x41 <= character && character <= 0x46 || 0x61 <= character && character <= 0x66) {
				formatting.replace(ChatColor.COLOR_CHAR + "[0-9A-Fa-f]", "");
				formatting = new StringBuilder(formatting).append(ChatColor.COLOR_CHAR).append(character).toString();
			} else {
				if (character == 'r' || character == 'R') {
					formatting.replaceAll(ChatColor.COLOR_CHAR + "[^0-9A-Fa-f]", "");
				} else {
					formatting = new StringBuilder(formatting).append(ChatColor.COLOR_CHAR).append(character).toString();
				}
			}

			// Only the bold modifier changes the size of the text
			switch (character) {
			case 'l':
			case 'L':
				isBold = true;
				break;
			case 'r':
			case 'R':
				isBold = false;
				break;
			}
			isSpecial = false;
		} else {
			pixelLength += MinecraftFont.getWidth(character);

			if (isBold) {
				pixelLength += 1;
			}
		}
	}

	public void append(String string) {
		for (char character : string.toCharArray()) {
			append(character);
		}
	}

	public String toString() {
		return stringBuilder.toString();
	}

	public int getPixelLength() {
		return pixelLength;
	}

	public int length() {
		return stringBuilder.length();
	}

	public char charAt(int i) {
		return stringBuilder.charAt(i);
	}

	public StringBuilder deleteCharAt(int i) {
		return stringBuilder.deleteCharAt(i);
	}

	/**
	 * Get the formatting and colour that is and will be applied to the appended
	 * characters
	 * 
	 * @return
	 */
	public String getCurrentFormatting() {
		return formatting;
	}
}
