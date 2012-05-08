package com.djupfryst.books;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.djupfryst.books.general.Confirmation;
import com.djupfryst.books.general.Doc;
import com.djupfryst.books.general.Log;
import com.djupfryst.books.general.MinecraftFont;
import com.djupfryst.books.general.StringUtils;
import com.djupfryst.books.library.Book;
import com.djupfryst.books.library.BookLoadException;
import com.djupfryst.books.library.Library;
import com.djupfryst.books.library.actions.Action;
import com.djupfryst.books.library.actions.AddAction;
import com.djupfryst.books.library.actions.RemoveAction;
import com.djupfryst.books.library.actions.ReplaceAction;
import com.djupfryst.books.library.actions.TitleChangeAction;

/**
 * @author iiMaXii
 */
public class BooksCommand implements CommandExecutor {
	private final static int REPLACE_EXTRA_CHARACTERS = 16;

	private Library library;

	private Confirmation clearConfirmation;

	public BooksCommand(Library library) {
		this.library = library;

		clearConfirmation = new Confirmation(20);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		BookReplaceCacher.clean();

		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (!player.hasPermission("books.write")) {
				Log.commandDenied(player, cmd, args);
				player.sendMessage(ChatColor.RED + "You are not authorized to perform this command.");
				return true;
			}

			Log.command(player, cmd, args);

			if (args.length == 0 || args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("--help")) {
				player.sendMessage(Books.COLOR + "Books help /" + label + " " + ((args.length == 0) ? "--help" : args[0]));
				player.sendMessage(ChatColor.GRAY + "To create a new book simply type " + ChatColor.GOLD + "/" + label + " <title>" + ChatColor.GRAY + ", while holding an");
				player.sendMessage(ChatColor.GRAY + "empty book. In order to add some content to it, you just");
				player.sendMessage(ChatColor.GRAY + "repeat the command with some text: " + ChatColor.GOLD + "/" + label + " <text>" + ChatColor.GRAY + ".");
				player.sendMessage(ChatColor.GOLD + "-h" + ChatColor.GRAY + ", " + ChatColor.GOLD + "--help" + ChatColor.GRAY + ": Show this help section");
				player.sendMessage(ChatColor.GOLD + "-t" + ChatColor.GRAY + ", " + ChatColor.GOLD + "--title" + ChatColor.GRAY + ": Change the title of a book");
				player.sendMessage(ChatColor.GOLD + "-r" + ChatColor.GRAY + ", " + ChatColor.GOLD + "--replace" + ChatColor.GRAY + ": Replace or remove a word or sentence");
				player.sendMessage(ChatColor.GOLD + "-w" + ChatColor.GRAY + ", " + ChatColor.GOLD + "--write" + ChatColor.GRAY + ": Instant book writing (allows you to leave out " + ChatColor.GOLD
						+ "/w" + ChatColor.GRAY + ")");
				player.sendMessage(ChatColor.GOLD + "--undo" + ChatColor.GRAY + ": Undo the last action");
				player.sendMessage(ChatColor.GOLD + "--redo" + ChatColor.GRAY + ": Redo the last undone action");
				player.sendMessage(ChatColor.GOLD + "--clear" + ChatColor.GRAY + ": Completely remove the contents of a book");
				if (player.hasPermission("books.addAuthor")) {
					player.sendMessage(ChatColor.GOLD + "--addauthor" + ChatColor.GRAY + ": Add an additional author");
					player.sendMessage(ChatColor.GOLD + "--removeauthor" + ChatColor.GRAY + ": Remove an author");
				}
				return true;
			}

			ItemStack itemStack = player.getItemInHand();
			if (itemStack.getType() != Material.BOOK) {
				player.sendMessage(ChatColor.RED + "You must be holding a book to execute this command.");
				return true;
			}

			Book book = null;
			short uid = itemStack.getDurability();
			if (uid != 0) {
				book = library.getBook(uid);
				if (book == null) {
					Log.warning("The player " + player.getName() + " tried to write to a book with the UID " + itemStack.getDurability()
							+ ", but no book is associated with this uid. Removing UID from book");
					uid = 0;
					itemStack.setDurability(uid);
				}
			}

			if (book == null && itemStack.getAmount() != 1) {
				player.sendMessage(ChatColor.RED + "You must be holding a single book to execute this command.");
				return true;
			} else if (book != null && !book.isAuthor(player)) {
				player.sendMessage(ChatColor.RED + "You must be an author of this book in order to do this.");
				return true;
			}

			if (book == null || args[0].equalsIgnoreCase("-t") || args[0].equalsIgnoreCase("--title")) {
				String title = StringUtils.merge(args, " ", (args[0].equalsIgnoreCase("-t") || args[0].equalsIgnoreCase("--title")) ? 1 : 0);

				if (book == null) {
					if (title.equals("")) {
						player.sendMessage(ChatColor.GOLD + "/" + label + " <title>");
					} else {
						book = new Book(player, title);
						book.setWriteMode(player, true);
						itemStack.setDurability(library.addBook(book));
						player.sendMessage(ChatColor.GRAY + "You have created a book.");
					}
				} else {
					if (title.equals("")) {
						player.sendMessage(ChatColor.GOLD + "/" + label + " " + args[0] + " <title>");
					} else {
						book.setTitle(title);
						player.sendMessage(ChatColor.GRAY + "The title has been changed.");
					}
				}
			} else if (args[0].equalsIgnoreCase("-w") || args[0].equalsIgnoreCase("--write")) {
				if (book.inWriteMode(player)) {
					book.setWriteMode(player, false);
					player.sendMessage(ChatColor.GRAY + "Write mode disabled.");
				} else {
					book.setWriteMode(player, true);
					player.sendMessage(ChatColor.GRAY + "Write mode enabled.");
				}
			} else if (args[0].equalsIgnoreCase("--clear")) {
				if (clearConfirmation.isConfirming(player)) {
					BookReplaceCacher.remove(book);
					library.removeBook(book);
					player.getItemInHand().setDurability((short) 0);
					player.sendMessage(ChatColor.GRAY + "The book has been removed.");
				} else {
					player.sendMessage(ChatColor.RED + "You are about to erase the contents of this book, please");
					player.sendMessage(ChatColor.RED + "repeat the command to confirm. This action cannot be undone.");
					clearConfirmation.add(player);
				}
			} else if (args[0].equalsIgnoreCase("-r") || args[0].equalsIgnoreCase("--replace")) {
				if (args.length == 1) {
					player.sendMessage(Books.COLOR + "Books help (Page 1 of 1) /" + label + " " + args[0]);
					player.sendMessage(ChatColor.GRAY + "Replace a word or a sentence. A confirmation screen will");
					player.sendMessage(ChatColor.GRAY + "always appear before any replacement occurs.");
					player.sendMessage(Books.COLOR + "Examples");
					player.sendMessage(ChatColor.GOLD + "/w --replace foo bar " + ChatColor.GRAY + "Replace the word " + ChatColor.WHITE + "foo " + ChatColor.GRAY + "with " + ChatColor.WHITE + "bar"
							+ ChatColor.GRAY + ".");
					player.sendMessage(ChatColor.GOLD + "/w --replace foo " + ChatColor.GRAY + "Remove the word " + ChatColor.WHITE + "foo" + ChatColor.GRAY + ".");
					player.sendMessage(ChatColor.GOLD + "/w --replace \"this sentence\" \"other sentence\" " + ChatColor.GRAY + "Replace " + ChatColor.WHITE + "this");
					player.sendMessage("sentence" + ChatColor.GRAY + " with " + ChatColor.WHITE + "other sentence" + ChatColor.GRAY + ".");
					player.sendMessage(ChatColor.GOLD + "/w --replace 2 " + ChatColor.GRAY + "Display the the second result page.");
				} else {
					// See if the player has previous searches
					ReplaceCache replaceCache = BookReplaceCacher.get(player);

					if (replaceCache == null || args.length != 2 || !args[1].matches("^\\d+$")) {
						String[] arguments = StringUtils.getArguments(StringUtils.merge(args, " ", 1));

						if (arguments == null) {
							player.sendMessage(ChatColor.RED + "Syntax Error");
							player.sendMessage(ChatColor.RED + "Missing end quote (\"). Please note that escaped quotes (\\\"), is not counted as quotation marks.");
							return true;
						} else if (arguments.length > 2) {
							player.sendMessage(ChatColor.RED + "Size Error");
							player.sendMessage(ChatColor.RED + "More than 2 arguments was found. You may use quotation mark (e.g. \"space bar allowed here\") to include whitespace in an argument.");
							return true;
						} else if (arguments.length != 0 && arguments[0].equals("")) {
							player.sendMessage(ChatColor.RED + "Argument Error");
							player.sendMessage(ChatColor.RED + "First argument cannot be an empty string.");
							return true;
						} else {
							try {
								String search = arguments[0];
								String replace;
								if (arguments.length == 1) {
									replace = "";
								} else {
									replace = arguments[1];
								}

								String content = book.getContent();

								int index = 0;
								ArrayList<ReplaceMatch> matches = new ArrayList<ReplaceMatch>();
								while (index < content.length()) {
									int matchIndex = content.indexOf(search, index);

									if (matchIndex == -1)
										break;

									boolean beforeAtEnd = false;
									boolean afterAtEnd = false;

									int beforeStartOffset = matchIndex - REPLACE_EXTRA_CHARACTERS;
									if (beforeStartOffset <= 0) {
										beforeStartOffset = 0;
										beforeAtEnd = true;
									}
									String before = content.substring(beforeStartOffset, matchIndex);

									int afterEndOffset = matchIndex + search.length() + REPLACE_EXTRA_CHARACTERS;
									afterEndOffset = (afterEndOffset > content.length()) ? content.length() : afterEndOffset;
									if (afterEndOffset >= content.length()) {
										afterEndOffset = content.length();
										afterAtEnd = true;
									}
									String after = content.substring(matchIndex + search.length(), afterEndOffset);

									matches.add(new ReplaceMatch(matchIndex, before, after, beforeAtEnd, afterAtEnd));

									index = matchIndex + 1;
								}

								if (matches.size() != 0) {
									replaceCache = new ReplaceCache(uid, search, replace, matches);
									BookReplaceCacher.put(player, replaceCache);
								}
							} catch (BookLoadException e) {
								player.sendMessage(ChatColor.RED + "An internal error occured, please contact an administrator.");
							}
						}
					}

					// Now show it
					if (replaceCache == null || replaceCache.matches.size() == 0) {
						player.sendMessage(ChatColor.RED + "No matches where found.");
					} else {
						String aboutReplace;
						if (replaceCache.replace.equals("")) {
							aboutReplace = ChatColor.GRAY + "Remove " + ChatColor.WHITE + replaceCache.search + ChatColor.GRAY + ".";
						} else {
							aboutReplace = ChatColor.GRAY + "Replace " + ChatColor.WHITE + replaceCache.search + ChatColor.GRAY + " with " + ChatColor.WHITE + replaceCache.replace + ChatColor.GRAY
									+ ".";
						}

						if (replaceCache.matches.size() == 1) {
							player.sendMessage(Books.COLOR + "Found match");
							player.sendMessage(aboutReplace);
							ReplaceMatch match = replaceCache.matches.get(0);

							player.sendMessage(ChatColor.WHITE + "#0" + ": " + ChatColor.GRAY + ((match.beforeAtEnd) ? "" : "...") + match.before + Books.COLOR + ChatColor.UNDERLINE
									+ replaceCache.search + ChatColor.RESET + ChatColor.GRAY + match.after + ((match.afterAtEnd) ? "" : "..."));

							player.sendMessage(Books.COLOR + "Please confirm using /w --confirm");
						} else {
							int descriptionWidth = MinecraftFont.getWidth(aboutReplace);
							int nbrOfLines = descriptionWidth / MinecraftFont.CHAT_WINDOW_WIDTH + ((descriptionWidth % MinecraftFont.CHAT_WINDOW_WIDTH == 0) ? 0 : 1);

							int pageStartLine;
							int pageEndLine;

							if (replaceCache.matches.size() + nbrOfLines + 1 < 10) {
								player.sendMessage(Books.COLOR + "Found matches");
								player.sendMessage(aboutReplace);
								pageStartLine = 0;
								pageEndLine = replaceCache.matches.size();
							} else {
								int page = (args.length == 1) ? 0 : Doc.parsePage(args[1]);
								int pages = Doc.calculateNumberOfPages(replaceCache.matches.size() + nbrOfLines, 8);

								player.sendMessage(Books.COLOR + "Found matches (Page " + page + " of " + pages + ") /" + label + " " + args[0] + " <page>");

								int[] interval = Doc.calculateInterval(replaceCache.matches.size() + nbrOfLines, page, 8);
								if (interval == null) {
									return true;
								}

								pageStartLine = interval[0] - nbrOfLines;
								pageEndLine = interval[1] - nbrOfLines;

								if (pageStartLine < 0) {
									player.sendMessage(aboutReplace);
									pageStartLine = 0;
								}
							}

							// Display the result
							for (int i = pageStartLine; i < pageEndLine; i++) {
								ReplaceMatch match = replaceCache.matches.get(i);

								player.sendMessage(ChatColor.WHITE + "#" + i + ": " + ChatColor.GRAY + ((match.beforeAtEnd) ? "" : "...") + match.before + Books.COLOR + ChatColor.UNDERLINE
										+ replaceCache.search + ChatColor.RESET + ChatColor.GRAY + match.after + (match.afterAtEnd ? "" : "..."));
							}
							player.sendMessage(Books.COLOR + "Confirm using /w --confirm <#|all> (e.g. /w --confirm 0-2 7)");
						}
					}
				}

			} else if (args[0].equalsIgnoreCase("-c") || args[0].equalsIgnoreCase("--confirm")) {
				ReplaceCache replaceCache = BookReplaceCacher.get(player);
				if (replaceCache == null) {
					player.sendMessage(ChatColor.RED + "There is nothing to confirm.");
				} else if (args.length == 1 && replaceCache.matches.size() != 1) {
					player.sendMessage(ChatColor.GOLD + "/w " + args[0] + " <#|all>");
				} else {
					HashSet<Integer> confirmedIndexes = new HashSet<Integer>();
					if (replaceCache.matches.size() == 1 || args[1].equalsIgnoreCase("all")) {
						for (int i = 0; i < replaceCache.matches.size(); i++) {
							confirmedIndexes.add(i);
						}
					} else {
						for (int i = 1; i < args.length; i++) {
							if (args[i].matches("^\\d+$")) {
								int index = Integer.parseInt(args[i]);

								if (index >= replaceCache.matches.size()) {
									player.sendMessage(ChatColor.RED + "The integer " + index + " is outside of the matches boundaries.");
									return true;
								}

								confirmedIndexes.add(index);
							} else if (args[i].matches("^\\d+-\\d+$")) {
								String[] nums = args[i].split("-");
								for (int j = Integer.parseInt(nums[0]); j <= Integer.parseInt(nums[1]); j++) {

									if (j >= replaceCache.matches.size()) {
										player.sendMessage(ChatColor.RED + "The interval " + args[i] + " is outside of the matches boundaries.");
										return true;
									}

									confirmedIndexes.add(j);
								}
							} else {
								player.sendMessage(ChatColor.RED + "Unable to parse \"" + args[i] + "\".");
								return true;
							}
						}
					}

					ArrayList<Integer> confirmedIndexesList = new ArrayList<Integer>(confirmedIndexes);

					// Make sure we have the indexes in decreasing
					// order or else the replace actions will fail
					Collections.sort(confirmedIndexesList);
					Collections.reverse(confirmedIndexesList);

					try {
						int lastOffset = Integer.MAX_VALUE;
						int failedReplacements = 0;
						for (int index : confirmedIndexesList) {
							ReplaceMatch replaceMatch = replaceCache.matches.get(index);

							// Make sure the a part of this search hasn't been
							// replaced
							if (replaceMatch.offset + replaceCache.search.length() > lastOffset) {
								player.sendMessage(ChatColor.RED + "Match #" + index + " could not be replaced, a part of it was already repalced by a subsequent match.");
								failedReplacements++;
								continue;
							} else {
								lastOffset = replaceMatch.offset;
							}

							book.smartReplace(replaceMatch.offset, replaceMatch.offset + replaceCache.search.length(), replaceCache.replace);

						}

						char s;
						if (confirmedIndexesList.size() == 1)
							s = '\0';
						else
							s = 's';

						String description = "Replaced " + (confirmedIndexesList.size() - failedReplacements) + " occurence" + s + ".";
						player.sendMessage(ChatColor.GRAY + description);
						book.displayEdit(player, player.getName() + " executed --replace. " + description);
					} catch (BookLoadException e) {
						player.sendMessage(ChatColor.RED + "An internal error occured, please contact an administrator.");
					} finally {
						// Make sure user can't use same search to replace again
						BookReplaceCacher.remove(book);
					}
				}
			} else if (args[0].equalsIgnoreCase("--undo")) {
				try {
					Action action = book.undo();

					if (action == null) {
						player.sendMessage(ChatColor.RED + "There is no recorded action that can be undone.");
					} else {
						// Make sure user can't replace
						BookReplaceCacher.remove(book);

						String description = userFriendlyActionDescription(action);
						player.sendMessage(ChatColor.GRAY + description);
						book.displayEdit(player, player.getName() + " executed --undo. " + description);
					}
				} catch (BookLoadException e) {
					player.sendMessage(ChatColor.RED + "An internal error occured, please contact an administrator.");
				}
			} else if (args[0].equalsIgnoreCase("--redo")) {
				try {
					Action action = book.redo();

					if (action == null) {
						player.sendMessage(ChatColor.RED + "There is no recorded (undone) action that can be redone.");
					} else {
						// Make sure user can't replace
						BookReplaceCacher.remove(book);
						String description = userFriendlyActionDescription(action);
						player.sendMessage(ChatColor.GRAY + description);
						book.displayEdit(player, player.getName() + " executed --redo. " + description);
					}
				} catch (BookLoadException e) {
					player.sendMessage(ChatColor.RED + "An internal error occured, please contact an administrator.");
				}
			} else if (args[0].equalsIgnoreCase("--addauthor") && player.hasPermission("books.addAuthor")) {
				if (!book.isCreator(player)) {
					player.sendMessage(ChatColor.RED + "Only the creator can add authors to a book.");
				} else if (args.length < 2) {
					player.sendMessage(label + " --addauthor <player>");
				} else {
					Player mentionedPlayer = Bukkit.getPlayer(args[1]);

					if (book.isAuthor(args[1])) {
						player.sendMessage(ChatColor.RED + "That player is already an author.");
					} else if (mentionedPlayer == null) {
						if (Bukkit.getOfflinePlayer(args[1]).hasPlayedBefore()) {
							player.sendMessage(ChatColor.RED + "The player has to be online.");
						} else {
							player.sendMessage(ChatColor.RED + "No player was found.");
						}
					} else {
						book.addAuthor(mentionedPlayer);
						player.sendMessage(ChatColor.GRAY + "The player " + ChatColor.WHITE + mentionedPlayer.getName() + ChatColor.GRAY + " has been added as an author.");
					}
				}
			} else if (args[0].equalsIgnoreCase("--removeauthor")) {
				if (!book.isCreator(player)) {
					player.sendMessage(ChatColor.RED + "Only the creator can add authors to a book.");
				} else if (args.length < 2) {
					player.sendMessage(label + " --removeauthor <player>");
				} else {
					OfflinePlayer mentionedPlayer = Bukkit.getOfflinePlayer(args[1]);
					if (player == mentionedPlayer) {
						player.sendMessage(ChatColor.RED + "You cannot remove yourself.");
					} else if (!book.isAuthor(mentionedPlayer)) {
						player.sendMessage(ChatColor.RED + "The mentioned player is not an author.");
					} else {
						book.removeAuthor(mentionedPlayer);
						player.sendMessage(ChatColor.GRAY + "The player " + ChatColor.WHITE + mentionedPlayer.getName() + " has been added as an author.");
					}
				}
			} else {
				// Make sure user can't replace
				BookReplaceCacher.remove(book);

				String text = StringUtils.merge(args, " ");

				try {
					book.append(text);

					player.sendMessage(ChatColor.GRAY + "Wrote \"" + text + "\"");
					book.displayEdit(player, player.getName() + " wrote \"" + text + "\"");
				} catch (BookLoadException e) {
					player.sendMessage(ChatColor.RED + "An internal error occured, please contact an administrator.");
				}

			}

		} else {
			sender.sendMessage("This command is not avalible from the console.");
		}

