package com.djupfryst.books.library.actions;

/**
 * Removal of a number of characters from the end
 * 
 * @author iiMaXii
 * 
 */
public class RemoveAction implements Action {
	private String removedString;

	public RemoveAction(String removedString) {
		this.removedString = removedString;
	}

	public String getRemovedString() {
		return removedString;
	}
}
