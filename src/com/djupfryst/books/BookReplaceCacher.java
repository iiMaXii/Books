package com.djupfryst.books;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.entity.Player;

import com.djupfryst.books.library.Book;

/**
 * Represents a match
 * 
 * @author iiMaXii
 * 
 */
class ReplaceMatch {
	String before, after;
	boolean beforeAtEnd, afterAtEnd;
	int offset;

	ReplaceMatch(int offset, String before, String after, boolean beforeAtEnd, boolean afterAtEnd) {
		this.offset = offset;
		this.before = before;
		this.after = after;
		this.beforeAtEnd = beforeAtEnd;
		this.afterAtEnd = afterAtEnd;
	}
}

/**
 * Represents a search
 * 
 * @author iiMaXii
 * 
 */
class ReplaceCache {
	long timestamp;
	short bookUid;
	ArrayList<ReplaceMatch> matches;
	String search, replace;

	public ReplaceCache(short bookUid, String search, String replace, ArrayList<ReplaceMatch> matches) {
		timestamp = System.currentTimeMillis();
		this.bookUid = bookUid;
		this.matches = matches;
		this.search = search;
		this.replace = replace;
	}
}

/**
 * Stores all replacement searches
 * 
 * @author iiMaXii
 * 
 */
class BookReplaceCacher {
	private static HashMap<Player, ReplaceCache> replaceList = new HashMap<Player, ReplaceCache>();
	private static int timeout = 60 * 1000;

	public static void put(Player player, ReplaceCache replaceCache) {
		replaceList.put(player, replaceCache);
	}

	public static ReplaceCache get(Player player) {
		ReplaceCache replaceCache = replaceList.get(player);

		if (replaceCache != null)
			replaceCache.timestamp = System.currentTimeMillis();

		return replaceCache;
	}

	public static void clean() {
		long now = System.currentTimeMillis();

		Iterator<Entry<Player, ReplaceCache>> iterator = replaceList.entrySet().iterator();
		while (iterator.hasNext()) {
			if (now - iterator.next().getValue().timestamp > timeout) {
				iterator.remove();
			}
		}
	}

	public static void remove(Player player) {
		replaceList.remove(player);
	}
	
	public static void remove(Book book) {
		Iterator<Entry<Player,ReplaceCache>> iterator = replaceList.entrySet().iterator();
		short uid = book.getUID();
		while (iterator.hasNext()) {
			if (iterator.next().getValue().bookUid == uid) {
				iterator.remove();
			}
		}
	}
}
