package com.djupfryst.books.general;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author iiMaXii
 */
public class InventoryManipulator {
	/**
	 * Check if the inventory has room for the specified stack of items, i.e.
	 * insert() will return true.
	 * 
	 * @param stack
	 *            The stack that we want to check
	 * @param inventory
	 *            The inventory we want to check for room
	 * @return True if the item stack will fit in the inventory, otherwise false
	 */
	public static boolean hasRoom(ItemStack itemStack, Inventory inventory) {
		return hasRoom(itemStack, inventory, 0, inventory.getSize());
	}

	/**
	 * Check if the inventory has room for the specified stack of items, i.e.
	 * insert() will return true.
	 * 
	 * @param stack
	 *            The stack that we want to check
	 * @param inventory
	 *            The inventory we want to check for room
	 * @param start
	 *            Where to start the insertion
	 * @return True if the item stack will fit in the inventory, otherwise false
	 */
	public static boolean hasRoom(ItemStack itemStack, Inventory inventory, int start) {
		return hasRoom(itemStack, inventory, start, inventory.getSize());
	}

	/**
	 * Check if the inventory has room for the specified stack of items, i.e.
	 * insert() will return true.
	 * 
	 * @param stack
	 *            The stack that we want to check
	 * @param inventory
	 *            The inventory we want to check for room
	 * @param start
	 *            Where to start the insertion
	 * @param upperLimit
	 *            Where to end the insertion, non-inclusive
	 * @return True if the item stack will fit in the inventory, otherwise false
	 */
	public static boolean hasRoom(ItemStack itemStack, Inventory inventory, int start, int upperLimit) {
		int amount = itemStack.getAmount();

		for (int currentSlot = start; currentSlot < upperLimit; currentSlot++) {
			ItemStack currentItem = inventory.getItem(currentSlot);
			if (currentItem == null) {
				amount -= itemStack.getMaxStackSize();
			} else if (currentItem.getType() == Material.BOOK && currentItem.getDurability() == itemStack.getDurability()) {
				amount -= itemStack.getMaxStackSize() - currentItem.getAmount();
			}

			if (amount <= 0)
				return true;
		}

		return false;
	}

	/**
	 * Insert a stack into an inventory, taking the item durability in
	 * consideration
	 * 
	 * @param stack
	 *            A stack of books
	 * @param inventory
	 *            The inventory to insert into
	 * @return True if all the books could be inserted, otherwise false
	 */
	public static boolean insert(ItemStack stack, Inventory inventory) {
		return insert(stack, inventory, 0, inventory.getSize());
	}

	/**
	 * Insert a stack into an inventory, taking the item durability in
	 * consideration
	 * 
	 * @param stack
	 *            A stack of books
	 * @param inventory
	 *            The inventory to insert into
	 * @param start
	 *            Where to start the insertion
	 * @return True if all the books could be inserted, otherwise false
	 */
	public static boolean insert(ItemStack stack, Inventory inventory, int start) {
		return insert(stack, inventory, start, inventory.getSize());
	}

	/**
	 * Insert a stack into an inventory, taking the items durability in
	 * consideration
	 * 
	 * @param stack
	 *            A stack to insert
	 * @param inventory
	 *            The inventory to insert into
	 * @param start
	 *            Where to start the insertion
	 * @param upperLimit
	 *            Where to end the insertion, non-inclusive
	 * @return True if all the books could be inserted, otherwise false
	 */
	public static boolean insert(ItemStack stack, Inventory inventory, int start, int upperLimit) {
		int firstEmptySlot = -1;
		int maxStackSize = stack.getMaxStackSize();

		for (int currentSlot = start; currentSlot < upperLimit; currentSlot++) {
			ItemStack currentItem = inventory.getItem(currentSlot);
			if (currentItem == null) {
				if (firstEmptySlot == -1)
					firstEmptySlot = currentSlot;
			} else if (currentItem.getType() == Material.BOOK && currentItem.getDurability() == stack.getDurability()) {
				int roomLeft = maxStackSize - currentItem.getAmount();
				if (roomLeft >= stack.getAmount()) {
					currentItem.setAmount(currentItem.getAmount() + stack.getAmount());
					stack.setAmount(0);
					break;
				} else {
					currentItem.setAmount(currentItem.getAmount() + roomLeft);
					stack.setAmount(stack.getAmount() - roomLeft);
				}
			}
		}

		if (stack.getAmount() > 0 && firstEmptySlot != -1) {
			inventory.setItem(firstEmptySlot, stack);
			stack.setAmount(0);
		}

		return stack.getAmount() <= 0;
	}
}
