package com.djupfryst.books.library.actions;

/**
 * Replacing a string with another string
 * 
 * @author iiMaXii
 * 
 */
public class ReplaceAction implements Action {
	private int start, end;
	private String replacedString;

	/**
	 * 
	 * @param start
	 *            The start of the string that replaced the replacedString
	 * @param end
	 *            The end of the string that replaced the replacedString
	 * @param replacedString
	 *            The string that got replaced
	 */
	public ReplaceAction(int start, int end, String replacedString) {
		this.start = start;
		this.end = end;
		this.replacedString = replacedString;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public String getReplacedString() {
		return replacedString;
	}
}
