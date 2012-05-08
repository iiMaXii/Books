package com.djupfryst.books.library.actions;

import java.util.LinkedList;

/**
 * @author iiMaXii
 */
public class ActionList {
	private static int LIMIT;
	private LinkedList<Action> actions;

	/**
	 * Set the limit for the
	 * 
	 * @param limit
	 */
	public static void setLimit(int limit) {
		LIMIT = limit;
	}

	/**
	 * Create a new action list
	 */
	public ActionList() {
		actions = new LinkedList<Action>();
	}

	/**
	 * Add a new action to the list
	 * 
	 * @param action
	 *            The action you want to add
	 */
	public void add(Action action) {
		if (LIMIT <= 0) {
			return;
		}

		if (actions.size() == LIMIT) {
			actions.remove();
		}

		actions.add(action);
	}

	/**
	 * Take out the most recently added action, i.e. delete it from the list and
	 * return and pass it on
	 * 
	 * @return The action that was added most recently
	 */
	public Action get() {
		if (actions.size() == 0) {
			return null;
		} else {
			return actions.removeLast();
		}
	}

	/**
	 * Remove all the actions from the list
	 */
	public void clear() {
		actions.clear();
	}

	public boolean isEmpty() {
		return actions.isEmpty();
	}
}
