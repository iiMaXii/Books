package com.djupfryst.books;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import com.djupfryst.books.general.InventoryManipulator;

/**
 * @author iiMaXii
 */
public class CraftCopyListener implements Listener {
	private Config config;

	private Recipe copyRecipe;
	private Recipe identityRecipe;

	public CraftCopyListener(Books books, Config config) {
		ShapelessRecipe copyRecipe = new ShapelessRecipe(new ItemStack(Material.BOOK, 1));
		copyRecipe.addIngredient(Material.BOOK);
		copyRecipe.addIngredient(Material.BOOK, -1);

		ShapelessRecipe identityRecipe = new ShapelessRecipe(new ItemStack(Material.BOOK, 1));
		identityRecipe.addIngredient(Material.BOOK, -1);

		books.getServer().addRecipe(copyRecipe);
		books.getServer().addRecipe(identityRecipe);

		this.config = config;

		this.copyRecipe = copyRecipe;
		this.identityRecipe = identityRecipe;
	}

	/**
	 * Set the result when copying and passing a book through the identity
	 * filter
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPrepareItemCraft(PrepareItemCraftEvent event) {
		if (shapelessRecipesAreEqual(event.getRecipe(), copyRecipe)) {
			if (event.getView() != null && !((Player) event.getView().getPlayer()).hasPermission("books.craftCopy") || !hasSufficientIngredients(event.getInventory().getMatrix(), event.getRecipe())
					|| !config.craftCopy()) {
				event.getInventory().setResult(null);
				return;
			}

			short uid = getUID(event.getInventory());

			event.getInventory().setResult(new ItemStack(Material.BOOK, 1, uid));
		} else if (shapelessRecipesAreEqual(event.getRecipe(), identityRecipe)) {
			if (event.getView() != null && !((Player) event.getView().getPlayer()).hasPermission("books.craftCopy") || !hasSufficientIngredients(event.getInventory().getMatrix(), event.getRecipe())
					|| !config.craftCopy()) {
				event.getInventory().setResult(null);
				return;
			}

			short uid = getUID(event.getInventory());

			event.getInventory().setResult(new ItemStack(Material.BOOK, 1, uid));
		}
	}

	/**
	 * Increases the non-empty book by one when using the copy recipe, because
	 * this recipe only gives one book
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCraftItem(CraftItemEvent event) {
		if (shapelessRecipesAreEqual(event.getRecipe(), copyRecipe)) {
			// Increase non-empty book amount (the result is only one book)
			ItemStack[] matrix = event.getInventory().getMatrix();
			for (ItemStack itemStack : matrix) {
				if (itemStack != null && itemStack.getType() == Material.BOOK && itemStack.getDurability() != 0) {
					itemStack.setAmount(itemStack.getAmount() + 1);
					break;
				}
			}
			event.getInventory().setMatrix(matrix);
		}
	}

	private boolean hasSufficientIngredients(ItemStack[] matrix, Recipe recipe) {
		boolean[] slotTouched = new boolean[matrix.length];
		int nbrTouchedSlots = 0;

		if (recipe instanceof ShapedRecipe) {
			ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;

			if (shapedRecipe.getShape().length == 1 && shapedRecipe.getShape()[0].length() == 3) {
				boolean allPaper = true;
				for (char character : shapedRecipe.getShape()[0].toCharArray()) {
					if (shapedRecipe.getIngredientMap().get(character).getType() != Material.PAPER)
						allPaper = false;
				}
				if (allPaper) {
					int count = 0;
					for (ItemStack item : matrix) {
						if (item != null && item.getType() == Material.PAPER && item.getAmount() >= 1)
							count++;
					}

					return count == 3;
				}
			}

			throw new RuntimeException("BooksListener.hasSufficientIngredients(): Unsupported recipe " + recipe.toString());
		} else if (recipe instanceof ShapelessRecipe) {
			ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;

			List<ItemStack> ingredients = shapelessRecipe.getIngredientList();

			for (ItemStack ingredient : ingredients) {
				if (ingredient.getDurability() == -1)
					continue;

				boolean foundEmptyBook = false;
				for (int i = 0; i < matrix.length; i++) {
					if (slotTouched[i])
						continue;

					if (matrix[i] != null && ingredient.getType() == matrix[i].getType() && ingredient.getAmount() <= matrix[i].getAmount() && ingredient.getDurability() == matrix[i].getDurability()) {
						slotTouched[i] = true;
						nbrTouchedSlots++;

						if (matrix[i].getDurability() == 0) {
							if (foundEmptyBook) {
								return false;
							}
							foundEmptyBook = true;
						}
					}
				}

			}

			// Now process all ingredients with wildcard (-1) as durability
			for (ItemStack ingredient : ingredients) {
				if (ingredient.getDurability() != -1)
					continue;

				for (int i = 0; i < matrix.length; i++) {
					if (slotTouched[i])
						continue;

					if (matrix[i] != null && ingredient.getType() == matrix[i].getType() && ingredient.getAmount() <= matrix[i].getAmount()) {
						slotTouched[i] = true;
						nbrTouchedSlots++;
					}
				}

			}
			return nbrTouchedSlots == ingredients.size();

		} else {
			throw new RuntimeException("BooksListener.hasSufficientIngredients(): Unsupported Recipe implementation " + recipe.getClass().getName());
		}
	}

	/**
	 * Stacking of books with same UID, otherwise swap the items
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		ItemStack itemInSlot = event.getCurrentItem();

		if (event.isShiftClick() && itemInSlot.getType() == Material.BOOK) {
			if (event instanceof CraftItemEvent) {
				CraftItemEvent craftEvent = (CraftItemEvent) event;

				boolean hasInsufficientIngredients = false;
				PlayerInventory playerInventory = craftEvent.getWhoClicked().getInventory();
				Recipe recipe = craftEvent.getRecipe();
				while (InventoryManipulator.hasRoom(itemInSlot, playerInventory)) {
					ItemStack result = itemInSlot.clone();

					// Remove ingredients
					ItemStack[] matrix = craftEvent.getInventory().getMatrix();
					removeCraftingIngredients(matrix, recipe);
					craftEvent.getInventory().setMatrix(matrix);

					// Move the item
					InventoryManipulator.insert(result, playerInventory);

					if (!hasSufficientIngredients(craftEvent.getInventory().getMatrix(), recipe)) {
						if (hasSufficientIngredients(craftEvent.getInventory().getMatrix(), identityRecipe)) {
							// We still have some books left in crafting matrix,
							// let us apply the identity recipe
							craftEvent = new CraftItemEvent(identityRecipe, craftEvent.getView(), craftEvent.getSlotType(), craftEvent.getSlot(), craftEvent.isRightClick(), craftEvent.isShiftClick());
						} else {
							hasInsufficientIngredients = true;
							break;
						}
					}

					// Prepare new craft
					onPrepareItemCraft(new PrepareItemCraftEvent(craftEvent.getInventory(), null, false));

					// Craft new item
					onCraftItem(craftEvent);

				}

				if (hasInsufficientIngredients) {
					event.setCurrentItem(null);
					// itemInSlot.setType(Material.AIR);
				}

				event.setCancelled(true);
			}
		}
	}

	/**
	 * Allows us to compare CraftShapelessRecipe with ShapelessRecipe
	 * 
	 * @param recipe0
	 * @param recipe1
	 * @return
	 */
	private boolean shapelessRecipesAreEqual(Recipe recipe0, Recipe recipe1) {
		if (recipe0 != null && recipe1 != null && recipe0 instanceof ShapelessRecipe && recipe1 instanceof ShapelessRecipe) {
			ShapelessRecipe shapelessRecipe0 = (ShapelessRecipe) recipe0;
			ShapelessRecipe shapelessRecipe1 = (ShapelessRecipe) recipe1;

			if (shapelessRecipe0.getResult().equals(shapelessRecipe1.getResult())) {
				// Doesn't check for duplicates, but I'm lazy
				return shapelessRecipe0.getIngredientList().containsAll(shapelessRecipe1.getIngredientList());
			}
		}
		return false;
	}

