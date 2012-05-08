package com.djupfryst.books.general;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Location;

/**
 * @author iiMaXii
 */
public class StringUtils {
	/**
	 * Merge the array using the specified delimiter.
	 * 
	 * @param strings
	 *            The array
	 * @param delimiter
	 *            The delimiter
	 * @return The merged string
	 */
	public final static String merge(String[] strings, String delimiter) {
		return merge(strings, delimiter, 0);
	}

	/**
	 * Merge the array using the specified delimiter.
	 * 
	 * @param strings
	 *            The array
	 * @param delimiter
	 *            The delimiter
	 * @param start
	 *            The position in the array where we should start merging
	 * @return The merged string
	 */
	public final static String merge(String[] strings, String delimiter, int start) {
		StringBuilder buffer = new StringBuilder();
		for (int i = start; i < strings.length; i++) {
			if (i != start)
				buffer.append(delimiter);
			buffer.append(strings[i]);
		}
		return buffer.toString();
	}

	/**
	 * Find if a list of strings contains the mentioned string
	 * 
	 * @param haystack
	 *            The list to search in
	 * @param needle
	 *            The string to look for
	 * @return True if the haystack has a value matching the needle
	 */
	public final static boolean containsIgnoreCase(Iterable<String> haystack, String needle) {
		for (String string : haystack) {
			if (needle.equalsIgnoreCase(string)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove the first occurrence of <code>needle</code> (ignoring case)
	 * 
	 * @param haystack
	 *            The iterable list or set to search in
	 * @param needle
	 *            The value to search for
	 * @return True if an element was removed otherwise false
	 */
	public final static boolean removeIgnoreCase(Iterable<String> haystack, String needle) {
		Iterator<String> iterator = haystack.iterator();

		while (iterator.hasNext()) {
			String string = iterator.next();
			if (needle.equalsIgnoreCase(string)) {
				iterator.remove();
				return true;
			}
		}
		return false;
	}

	/**
	 * Split all arguments into an array. Anything contained within quotes is
	 * considered as one argument, and you may find escaped quote within the
	 * arguments.
	 * 
	 * @param string
	 *            The string to split
	 * @return The arguments or null on syntax errors
	 */
	public final static String[] getArguments(String string) {
		List<String> arguments = new LinkedList<String>();
		boolean appended = false;
		boolean inQuotedString = false;
		StringBuilder currentArgument = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			char currentChar = string.charAt(i);

			if (appended) {
				if (currentChar == '\\' || currentChar == '"') {
					currentArgument.append(currentChar);
				} else {
					currentArgument.append('\\').append(currentChar);
				}
				appended = false;
			} else if (currentChar == '\\') {
				appended = true;
			} else if (currentChar == '"') {
				if (currentArgument.length() != 0) {
					arguments.add(currentArgument.toString());
					currentArgument = new StringBuilder();
				}
				inQuotedString = !inQuotedString;
			} else if (inQuotedString) {
				currentArgument.append(currentChar);
			} else if (currentChar == ' ') {
				if (currentArgument.length() != 0) {
					arguments.add(currentArgument.toString());
					currentArgument = new StringBuilder();
				}
			} else {
				currentArgument.append(currentChar);
			}
		}
		// Syntax error
		if (inQuotedString)
			return null;

		if (appended)
			currentArgument.append('\\');

		if (currentArgument.length() != 0)
			arguments.add(currentArgument.toString());

		String[] args = new String[arguments.size()];
		arguments.toArray(args);
		return args;
	}

	/**
	 * Get a readable representation of the location
	 * 
	 * @param location
	 *            The location
	 * @return A string formatted "(world, x, y, z)"
	 */
	public final static String toString(Location location) {
		return "(" + location.getWorld().getName() + ", " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
	}

	public static boolean contains(char[] array, char character) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == character) {
				return true;
			}
		}
		return false;
	}
}
