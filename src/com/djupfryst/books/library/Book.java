package com.djupfryst.books.library;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.djupfryst.books.Books;
import com.djupfryst.books.general.Doc;
import com.djupfryst.books.general.Log;
import com.djupfryst.books.general.MinecraftFont;
import com.djupfryst.books.general.MinecraftStringBuilder;
import com.djupfryst.books.general.StringUtils;
import com.djupfryst.books.library.actions.*;

/**
 * Represents an in-game book
 * 
 * @author iiMaXii
 * 
 */
public class Book {
	private final static int LINES_PER_PAGE = 7;
	private final static int WORD_SPLIT_MINIMUM_WORD_WIDTH = 80;
	// public final static int WORD_SPLIT_MINIMUM_WORD_WIDTH_SECOND_LINE = 20;
	private final static int WORD_SPLIT_MAXIMUM_LINE_WIDTH = MinecraftFont.CHAT_WINDOW_WIDTH - WORD_SPLIT_MINIMUM_WORD_WIDTH;
	private final static char[] END_CHARACTERS = { '.', ',', ':', ';', '?', '!', ' ' };

	// Books that players are currently holding
	private static HashMap<Player, Book> currentBooks = new HashMap<Player, Book>();

	private static Library library;

	static void setLibrary(Library library) {
		Book.library = library;
	}

	private long lastEdited, lastRendered, lastSaved, lastViewed;

	private String title;
	private String creator;
	private HashSet<String> authors;
	private short uid;

	private String content;
	private ArrayList<String> rendered;

	private ActionList actions, undoneActions;

	private AuthorPermissions permissions;
	private HashSet<String> writeMode;
	private HashSet<Player> authorsViewing;

	Book(short uid, Player player, String title, String content) {
		this(uid);

		this.creator = player.getName();
		this.title = title;
		this.content = content;

		lastSaved = -1; // Make sure we save

		permissions = library.getPermissions(creator);
	}

	private Book(short uid) {
		this.uid = uid;

		authors = new HashSet<String>();

		rendered = new ArrayList<String>();

		actions = new ActionList();
		undoneActions = new ActionList();

		writeMode = new HashSet<String>();
		authorsViewing = new HashSet<Player>();

		lastRendered = -1; // Make sure we render it
	}

	/**
	 * Create a new book
	 * 
	 * @param title
	 *            Title of the book
	 * @param owner
	 *            The player who created the book
	 */
	public Book(Player player, String title) {
		this((short) -1, player, title, new String());
	}

	public boolean isCreator(OfflinePlayer player) {
		return creator.equalsIgnoreCase(player.getName());
	}

	public boolean isCreator(String playerName) {
		return creator.equalsIgnoreCase(playerName);
	}

	/**
	 * Add an additional author
	 * 
	 * @param player
	 *            The player to add
	 */
	public void addAuthor(Player player) {
		authors.add(player.getName());

		// If the added player is holding the book, add as viewer
		if (player.getItemInHand().getType() == Material.BOOK && player.getItemInHand().getDurability() == uid) {
			this.addViewer(player);
		}
		lastEdited = System.currentTimeMillis();
	}