	/**
	 * Get the UID of the book that is being copied
	 * 
	 * @param craftingInventory
	 *            The inventory where the crafting occures
	 * @return The books UID or 0 of not found
	 */
	private short getUID(CraftingInventory craftingInventory) {
		short uid = 0;
		for (ItemStack stack : craftingInventory.getMatrix()) {
			if (stack != null && stack.getType() == Material.BOOK && stack.getDurability() != 0) {
				uid = stack.getDurability();
			}
		}
		return uid;
	}

	/**
	 * Craft the item that is found on craft inventory
	 * 
	 * @param craftingInventory
	 *            The crafting inventory
	 * @return True if the ingredients where removed, or false if they where
	 *         insufficient
	 */
	private void removeCraftingIngredients(ItemStack[] matrix, Recipe recipe) {
		if (recipe == null)
			return;

		boolean[] slotTouched = new boolean[matrix.length];

		if (recipe instanceof ShapedRecipe) {
			ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;

			if (shapedRecipe.getShape().length == 1 && shapedRecipe.getShape()[0].length() == 3) {
				boolean allPaper = true;
				for (char character : shapedRecipe.getShape()[0].toCharArray()) {
					if (shapedRecipe.getIngredientMap().get(character).getType() != Material.PAPER)
						allPaper = false;
				}
				if (allPaper) {
					for (ItemStack item : matrix) {
						if (item != null && item.getType() == Material.PAPER && item.getAmount() >= 1)
							item.setAmount(item.getAmount() - 1);
					}
					return;
				}
			}

			throw new RuntimeException("BooksListener.removeCraftingIngredients(): Unsupported recipe " + recipe.toString());
		} else if (recipe instanceof ShapelessRecipe) {
			ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;

			for (ItemStack ingredient : shapelessRecipe.getIngredientList()) {
				if (ingredient.getDurability() == -1)
					continue;

				for (int i = 0; i < matrix.length; i++) {
					if (slotTouched[i])
						continue;

					if (matrix[i] != null && ingredient.getType() == matrix[i].getType() && ingredient.getAmount() <= matrix[i].getAmount() && ingredient.getDurability() == matrix[i].getDurability()) {
						matrix[i].setAmount(matrix[i].getAmount() - ingredient.getAmount());
						slotTouched[i] = true;
					}
				}

			}

			// Now process all ingredients with wildcard (-1) as durability
			for (ItemStack ingredient : shapelessRecipe.getIngredientList()) {
				if (ingredient.getDurability() != -1)
					continue;

				for (int i = 0; i < matrix.length; i++) {
					if (slotTouched[i])
						continue;

					if (matrix[i] != null && ingredient.getType() == matrix[i].getType() && ingredient.getAmount() <= matrix[i].getAmount()) {
						matrix[i].setAmount(matrix[i].getAmount() - ingredient.getAmount());
						slotTouched[i] = true;
					}
				}

			}

		} else {
			throw new RuntimeException("BooksListener.removeCraftingIngredients(): Unsupported Recipe implementation " + recipe.getClass().getName());
		}
	}
}
