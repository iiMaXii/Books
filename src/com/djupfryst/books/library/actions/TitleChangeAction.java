package com.djupfryst.books.library.actions;

/**
 * @author iiMaXii
 */
public class TitleChangeAction implements Action {
	private String previousTitle;

	public TitleChangeAction(String previousTitle) {
		this.previousTitle = previousTitle;
	}

	public String getPreviousTitle() {
		return previousTitle;
	}
}
