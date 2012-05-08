package com.djupfryst.books.general;

import org.bukkit.ChatColor;

/**
 * @author iiMaXii
 */
public class MinecraftFont {
	public static int CHAT_WINDOW_WIDTH = 319;

	public MinecraftFont() {
		
	}
	
	public static int getWidth(String string) {
		boolean isSpecial = false;
		boolean isBold = false;
		int totalWidth = 0;
		for (char character : string.toCharArray()) {
			if (character == ChatColor.COLOR_CHAR) {
				isSpecial = true;
			} else if (isSpecial) {
				// Only the bold modifier changes the size of the text
				switch (character) {
				case 'l':
					isBold = true;
				case 'r':
					isBold = false;
				}
				isSpecial = false;
			} else {
				totalWidth += getWidth(character);
				
				if (isBold) {
					totalWidth += 1;
				}
			}
		}

		return totalWidth;
	}

	public static int getWidth(char character) {
		int length;
		switch (character) {
		case '!':
		case ',':
		case '.':
		case ':':
		case ';':
		case 'i':
		case '|':
		case '¡':
			length = 2;
			break;
		case '\'':
		case 'l':
		case 'ì':
		case 'í':
			length = 3;
			break;
		case ' ':
		case 'I':
		case 't':
		case '[':
		case ']':
		case '×':
		case 'ï':
			length = 4;
			break;
		case '"':
		case '(':
		case ')':
		case '*':
		case '<':
		case '>':
		case 'f':
		case 'k':
		case '{':
		case '}':
			length = 5;
			break;
		case '#':
		case '$':
		case '%':
		case '&':
		case '+':
		case '-':
		case '/':
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
		case '=':
		case '?':
		case 'A':
		case 'B':
		case 'C':
		case 'D':
		case 'E':
		case 'F':
		case 'G':
		case 'H':
		case 'J':
		case 'K':
		case 'L':
		case 'M':
		case 'N':
		case 'O':
		case 'P':
		case 'Q':
		case 'R':
		case 'S':
		case 'T':
		case 'U':
		case 'V':
		case 'W':
		case 'X':
		case 'Y':
		case 'Z':
		case '\\':
		case '^':
		case '_':
		case 'a':
		case 'b':
		case 'c':
		case 'd':
		case 'e':
		case 'g':
		case 'h':
		case 'j':
		case 'm':
		case 'n':
		case 'o':
		case 'p':
		case 'q':
		case 'r':
		case 's':
		case 'u':
		case 'v':
		case 'w':
		case 'x':
		case 'y':
		case 'z':
		case '£':
		case 'ª':
		case '«':
		case '¬':
		case 'º':
		case '»':
		case '¼':
		case '½':
		case '¿':
		case 'Ä':
		case 'Å':
		case 'Æ':
		case 'Ç':
		case 'É':
		case 'Ñ':
		case 'Ö':
		case 'Ø':
		case 'Ü':
		case 'à':
		case 'á':
		case 'â':
		case 'ä':
		case 'å':
		case 'æ':
		case 'ç':
		case 'è':
		case 'é':
		case 'ê':
		case 'ë':
		case 'î':
		case 'ñ':
		case 'ò':
		case 'ó':
		case 'ô':
		case 'ö':
		case 'ø':
		case 'ù':
		case 'ú':
		case 'û':
		case 'ü':
		case 'ÿ':
		case 'ƒ':
		case '⌂':
			length = 6;
			break;
		case '@':
		case '~':
		case '®':
			length = 7;
			break;
		default:
			Log.warning("Could not get width of character '" + character + "' (" + (int) character + ")");
			length = 0;
			break;
		}

		return length;
	}
}
