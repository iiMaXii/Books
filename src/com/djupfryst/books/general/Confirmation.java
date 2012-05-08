package com.djupfryst.books.general;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.entity.Player;

/**
 * Used to keep track of confirmations for commands (e.g. /w --clear)
 * 
 * @author iiMaXii
 * 
 */
public class Confirmation {
	private HashMap<Player, Long> confirmations;
	private int timeout;

	/**
	 * 
	 * @param timeout
	 *            The amount of time a player is saved in seconds
	 */
	public Confirmation(int timeout) {
		this.timeout = timeout * 1000;

		confirmations = new HashMap<Player, Long>();
	}

	public void add(Player player) {
		confirmations.put(player, System.currentTimeMillis());
	}

	/**
	 * See if the player has previously been added
	 * 
	 * @param player
	 *            The player to check for
	 * @return True if the player has been added within the specified time,
	 *         otherwise false
	 */
	public boolean isConfirming(Player player) {
		clean();

		if (confirmations.containsKey(player)) {
			confirmations.remove(player);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Remove confirmations older than the timeout
	 */
	public void clean() {
		Iterator<Entry<Player, Long>> i = confirmations.entrySet().iterator();

		long now = System.currentTimeMillis();

		while (i.hasNext()) {
			if (now - i.next().getValue() > timeout) {
				i.remove();
			}
		}
	}
}
