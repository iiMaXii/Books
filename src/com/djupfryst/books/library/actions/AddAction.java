package com.djupfryst.books.library.actions;

/**
 * Adding a number of characters to the end
 * 
 * @author iiMaXii
 * 
 */
public class AddAction implements Action {
	int numberOfCharactersAdded;

	public AddAction(int numberOfCharactersAdded) {
		this.numberOfCharactersAdded = numberOfCharactersAdded;
	}

	public int getNumberOfCharactersAdded() {
		return numberOfCharactersAdded;
	}
}
