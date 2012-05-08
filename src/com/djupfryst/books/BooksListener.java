package com.djupfryst.books;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import com.djupfryst.books.general.InventoryManipulator;
import com.djupfryst.books.general.Log;
import com.djupfryst.books.library.Book;
import com.djupfryst.books.library.BookLoadException;
import com.djupfryst.books.library.Library;

/**
 * @author iiMaXii
 */
class PageCacher {
	Book book;
	long timestamp = System.currentTimeMillis();
	int page = 1;

	PageCacher(Book book) {
		this.book = book;
	}
}

/**
 * @author iiMaXii
 */
public class BooksListener implements Listener {
	private final static int PAGE_CHACHE_TIME = 40 * 1000;

	private Library library;
	private HashMap<Player, PageCacher> lastPage;

	public BooksListener(Library library) {
		this.library = library;
		lastPage = new HashMap<Player, PageCacher>();
	}

	/**
	 * Displays a book for a player
	 * 
	 * @param player
	 *            The player that want to view the book
	 * @param book
	 *            The book that will be shown to the player
	 */
	private void displayBook(Player player, Book book) {
		// Find and remove old page caches
		long now = System.currentTimeMillis();
		Iterator<Entry<Player, PageCacher>> iterator = lastPage.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<Player, PageCacher> next = iterator.next();
			if (now - next.getValue().timestamp > PAGE_CHACHE_TIME) {
				iterator.remove();
			}
		}

		// Remove players page cache if the player is viewing another book than
		// the one cached
		PageCacher pageCache = lastPage.get(player);
		if (pageCache != null && pageCache.book != book) {
			lastPage.remove(player);
			pageCache = null;
		}

