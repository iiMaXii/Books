package com.djupfryst.books.library;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.djupfryst.books.general.Log;
import com.djupfryst.books.general.StringUtils;

/**
 * @author iiMaXii
 */
class AuthorPermissions {
	boolean renderColours;
	boolean renderFormatting;

	AuthorPermissions(boolean renderColours, boolean renderFormatting) {
		this.renderColours = renderColours;
		this.renderFormatting = renderFormatting;
	}

	public AuthorPermissions() {
	}
}

/**
 * @author iiMaXii
 */
public class Library implements Runnable, Listener {
	static File DIRECTORY;

	private ArrayList<Book> books; // Note, the UID is index+1
	private HashMap<Block, Book> bookshelves;

	private HashMap<String, AuthorPermissions> permissions;

	public Library(File dataFolder) {
		DIRECTORY = new File(dataFolder, "books");

		books = new ArrayList<Book>();
		bookshelves = new HashMap<Block, Book>();

		permissions = new HashMap<String, AuthorPermissions>();

		Book.setLibrary(this);
	}

	AuthorPermissions getPermissions(String author) {
		AuthorPermissions permission = permissions.get(author);
		if (permission == null) {
			permission = new AuthorPermissions();
			permissions.put(author, permission);
		}

		return permission;
	}

	/**
	 * Add a book
	 * 
	 * @param book
	 *            The book to add
	 * @return The books UID
	 */
	public short addBook(Book book) {
		if (books.size() >= Short.MAX_VALUE) {
			Log.severe("The number of books have exceeded the maximum value.");
		}
		short uid = (short) (books.size() + 1);

		book.setUID(uid);
		books.add(book);

		return uid;
	}

	/**
	 * Put a book in a block
	 * 
	 * @param book
	 *            The book to add the book to
	 * @param block
	 *            The block
	 */
	public void addBook(Book book, Block block) {
		bookshelves.put(block, book);
	}

	/**
	 * Remove a book from the library
	 * 
	 * @param book
	 *            The book that should be removed
	 */
	public void removeBook(Book book) {
		books.set(book.getUID() - 1, null);

		Iterator<Entry<Block, Book>> bookshelvesIterator = bookshelves.entrySet().iterator();
		while (bookshelvesIterator.hasNext()) {
			Entry<Block, Book> next = bookshelvesIterator.next();
			if (next.getValue() == book) {
				bookshelvesIterator.remove();
			}
		}
	}

	/**
	 * Get a book
	 * 
	 * @param uid
	 *            The books UID
	 * @return The book
	 */
	public Book getBook(short uid) {
		if (uid > books.size())
			return null;

		return books.get(uid - 1);
	}

	/**
	 * Get a book located in a block
	 * 
	 * @param block
	 *            The block that contains the book
	 * @return The book found in the bookshelf or null if no book was found
	 */
	public Book getBook(Block block) {
		return bookshelves.get(block);
	}

	/**
	 * Remove a book from a block
	 * 
	 * @param block
	 *            The block that contains the book
	 * @return True if the block contained a book, otherwise false
	 */
	public boolean removeBook(Block block) {
		return bookshelves.remove(block) != null;
	}