		return true;
	}

	private String userFriendlyActionDescription(Action action) {
		if (action instanceof AddAction) {
			AddAction addAction = (AddAction) action;

			char s;
			if (addAction.getNumberOfCharactersAdded() == 1)
				s = '\0';
			else
				s = 's';

			return addAction.getNumberOfCharactersAdded() + " character" + s + " has been removed.";
		} else if (action instanceof RemoveAction) {
			RemoveAction removeAction = (RemoveAction) action;

			char s;
			if (removeAction.getRemovedString().length() == 1)
				s = '\0';
			else
				s = 's';

			return removeAction.getRemovedString().length() + " character" + s + " has been restored.";
		} else if (action instanceof ReplaceAction) {
			ReplaceAction replaceAction = (ReplaceAction) action;

			char s;
			if (replaceAction.getReplacedString().length() == 1)
				s = '\0';
			else
				s = 's';

			return (replaceAction.getEnd() - replaceAction.getStart()) + " character" + s + " have been replaced by " + replaceAction.getReplacedString().length() + " at index "
					+ replaceAction.getStart() + ".";
		} else if (action instanceof TitleChangeAction) {
			// TitleChangeAction titleChangeAction = (TitleChangeAction) action;

			return "The title has been changed.";
		}
		return null;
	}
}