		try {
			int pages = book.getPages();

			String additionalAuthors = (book.getAuthors().isEmpty()) ? "" : ChatColor.RED + "+" + ChatColor.WHITE;
			if (pages == 0) {
				player.sendMessage(book.getTitle() + Books.COLOR + " by " + ChatColor.WHITE + book.getCreator() + additionalAuthors + " (empty)");
			} else {
				if (pageCache == null) {
					pageCache = new PageCacher(book);
					lastPage.put(player, pageCache);
				} else if (pageCache.page > pages) {
					// May happen if book is edited
					pageCache.page = 1;
				}

				player.sendMessage(book.getTitle() + Books.COLOR + " by " + ChatColor.WHITE + book.getCreator() + additionalAuthors + Books.COLOR + " (Page " + pageCache.page + " of " + pages + ")");
				player.sendMessage(Books.COLOR + "-----------------------------------------------------");
				String[] lines = book.getPage(pageCache.page);
				for (int i = 0; i < lines.length; i++) {
					player.sendMessage(lines[i]);
				}
				player.sendMessage(Books.COLOR + "-----------------------------------------------------");

				pageCache.page = pageCache.page % (pages + 1) + 1;
				pageCache.timestamp = now;
			}
		} catch (BookLoadException e) {
			player.sendMessage(ChatColor.RED + "An internal error occured, please contact an administrator.");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.BOOKSHELF) {
			Player player = event.getPlayer();
			Block block = event.getBlock();

			block.setType(Material.AIR);

			Book bookInShelf = library.getBook(block);
			if (bookInShelf != null && player.getGameMode() != GameMode.CREATIVE) {
				block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.BOOK, 1, bookInShelf.getUID()));
			}
			if (player.getGameMode() != GameMode.CREATIVE)
				block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.BOOKSHELF, 1));

			library.removeBook(block);

			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.BOOKSHELF
				&& (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
			Player player = event.getPlayer();
			short uid = 0;
			if (event.getItem() != null && event.getItem().getType() == Material.BOOK) {
				uid = event.getItem().getDurability();
			}

			Book bookInShelf = library.getBook(event.getClickedBlock());
			Book bookInHand = (uid == 0) ? null : library.getBook(uid);

			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (bookInShelf != null) {
					displayBook(player, bookInShelf);
				}
			} else if (bookInShelf != null && bookInHand != null && player.hasPermission("books.putInShelf")) {
				player.sendMessage(ChatColor.RED + "Another book is already found in this bookshelf.");
			} else if (bookInShelf != null) {
				if (event.getItem() != null && event.getItem().getType() == Material.BOOK && player.hasPermission("books.copyFromShelf")) {
					if (player.getItemInHand().getAmount() == 1) {
						player.setItemInHand(new ItemStack(Material.BOOK, 1, bookInShelf.getUID()));
						player.sendMessage(ChatColor.GRAY + "The book has been copied from the bookshelf.");
					} else {
						player.sendMessage(ChatColor.RED + "You need to be holding a single book to copy the contents.");
					}
				} else {
					displayBook(player, bookInShelf);
				}
				event.setCancelled(true);
			} else if (bookInHand != null) {
				if (player.hasPermission("books.putInShelf")) {
					library.addBook(bookInHand, event.getClickedBlock());

					if (player.getGameMode() != GameMode.CREATIVE) {
						ItemStack itemStackInHand = player.getItemInHand();
						if (itemStackInHand.getAmount() == 1) {
							// player.setItemInHand(new
							// ItemStack(Material.AIR));
							player.setItemInHand(null);
						} else {
							itemStackInHand.setAmount(itemStackInHand.getAmount() - 1);
						}
					}
				} else {
					displayBook(player, bookInHand);
				}
			}

		} else if (event.getItem() != null && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getItem().getType() == Material.BOOK
				&& event.getItem().getDurability() != 0) {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				switch (event.getClickedBlock().getType()) {
				case BED_BLOCK:
				case BREWING_STAND:
				case BURNING_FURNACE:
				case CAKE_BLOCK:
				case CHEST:
				case DIODE_BLOCK_OFF:
				case DIODE_BLOCK_ON:
				case DISPENSER:
				case ENCHANTMENT_TABLE:
				case FENCE_GATE:
				case FURNACE:
				case JUKEBOX:
				case LEVER:
				case MINECART:
				case NETHER_FENCE:
				case NOTE_BLOCK:
				case STONE_BUTTON:
				case TRAP_DOOR:
				case WOODEN_DOOR:
				case WORKBENCH:
					return;
				}
			}

			Player player = event.getPlayer();

			Book book = library.getBook(event.getItem().getDurability());
			if (book == null) {
				Log.warning("The player " + player.getName() + " tried to read a book with the UID " + event.getItem().getDurability()
						+ ", but no book is associated with this uid. Removing UID from book.");
				event.getItem().setDurability((short) 0);
				return;
			}

			displayBook(player, book);
			event.setCancelled(true);
		}
	}

	/**
	 * Show the title and author when player selects a book
	 * 
	 * @param event
	 */
	@EventHandler
	public void onPlayerItemHeld(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();

		// Remove as viewer from previous book
		Book previousBook = Book.getCurrent(player);
		if (previousBook != null) {
			previousBook.removeViewer(player);
		}

		ItemStack item = player.getInventory().getItem(event.getNewSlot());
		if (item != null && item.getType() == Material.BOOK && item.getDurability() != 0) {
			Book book = library.getBook(item.getDurability());
			if (book == null) {
				Log.warning("The player " + player.getName() + " switched to a book with the UID " + item.getDurability() + ", but no book is associated with this uid. Removing UID from book.");
				item.setDurability((short) 0);
			} else {
				String additionalAuthors = ((book.getAuthors().isEmpty()) ? "" : ChatColor.RED + "+" + ChatColor.WHITE);
				String open = (book.isAuthor(player) && book.inWriteMode(player)) ? ChatColor.RED + " (open)" : "";

				player.sendMessage(book.getTitle() + Books.COLOR + " by " + ChatColor.WHITE + book.getCreator() + additionalAuthors + open);

				book.addViewer(player);
			}
		}
	}

	/**
	 * Enable the PlayerItemHeldEvent on login.
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		onPlayerItemHeld(new PlayerItemHeldEvent(event.getPlayer(), 0, 0));
	}

	/**
	 * Remove a player from the view cache
	 * 
	 * @param event
	 */
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Book book = Book.getCurrent(player);
		if (book != null) {
			book.removeViewer(player);
		}
	}

	// @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	// public void onInventoryClickOnItemHeld(InventoryClickEvent event) {
	// ItemStack cursorItem = event.getCursor();
	// ItemStack slotItem = event.getCurrentItem();
	//
	// if (slotItem != null && slotItem.getType() == Material.BOOK) {
	//
	// }
	//
	//
	// event.getWhoClicked().getInventory().getHeldItemSlot();
	// }

	/**
	 * Stacking of books with same UID, otherwise swap the items
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		ItemStack itemInSlot = event.getCurrentItem();
		ItemStack itemInHand = event.getCursor();

		if (event.isShiftClick() && itemInSlot.getType() == Material.BOOK) {
			if (!(event instanceof CraftItemEvent)) {
				boolean clickFromUpperInventory;
				if (event.getRawSlot() < event.getInventory().getSize()) {
					clickFromUpperInventory = true;
				} else {
					clickFromUpperInventory = false;
				}
				boolean allInserted = false;
				switch (event.getInventory().getType()) {
				case CREATIVE:
					return;
				case ENCHANTING:
				case BREWING:
					if (clickFromUpperInventory) {
						allInserted = InventoryManipulator.insert(itemInSlot, event.getWhoClicked().getInventory());
					}
					break;
				case FURNACE:
				case WORKBENCH:
				case CRAFTING:
					if (clickFromUpperInventory) {
						allInserted = InventoryManipulator.insert(itemInSlot, event.getWhoClicked().getInventory());
					} else {
						if (event.getSlot() < 9) {
							allInserted = InventoryManipulator.insert(itemInSlot, event.getWhoClicked().getInventory(), 9);
						} else {
							allInserted = InventoryManipulator.insert(itemInSlot, event.getWhoClicked().getInventory(), 0, 9);
						}
					}
					break;
				case CHEST:
				case DISPENSER:
					if (clickFromUpperInventory) {
						allInserted = InventoryManipulator.insert(itemInSlot, event.getWhoClicked().getInventory());
					} else {
						allInserted = InventoryManipulator.insert(itemInSlot, event.getInventory());
					}
					break;
				}

				if (allInserted) {
					event.setCurrentItem(null);
				}

				event.setCancelled(true);
			}
		} else if (itemInSlot != null && itemInSlot.getType() == Material.BOOK && itemInHand.getType() == Material.BOOK && itemInSlot.getDurability() != itemInHand.getDurability()) {
			if (event.isLeftClick()) {
				event.setCurrentItem(itemInHand);
				event.setCursor(itemInSlot);
			}

			event.setCancelled(true);
		}
	}

	/**
	 * Prevent unwanted stacking of books when picking up items, and call
	 * onPlayerItemHeld() if a book was added to the hand
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		ItemStack pickedUpItem = event.getItem().getItemStack();
		if (pickedUpItem.getType() == Material.BOOK) {
			// Check if the book is added to the hand
			// Maybe not the best way, but it works
			boolean handWasEmpty = false;
			if (event.getPlayer().getItemInHand().getType() == Material.AIR) {
				handWasEmpty = true;
			}

			if (InventoryManipulator.insert(pickedUpItem, event.getPlayer().getInventory())) {
				event.getItem().remove();
			}

			if (handWasEmpty && event.getPlayer().getItemInHand().getType() == Material.BOOK) {
				int heldItemSlot = event.getPlayer().getInventory().getHeldItemSlot();
				onPlayerItemHeld(new PlayerItemHeldEvent(event.getPlayer(), heldItemSlot, heldItemSlot));
			}

			event.setCancelled(true);
		}
	}

	/**
	 * Call onPlayerItemHeld() if the player drops a book
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (event.getItemDrop().getItemStack().getType() == Material.BOOK && event.getPlayer().getItemInHand().getType() == Material.AIR && event.getItemDrop().getItemStack().getDurability() != 0) {
			Book book = library.getBook(event.getItemDrop().getItemStack().getDurability());

			book.removeViewer(event.getPlayer());
		}
	}

	/**
	 * Allow pistons to move bookshelves that contains books
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		HashMap<Block, Book> movedBookshelves = new HashMap<Block, Book>();
		for (Block movableBlock : event.getBlocks()) {
			if (movableBlock.getType() == Material.BOOKSHELF) {
				Book containedBook = library.getBook(movableBlock);
				if (containedBook != null) {
					movedBookshelves.put(movableBlock, containedBook);
					library.removeBook(movableBlock);
				}
			}
		}

		for (Entry<Block, Book> movedBookshelf : movedBookshelves.entrySet()) {
			library.addBook(movedBookshelf.getValue(), movedBookshelf.getKey().getRelative(event.getDirection()));
		}
	}

	/**
	 * Allow sticky pistons retract bookshelves that contains books
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		if (event.isSticky() && event.getRetractLocation().getBlock().getType() == Material.BOOKSHELF) {
			Book containedBook = library.getBook(event.getRetractLocation().getBlock());
			if (containedBook != null) {
				library.removeBook(event.getRetractLocation().getBlock());
				library.addBook(containedBook, event.getRetractLocation().getBlock().getRelative(event.getDirection().getOppositeFace()));
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerChat(PlayerChatEvent event) {
		Player player = event.getPlayer();
		if (player.getItemInHand().getType() == Material.BOOK && player.getItemInHand().getDurability() != 0) {
			Book book = library.getBook(player.getItemInHand().getDurability());
			if (book != null && book.isAuthor(player) && book.inWriteMode(player)) {
				player.chat("/w " + event.getMessage());
				event.setCancelled(true);
			}
		}
	}
}