	private static String readFile(File file) {
		StringBuilder stringBuilder = new StringBuilder();
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line).append("\n");
			}

			bufferedReader.close();
		} catch (IOException e) {
			Log.warning("Could not read the file " + file.toString());
		}

		return stringBuilder.toString();
	}

	public void load() {
		if (!DIRECTORY.exists()) {
			convertFromBookworm();
		}

		// Permissions
		File permissionsFile = new File(DIRECTORY.getParentFile(), "permissions.yml");
		if (permissionsFile.exists()) {
			YamlConfiguration permissionsConfig = new YamlConfiguration();
			try {
				permissionsConfig.load(permissionsFile);
				for (String author : permissionsConfig.getKeys(false)) {
					Log.info("Read " + author + ": " + permissionsConfig.getBoolean(author + ".renderColours") + ", " + permissionsConfig.getBoolean(author + ".renderFormatting"));
					permissions.put(author, new AuthorPermissions(permissionsConfig.getBoolean(author + ".renderColours"), permissionsConfig.getBoolean(author + ".renderFormatting")));
				}
			} catch (IOException e) {
				Log.warning("Could not read file " + permissionsFile.toString());
			} catch (InvalidConfigurationException e) {
				Log.warning("Could not read file " + permissionsFile.toString() + ", invalid configuration");
				e.printStackTrace();
			}
		}

		File[] bookFiles = DIRECTORY.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.matches("^\\d+\\.txt$");
			}
		});

		if (bookFiles != null && bookFiles.length != 0) {
			for (File bookFile : bookFiles) {
				Book book = Book.fromFile(bookFile.getName());

				short uid;
				if (book != null) {
					uid = book.getUID();
				} else {
					uid = Short.parseShort(bookFile.getName().split("\\.")[0]);
				}

				int index = uid - 1;
				if (books.size() == index) {
					books.add(book);
				} else if (books.size() > index) {
					books.set(index, book);
				} else {
					while (books.size() < index) {
						books.add(null);
					}
					books.add(book);
				}
			}

			// Bookshelves
			File bookshelvesFile = new File(DIRECTORY, "bookshelves.txt");
			if (bookshelvesFile.exists()) {
				String bookshelvesString = readFile(bookshelvesFile);
				String[] bookshelfLines = bookshelvesString.split("\n");

				int line = 1;
				bookshelves: for (String bookshelf : bookshelfLines) {
					String[] currentBookshelf = bookshelf.split(",");

					if (currentBookshelf.length == 5) {
						World bookshelfWorld = Bukkit.getWorld(currentBookshelf[0]);

						int[] integers = new int[4];
						for (int i = 0; i < 4; i++) {
							try {
								integers[i] = Integer.parseInt(currentBookshelf[i + 1]);
							} catch (NumberFormatException e) {
								Log.warning("Could not read line " + line + " in file " + bookshelvesFile.toString() + " (got " + currentBookshelf[i + 1] + " expected integer)");
								continue bookshelves;
							}
						}

						Location bookshelfLocation = new Location(bookshelfWorld, integers[0], integers[1], integers[2]);

						if (bookshelfLocation.getBlock().getType() == Material.BOOKSHELF) {
							Book bookshelfBook = getBook((short) integers[3]);
							if (bookshelfBook != null) {
								bookshelves.put(bookshelfLocation.getBlock(), bookshelfBook);
							} else {
								Log.warning("Book with UID " + integers[3] + " located in bookshelf at " + StringUtils.toString(bookshelfLocation)
										+ " could not be added since the no book was assosiated with the UID");
							}
						} else {
							Log.warning("Book with UID " + integers[3] + " located in bookshelf at " + StringUtils.toString(bookshelfLocation)
									+ " could not be added since the found block was of type " + bookshelfLocation.getBlock().getType().toString());
						}
					} else {
						Log.warning("Could not read line " + line + " in file " + bookshelvesFile.toString() + " (got " + currentBookshelf.length + " entries expected 5)");
					}
					line++;
				}
			}
		}
	}

	public void save() {
		if (!DIRECTORY.exists()) {
			Log.info("Book directory does not exist, creating it");
			if (!DIRECTORY.mkdirs())
				Log.warning("Unable to create directory " + DIRECTORY.toString());
		}

		// Books
		for (int i = 0; i < books.size(); i++) {
			Book book = books.get(i);

			if (book != null) {
				book.save();
			} else {
				File file = new File(DIRECTORY, (i + 1) + ".txt");
				if (i == books.size() - 1) {
					if (!file.exists()) {
						try {
							file.createNewFile();
						} catch (IOException e) {
							Log.warning("Unable to create the file " + file.toString());
						}
					} else {
						try {
							int next = new FileInputStream(file).read();
							if (next != -1) {
								FileOutputStream fileOutputStream = new FileOutputStream(file);
								fileOutputStream.write(new byte[] {});
								fileOutputStream.close();
								Log.info("File " + file.toString() + " has been cleared.");
							}
						} catch (IOException e) {
							Log.warning("Could not touch the file " + file.toString());
						}
					}
				} else if (file.exists()) {
					if (i != books.size() - 1 && !file.delete()) {
						Log.warning("Unable to delete the file " + file.toString());
					}
				}
			}
		}

		// Bookshelves
		File bookshelvesFile = new File(DIRECTORY, "bookshelves.txt");
		if (bookshelves.size() == 0) {
			if (bookshelvesFile.exists()) {
				if (!bookshelvesFile.delete()) {
					Log.warning("Unable to delete the file " + bookshelvesFile.toString());
				}
			}
		} else {
			String bookshelvesString;
			{
				StringBuilder stringBuilder = new StringBuilder();
				for (Entry<Block, Book> bookshelf : bookshelves.entrySet()) {
					Location currentBlockLocation = bookshelf.getKey().getLocation();
					short currentUID = bookshelf.getValue().getUID();

					stringBuilder.append(currentBlockLocation.getWorld().getName() + ',' + currentBlockLocation.getBlockX() + ',' + currentBlockLocation.getBlockY() + ','
							+ currentBlockLocation.getBlockZ() + ',' + currentUID + "\n");
				}
				bookshelvesString = stringBuilder.toString();
			}

			try {
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(bookshelvesFile), "UTF-8");
				outputStreamWriter.write(bookshelvesString);
				outputStreamWriter.close();
			} catch (IOException e) {
				Log.warning("Unable to write to file " + bookshelvesFile.toString());
				Log.dump(bookshelvesFile, bookshelvesString);
			}
		}

		// Permissions
		File permissionsFile = new File(DIRECTORY.getParentFile(), "permissions.yml");
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(permissionsFile), "UTF-8"));
			bufferedWriter.write("# This file stores information about who is allowed to use  #\r\n");
			bufferedWriter.write("# colours and formatting in books. This because permissions #\r\n");
			bufferedWriter.write("# aren't accessible once a user is offline.                 #\r\n");
			bufferedWriter.write("#                                                           #\r\n");
			bufferedWriter.write("# WARNING! DO NOT EDIT THIS FILE AS IT IS UPDATED EVERYTIME #\r\n");
			bufferedWriter.write("# SOMEONE WRITES TO A BOOK. USE PERMISSIONS INSTEAD.        #\r\n");
			bufferedWriter.close();
		} catch (IOException e) {
			Log.warning("Failed to add commentary to file " + permissionsFile);
		}
		YamlConfiguration permissionsConfig = new YamlConfiguration();

		try {
			permissionsConfig.load(permissionsFile);
		} catch (IOException e) {
			Log.warning("Unable to read file " + permissionsFile);
		} catch (InvalidConfigurationException e) {
			Log.warning("Unable to read file " + permissionsFile);
		}

		for (Entry<String, AuthorPermissions> permission : permissions.entrySet()) {
			permissionsConfig.set(permission.getKey() + ".renderColours", permission.getValue().renderColours);
			permissionsConfig.set(permission.getKey() + ".renderFormatting", permission.getValue().renderFormatting);
		}

		try {
			permissionsConfig.save(permissionsFile);
		} catch (IOException e) {
			Log.warning("Failed trying to save data to " + permissionsFile.toString());

			Log.dump(permissionsFile, permissionsConfig.toString());
		}
	}

	public void run() {
		for (int i = 0; i < books.size(); i++) {
			Book book = books.get(i);

			if (book != null) {
				book.clean();
			} else {
				File file = new File(DIRECTORY, (i + 1) + ".txt");
				if (i == books.size() - 1) {
					if (!file.exists()) {
						try {
							file.createNewFile();
						} catch (IOException e) {
							Log.warning("Unable to create the file " + file.toString());
						}
					} else {
						try {
							int next = new FileInputStream(file).read();
							if (next != -1) {
								FileOutputStream fileOutputStream = new FileOutputStream(file);
								fileOutputStream.write(new byte[] {});
								fileOutputStream.close();
								Log.info("File " + file.toString() + " has been cleared.");
							}
						} catch (IOException e) {
							Log.warning("Could not touch the file " + file.toString());
						}
					}
				} else if (file.exists()) {
					if (i != books.size() - 1 && !file.delete()) {
						Log.warning("Unable to delete the file " + file.toString());
					}
				}
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		for (Book book : books) {
			if (book != null && book.isAuthor(player) && book.inWriteMode(player)) {
				book.setWriteMode(player, false);
			}
		}
	}

	/**
	 * TODO read charset from BookWorm config.yml
	 * 
	 * @param books
	 */
	private boolean convertFromBookworm() {
		File bookWormDirectory = new File(DIRECTORY.getParentFile().getParentFile(), "BookWorm");
		if (bookWormDirectory.exists()) {
			File[] bookWormBooks = bookWormDirectory.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.matches("^\\d+_.*") || name.matches("^\\d+.txt$");
				}
			});
			if (bookWormBooks != null && bookWormBooks.length != 0) {
				Log.info("Converting books from BookWorm");
				if (!DIRECTORY.exists())
					DIRECTORY.mkdirs();

				LinkedList<Integer> uidList = new LinkedList<Integer>();
				for (File bookWormBook : bookWormBooks) {
					String[] lines = readFile(bookWormBook).split("\n");
					if (lines.length < 3) {
						Log.warning("Could not convert the file " + bookWormBook.toString() + ", got " + lines.length + " line(s) expected atleast 4");
					} else {
						File saveFile = null;
						try {
							int uid = Integer.parseInt(lines[0]);
							String title = lines[1];
							String author = lines[2];
							String content = (lines.length == 3) ? "" : "::" + lines[3].replaceAll("(?i)" + ChatColor.COLOR_CHAR + "([0-9a-fk-or])", "&$1");

							saveFile = new File(DIRECTORY, uid + ".txt");
							OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(saveFile), "UTF-8");
							outputStreamWriter.write(title + "\n" + author + "\n" + content);
							outputStreamWriter.close();
							uidList.add(uid);
							Log.info("The book \"" + title + "\" by " + author + " has been converted");
						} catch (NumberFormatException e) {
							Log.warning("Could not convert the file " + bookWormBook.toString() + ", first line is not an integer");
						} catch (IOException e) {
							Log.warning("Could not convert the file " + bookWormBook.toString() + ", unable to create new file " + saveFile.toString());
						}
					}
				}

				File bookWormBookshelves = new File(bookWormDirectory, "bookshelves.txt");
				String[] lines = readFile(bookWormBookshelves).split("\n");
				StringBuilder b = new StringBuilder();
				int convertedBookshelves = 0;
				bookWormBookshelves: for (int row = 0; row < lines.length; row++) {
					if (lines[row].isEmpty())
						continue;

					String[] lineData = lines[row].split("[,:]");
					if (lineData.length == 5) {
						if (Bukkit.getWorld(lineData[0]) != null) {
							int[] additionalData = new int[4];
							for (int i = 0; i < 4; i++) {
								try {
									additionalData[i] = Integer.parseInt(lineData[i + 1]);
								} catch (NumberFormatException e) {
									Log.warning("Could not convert the row " + row + " in " + bookWormBookshelves.toString() + ", got \"" + additionalData[i] + "\" expected an integer");
									continue bookWormBookshelves;
								}
							}

							if (uidList.contains(additionalData[3])) {
								b.append(lineData[0]).append(',').append(additionalData[0]).append(',').append(additionalData[1]).append(',').append(additionalData[2]).append(',')
										.append(additionalData[3]);
								convertedBookshelves++;
							} else {
								Log.warning("Could not convert the row " + row + " in " + bookWormBookshelves.toString() + ", no book found with id " + additionalData[3]);
							}
						} else {
							Log.warning("Could not convert the row " + row + " in " + bookWormBookshelves.toString() + ", could not find the world \"" + lineData[0] + "\"");
						}
					} else {
						Log.warning("Could not convert the row " + row + " in " + bookWormBookshelves.toString() + ", got " + lineData.length + " entries expected 5");
					}

				}

				if (b.length() != 0) {
					File bookshelvesFile = new File(DIRECTORY, "bookshelves.txt");
					OutputStreamWriter outputStreamWriter;
					try {
						outputStreamWriter = new OutputStreamWriter(new FileOutputStream(bookshelvesFile), "UTF-8");
						outputStreamWriter.write(b.toString());
						outputStreamWriter.close();
						if (convertedBookshelves == 1) {
							Log.info(convertedBookshelves + " bookshelf have been converted");
						} else {
							Log.info(convertedBookshelves + " bookshelves have been converted");
						}
					} catch (IOException e) {
						Log.warning("An error occured when trying to create the file " + bookshelvesFile.toString());
					}
				}
				return true;
			}
		}
		return false;
	}
}
