package com.djupfryst.books.general;

/**
 * Calculates the interval for pages of a document that consist of multiple
 * lines and that may be so long it doesn't fit in the chat buffer
 * 
 * @author iiMaXii
 * 
 */
public class Doc {
	private final static int DEFAULT_LINES_PER_PAGE = 9;

	public static int parsePage(String number) {
		int page;
		try {
			page = Integer.parseInt(number);
		} catch (NumberFormatException e) {
			page = 1;
		}

		return page;
	}

	/**
	 * Calculate the number of pages the entities will give
	 * 
	 * @param entities
	 *            The total number of rows
	 * @param pageSize
	 *            How many rows a page consists of
	 * @return The total number of pages
	 */
	public static int calculateNumberOfPages(int entities, int pageSize) {
		return entities / pageSize + ((entities % pageSize == 0) ? 0 : 1);
	}

	/**
	 * Calculates the interval for the specified page
	 * 
	 * @param entities
	 *            The total number of rows
	 * @param page
	 *            The page number
	 * @param pageSize
	 *            How many rows a page consists of
	 * @return An array containing start and finish (non inclusive), or may be
	 *         null if the page doesn't exist
	 */
	public static int[] calculateInterval(int entities, int page, int pageSize) {
		if (page < 1)
			return null;

		int start = (page - 1) * pageSize;
		if (start > entities)
			return null;

		int end = start + pageSize;

		if (end > entities) {
			end = entities;
		}

		return new int[] { start, end };
	}

	/**
	 * Calculate the number of pages the entities will give, assuming that the
	 * number of rows for one page is 9.
	 * 
	 * @param entities
	 *            The total number of rows
	 * @return The total number of pages
	 */
	public static int calculateNumberOfPages(int entities) {
		return calculateNumberOfPages(entities, DEFAULT_LINES_PER_PAGE);
	}

	/**
	 * Calculates the interval for the specified page, assuming that the number
	 * of rows for one page is 9.
	 * 
	 * @param entities
	 *            The total number of rows
	 * @param page
	 *            The page number
	 * @return An array containing start and finish, may be null if the first
	 *         integer is outside the boundaries
	 */
	public static int[] calculateInterval(int entities, int page) {
		return calculateInterval(entities, page, DEFAULT_LINES_PER_PAGE);
	}

}