	public void removeAuthor(OfflinePlayer mentionedPlayer) {
		StringUtils.removeIgnoreCase(authors, mentionedPlayer.getName());

		// Remove the player from viewing authors
		Iterator<Player> iterator = authorsViewing.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getName().equalsIgnoreCase(mentionedPlayer.getName()))
				iterator.remove();
		}
		lastEdited = System.currentTimeMillis();
	}

	/**
	 * Get a list of additional authors, the creator is not included
	 * 
	 * @return A list of additional authors
	 */
	public Set<String> getAuthors() {
		return authors;
	}

	/**
	 * Check if the player is an author
	 * 
	 * @param player
	 *            The player to check
	 * @return True if the player is an author, otherwise false
	 */
	public boolean isAuthor(OfflinePlayer player) {
		return isAuthor(player.getName());
	}

	/**
	 * Check if the player is an author
	 * 
	 * @param player
	 *            The player to check
	 * @return True if the player is an author, otherwise false
	 */
	public boolean isAuthor(String playerName) {
		return isCreator(playerName) || StringUtils.containsIgnoreCase(authors, playerName);
	}

	/**
	 * Set the books UID
	 * 
	 * @param uid
	 *            The new UID
	 */
	void setUID(short uid) {
		this.uid = uid;
	}

	/**
	 * Get the books UID
	 * 
	 * @return The UID
	 */
	public short getUID() {
		return uid;
	}

	/**
	 * Set the title of the book
	 * 
	 * @param title
	 *            The title of the book
	 */
	public void setTitle(String title) {
		actions.add(new TitleChangeAction(this.title));

		// If we have undone actions these will be removed
		undoneActions.clear();

		this.title = title;
	}

	/**
	 * Get the title of the book
	 * 
	 * @return The books title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Get the creator of the book
	 * 
	 * @return The creator
	 */
	public String getCreator() {
		return creator;
	}

	/**
	 * Get the contents of the book
	 * 
	 * @return The contents of the book
	 * @throws BookLoadException
	 */
	public String getContent() throws BookLoadException {
		load();

		lastViewed = System.currentTimeMillis();
		return content;
	}

	/**
	 * Set the contents of the book, replacing any previously written text
	 * 
	 * @param content
	 *            The contents
	 * @throws BookLoadException
	 */
	public void setContent(String newContent) throws BookLoadException {
		load();

		actions.add(new ReplaceAction(0, newContent.length(), content));

		// If we have undone actions these will be removed
		undoneActions.clear();

		content = newContent;
		lastEdited = System.currentTimeMillis();
	}

	private boolean isEndOfParagraph() {
		String lastTwo = content.substring(content.length() - 2);
		return lastTwo.equals(";;") || lastTwo.equals("::");
	}

	/**
	 * Add text to the last paragraph
	 * 
	 * @param text
	 *            The text we want to append
	 * @throws BookLoadException
	 */
	public void append(String text) throws BookLoadException {
		load();

		if (!content.isEmpty() && !StringUtils.contains(END_CHARACTERS, text.charAt(0)) && !isEndOfParagraph()) {
			text = ' ' + text;
		}

		actions.add(new AddAction(text.length()));

		// If we have undone actions these will be removed
		undoneActions.clear();

		content += text;
		lastEdited = System.currentTimeMillis();
	}

	/**
	 * Perform a undo or redo action
	 * 
	 * @param action
	 *            The action to perform
	 * @return Action that undoes the performed action
	 */
	private Action performAction(Action action) {
		Action reverseAction;

		if (action instanceof AddAction) {
			AddAction addAction = (AddAction) action;

			String removed = content.substring(content.length() - addAction.getNumberOfCharactersAdded(), content.length());
			String newContent = content.substring(0, content.length() - addAction.getNumberOfCharactersAdded());

			content = newContent;

			reverseAction = new RemoveAction(removed);
			lastEdited = System.currentTimeMillis();
		} else if (action instanceof RemoveAction) {
			RemoveAction removeAction = (RemoveAction) action;

			content += removeAction.getRemovedString();

			reverseAction = new AddAction(removeAction.getRemovedString().length());
			lastEdited = System.currentTimeMillis();
		} else if (action instanceof ReplaceAction) {
			ReplaceAction replaceAction = (ReplaceAction) action;

			// Save in case redo method is called
			int reverseStart = replaceAction.getStart();
			int reverseEnd = reverseStart + replaceAction.getReplacedString().length();
			String reverseReplacedString = content.substring(replaceAction.getStart(), replaceAction.getEnd());

			// Undo the action
			String firstPart = content.substring(0, replaceAction.getStart());
			String secondPart = content.substring(replaceAction.getEnd());
			content = firstPart + replaceAction.getReplacedString() + secondPart;

			reverseAction = new ReplaceAction(reverseStart, reverseEnd, reverseReplacedString);
			lastEdited = System.currentTimeMillis();
		} else if (action instanceof TitleChangeAction) {
			TitleChangeAction titleChangeAction = (TitleChangeAction) action;

			reverseAction = new TitleChangeAction(title);

			title = titleChangeAction.getPreviousTitle();
		} else {
			reverseAction = null;
		}

		return reverseAction;
	}

	/**
	 * Undo the last action
	 * 
	 * @return The inverse action
	 * @throws BookLoadException
	 */
	public Action undo() throws BookLoadException {
		load();

		Action action = actions.get();
		if (action == null)
			return null;

		Action reverseAction = performAction(action);
		undoneActions.add(reverseAction);

		return action;
	}

	/**
	 * Redo the last undone action
	 * 
	 * @return The inverse action
	 * @throws BookLoadException
	 */
	public Action redo() throws BookLoadException {
		load();

		Action action = undoneActions.get();
		if (action == null)
			return null;

		Action reverseAction = performAction(action);
		actions.add(reverseAction);

		return action;
	}

	/**
	 * Perform a replace
	 * 
	 * @param start
	 *            The start of the substring that will be replaced
	 * @param end
	 *            The end of the substring that will be replaced
	 * @param string
	 *            The that will be inserted
	 * @throws BookLoadException
	 */
	public void replace(int start, int end, String string) throws BookLoadException {
		load();

		String stringBefore = content.substring(0, start);
		String replacedString = content.substring(start, end);
		String stringAfter = content.substring(end);

		content = stringBefore + string + stringAfter;
		lastEdited = System.currentTimeMillis();

		actions.add(new ReplaceAction(start, start + string.length(), replacedString));
	}

	/**
	 * Perform a replace and remove unwanted line breaks
	 * 
	 * @param start
	 *            The start of the substring that will be replaced
	 * @param end
	 *            The end of the substring that will be replaced
	 * @param string
	 *            The that will be inserted
	 * @throws BookLoadException
	 */
	public void smartReplace(int start, int end, String string) throws BookLoadException {
		load();

		// Remove unwanted line breaks
		if (string.isEmpty()) {
			if (start == 0) {
				if (end != content.length() && content.charAt(end) == ' ') {
					end++;
				}
			} else {
				if (end == content.length() && content.charAt(start - 1) == ' ') {
					start--;
				} else if (content.charAt(start - 1) == ' ' && end != content.length() && content.charAt(end) == ' ') {
					start--;
				}
			}
		}

		replace(start, end, string);

		// String stringBefore = content.substring(0, start);
		// String replacedString = content.substring(start, end);
		// String stringAfter = content.substring(end);
		//
		// content = stringBefore + string + stringAfter;
		// lastEdited = System.currentTimeMillis();
		//
		// actions.add(new ReplaceAction(start, start + string.length(),
		// replacedString));
	}

	private static boolean isColourChar(char c) {
		if (0x30 <= c && c <= 0x39 || 0x41 <= c && c <= 0x46 || 0x61 <= c && c <= 0x66) {
			return true;
		} else {
			return false;
		}
	}

	private static boolean isFormatChar(char c) {
		if (0x6C <= c && c <= 0x6F || c == 0x72) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Parse the colour and formatting codes
	 * 
	 * @param string
	 *            The string to parse
	 * @param parseColours
	 *            If colours should be parsed
	 * @param parseFormatting
	 *            If text formatting should be parsed
	 * @return The new parsed string
	 */
	private static String parseSpecials(String string, boolean parseColours, boolean parseFormatting) {
		if (parseColours == false && parseFormatting == false)
			return string;

		StringBuffer b = new StringBuffer(string.length());

		int lastEnd = 0;
		int index = string.indexOf('&');
		while (index != -1) {
			if (index + 2 <= string.length() && (parseColours && isColourChar(string.charAt(index + 1)) || parseFormatting && isFormatChar(string.charAt(index + 1)))) {
				int slashBegin = index;
				while (slashBegin != 0 && string.charAt(--slashBegin) == '\\')
					;
				if (slashBegin != 0) {
					slashBegin++;
				}

				b.append(string.substring(lastEnd, slashBegin));

				for (int i = 0; i < (index - slashBegin) / 2; i++) {
					b.append('\\');
				}

				b.append(((index - slashBegin) % 2 == 0) ? ChatColor.COLOR_CHAR : '&').append(string.charAt(index + 1));

				lastEnd = index + 2;
			}
			index = string.indexOf('&', index + 1);
		}
		b.append(string.substring(lastEnd));

		return b.toString();
	}

	/**
	 * Render the book (parse colours, divide into pages etc.), if not already
	 * rendered
	 * 
	 * @throws BookLoadException
	 */
	private void render() throws BookLoadException {
		load();

		if (lastRendered < lastEdited || permissionsUpdated()) {
			rendered.clear();

			if (content.isEmpty()) {
				lastRendered = System.currentTimeMillis();
				return;
			}

			String tempContent = parseSpecials(content, permissions.renderColours, permissions.renderFormatting);

			boolean paragraph = false;
			for (String textParagraph : tempContent.split("::")) {
				// Log.info("Processing: " + textParagraph);
				if (textParagraph.isEmpty()) {
					paragraph = true;
					// Log.info("Empty");
					continue;
				}

				MinecraftStringBuilder line = new MinecraftStringBuilder();
				if (paragraph) {
					line.append("     ");
				}

				for (String textLine : textParagraph.split(";;")) {
					String[] textLineWords = textLine.split(" ");
					int index = 0;
					while (index < textLineWords.length) {
						// Log.info("Processing word: " + textLineWords[index] +
						// ", " + MinecraftFont.getWidth(textLineWords[index]));
						String word = textLineWords[index];
						if (word.isEmpty()) {
							line.append(' ');
							index++;
							continue;
						}

						if (line.getPixelLength() > 0) {
							word = ' ' + word;
						}

						int currentWordWidth = MinecraftFont.getWidth(word);
						if ((line.getPixelLength() + currentWordWidth) <= MinecraftFont.CHAT_WINDOW_WIDTH) {
							// Log.info(currentLineWidth + "+" +
							// currentWordWidth +
							// "=" + (currentLineWidth + currentWordWidth) +
							// "<=" +
							// MinecraftFont.CHAT_WINDOW_WIDTH);

							line.append(word);
							index++;
						} else if (WORD_SPLIT_MINIMUM_WORD_WIDTH <= currentWordWidth && line.getPixelLength() <= WORD_SPLIT_MAXIMUM_LINE_WIDTH) {
							StringBuilder currentWord = new StringBuilder(word);
							MinecraftStringBuilder splitWord = new MinecraftStringBuilder(line);
							int hyphenPixelWidth = MinecraftFont.getWidth('-');
							while ((line.getPixelLength() + splitWord.getPixelLength() + hyphenPixelWidth) <= MinecraftFont.CHAT_WINDOW_WIDTH) {
								splitWord.append(currentWord.charAt(0));
								currentWord.deleteCharAt(0);
							}
							line.append('-');
							// Log.info("Adding line (1): \"" + line.toString()
							// +
							// "\" width:" + line.getPixelLength() + ", real:" +
							// MinecraftFont.getWidth(line.toString()));
							rendered.add(line.toString());
							line = new MinecraftStringBuilder(line.getCurrentFormatting());
							textLineWords[index] = currentWord.toString();
						} else {
							// Log.info("Adding line (2): \"" + line.toString()
							// +
							// "\" width:" + line.getPixelLength() + ", real:" +
							// MinecraftFont.getWidth(line.toString()));
							rendered.add(line.toString());
							line = new MinecraftStringBuilder(line.getCurrentFormatting());
						}
					}
					if (line.length() != 0) {
						// Log.info("Adding line (3): \"" + line.toString() +
						// "\" width:" + line.getPixelLength() + ", real:" +
						// MinecraftFont.getWidth(line.toString()));
						rendered.add(line.toString());
						line = new MinecraftStringBuilder(line.getCurrentFormatting());
					}
				}
				paragraph = true;
			}

			lastRendered = System.currentTimeMillis();
		}
	}

	/**
	 * Get total number of pages of the book
	 * 
	 * @return The number of pages in the book
	 * @throws BookLoadException
	 */
	public int getPages() throws BookLoadException {
		render();

		lastViewed = System.currentTimeMillis();

		return Doc.calculateNumberOfPages(rendered.size(), LINES_PER_PAGE);
	}

	/**
	 * The the specified page from the book
	 * 
	 * @param page
	 *            The page number
	 * @return The requested page
	 * @throws BookLoadException
	 */
	public String[] getPage(int page) throws BookLoadException {
		render();

		int[] interval = Doc.calculateInterval(rendered.size(), page, LINES_PER_PAGE);
		if (interval == null)
			return null;

		String[] requestedLines = new String[interval[1] - interval[0]];
		int count = 0;
		for (int i = interval[0]; i < interval[1]; i++) {
			requestedLines[count] = rendered.get(i);
			count++;
		}

		lastViewed = System.currentTimeMillis();

		return requestedLines;
	}

	/**
	 * Save the book to disk, if not already saved
	 */
	void save() {
		if (lastEdited > lastSaved) {
			File file = new File(Library.DIRECTORY, uid + ".txt");

			StringBuilder authorsBuilder = new StringBuilder(creator);
			for (String author : authors) {
				authorsBuilder.append(',').append(author);
			}

			String fileContent = title + "\n" + authorsBuilder.toString() + "\n" + content;
			try {
				BufferedWriter bufferWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
				bufferWriter.write(fileContent);
				bufferWriter.close();

				lastSaved = System.currentTimeMillis();
			} catch (IOException e) {
				Log.severe("Failed saving the book with uid " + uid + ", dumping file content");
				Log.dump(file, fileContent);
			}
		}
	}

	/**
	 * Load the book from disk, if not already in memory
	 * 
	 * @return True if the file is loaded correctly, or false if the file was
	 *         not found or invalid
	 * @throws BookLoadException
	 */
	private void load() throws BookLoadException {
		if (content == null) {
			File file = new File(Library.DIRECTORY, uid + ".txt");

			String[] lines = readFile(file);

			if (lines == null || lines.length != 3 && lines.length != 2) {
				if (lines == null) {
					Log.warning("Could not parse the book located in the file " + file.toString() + " (unable to read file)");
				} else {
					Log.warning("Could not parse the book located in the file " + file.toString() + " (got " + lines.length + " line(s) expected 2 or 3)");
				}

				File newFile = new File(Library.DIRECTORY, "trash/" + uid + ".txt");
				if (newFile.exists()) {
					newFile.delete();
				} else if (!newFile.getParentFile().exists()) {
					newFile.getParentFile().mkdir();
				}
				if (file.renameTo(newFile)) {
					library.removeBook(this);
					Log.info("The book has been moved to " + newFile.toString());
				} else {
					Log.warning("Unable to move " + file.toString() + " to trash (" + newFile.toString() + ")");
				}
				throw new BookLoadException();
			} else {
				title = lines[0];

				String[] authors = lines[1].split(",");
				creator = authors[0];
				this.authors.clear();
				for (int i = 1; i < authors.length; i++) {
					this.authors.add(authors[i]);
				}

				content = (lines.length == 3) ? lines[2] : new String();
			}
		}
	}

	/**
	 * Remove any unused data from the memory
	 */
	void clean() {
		long now = System.currentTimeMillis();

		if ((!actions.isEmpty() || !actions.isEmpty()) && now - lastEdited > 1000 * 3600) {
			// Remove all actions
			actions.clear();
			undoneActions.clear();
			Log.info("Actions unloaded from " + title + " (" + uid + ")");
		}

		if (content != null && now - lastEdited > 1000 * 3600) {
			save();
			// Remove the content
			content = null;
			Log.info("Content unloaded from book " + title + " (" + uid + ")");
		}

		if (rendered.size() != 0 && now - lastViewed > 1000 * 3600 * 3) {
			// Remove the rendered content
			rendered.clear();
			lastRendered = -1;
			Log.info("Rendered content unloaded from book " + title + " (" + uid + ")");
		}
	}

	/**
	 * Try to create a new book from a file
	 * 
	 * @param file
	 *            The file that contains the book. (Note the file will be loaded
	 *            from the directory specified with Book.setDirectory())
	 * @return The book object or null if the file was invalid or not found
	 */
	static Book fromFile(String file) {
		short uid = Short.parseShort(file.split("\\.")[0]);

		Book book = new Book(uid);

		String[] lines = readFile(new File(Library.DIRECTORY, file), 2);

		if (lines == null || lines.length != 2) {
			int nbrLines = (lines == null) ? -1 : lines.length;
			// An empty file may be created so we know the book got deleted
			if (nbrLines != 0) {
				Log.warning("Could not parse the book located in the file " + file + " (got " + nbrLines + " line(s) expected 2 or 3)");
			}
			return null;
		} else {
			book.title = lines[0];

			String[] authors = lines[1].split(",");
			book.creator = authors[0];
			for (int i = 1; i < authors.length; i++) {
				book.authors.add(authors[i]);
			}

			book.permissions = library.getPermissions(book.creator);

			return book;
		}
	}

	private static String[] readFile(File file) {
		LinkedList<String> lines = new LinkedList<String>();
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				lines.add(line);
			}

			bufferedReader.close();
		} catch (IOException e) {
			Log.warning("Could not read the file " + file.toString());
			return null;
		}

		return lines.toArray(new String[lines.size()]);
	}

	private static String[] readFile(File file, int limit) {
		ArrayList<String> lines = new ArrayList<String>(limit);
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

			int lineNbr = 0;
			String line;
			while ((line = bufferedReader.readLine()) != null && lineNbr < limit) {
				lines.add(line);
				lineNbr++;
			}

			bufferedReader.close();
		} catch (IOException e) {
			Log.warning("Could not read the file " + file.toString());
			return null;
		}

		return lines.toArray(new String[lines.size()]);
	}

	public boolean inWriteMode(Player player) {
		return writeMode.contains(player.getName());
	}

	public void setWriteMode(Player player, boolean writeMode) {
		if (writeMode) {
			this.writeMode.add(player.getName());
		} else {
			this.writeMode.remove(player.getName());
		}
	}

	/**
	 * Display an edit to other players holding the book
	 * 
	 * @param editor
	 *            The player who performed the edit
	 * @param message
	 *            The message that will be displayed
	 */
	public void displayEdit(Player editor, String message) {
		if (authors.isEmpty())
			return;

		for (Player player : authorsViewing) {
			if (player != editor) {
				player.sendMessage(Books.COLOR + message);
			}
		}
	}

	/**
	 * Update the colour and formatting permissions, if needed. Note method will
	 * only update if the creator is is online.
	 * 
	 * @return True if the permissions have been updated, otherwise false
	 */
	private boolean permissionsUpdated() {
		Player player = Bukkit.getPlayerExact(creator);
		boolean updated = false;
		if (player != null) {
			boolean renderColours = player.hasPermission("books.colour");
			if (renderColours != permissions.renderColours) {
				Log.info(player.getName() + " changed renderColours to " + Boolean.toString(renderColours));
				permissions.renderColours = renderColours;

				updated = true;
			}
			boolean renderFormatting = player.hasPermission("books.format");
			if (renderFormatting != permissions.renderFormatting) {
				Log.info(player.getName() + " changed renderFormatting to " + Boolean.toString(renderFormatting));
				permissions.renderFormatting = renderFormatting;

				updated = true;
			}
		}

		return updated;
	}

	public void addViewer(Player player) {
		if (isAuthor(player)) {
			authorsViewing.add(player);
			currentBooks.put(player, this);
		}
	}

	public void removeViewer(Player player) {
		if (isAuthor(player)) {
			authorsViewing.remove(player);
			currentBooks.remove(player);
		}
	}

	public static Book getCurrent(Player player) {
		return currentBooks.get(player);
	}

	public static Book removeCurrent(Player player) {
		return currentBooks.remove(player);
	}
}
