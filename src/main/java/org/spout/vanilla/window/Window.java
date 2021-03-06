/*
 * This file is part of Vanilla.
 *
 * Copyright (c) 2011-2012, VanillaDev <http://www.spout.org/>
 * Vanilla is licensed under the SpoutDev License Version 1.
 *
 * Vanilla is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * Vanilla is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spout.vanilla.window;

import org.spout.api.inventory.InventoryBase;
import org.spout.api.inventory.InventoryViewer;
import org.spout.api.inventory.ItemStack;
import org.spout.api.inventory.special.InventoryBundle;
import org.spout.api.player.Player;

import org.spout.vanilla.controller.WindowOwner;
import org.spout.vanilla.controller.living.player.VanillaPlayer;
import org.spout.vanilla.protocol.msg.CloseWindowMessage;
import org.spout.vanilla.protocol.msg.OpenWindowMessage;
import org.spout.vanilla.util.InventoryUtil;
import org.spout.vanilla.util.ItemUtil;
import org.spout.vanilla.util.SlotIndexMap;

import static org.spout.vanilla.util.VanillaNetworkUtil.sendPacket;

public class Window implements InventoryViewer {
	private static final SlotIndexMap DEFAULT_SLOTS = new SlotIndexMap();
	protected final int id;
	protected final int instanceId;
	protected String title;
	protected final VanillaPlayer owner;
	protected InventoryBundle inventory;
	protected ItemStack itemOnCursor;
	protected SlotIndexMap slotIndexMap = DEFAULT_SLOTS;
	protected boolean isOpen = false;
	protected WindowOwner[] windowOwners;

	public Window(int id, String title, VanillaPlayer owner, WindowOwner... windowOwners) {
		this.id = id;
		this.title = title;
		this.owner = owner;
		this.instanceId = InventoryUtil.nextWindowId();
		this.windowOwners = windowOwners;
	}

	public void setInventory(InventoryBase... inventories) {
		if (this.inventory != null) {
			this.inventory.stopWatching();
			this.inventory.removeViewer(this);
		}
		this.inventory = new InventoryBundle(inventories);
	}

	public InventoryBundle getInventory() {
		return this.inventory;
	}

	public int getInventorySize() {
		return this.inventory.getSize();
	}

	public int getId() {
		return this.id;
	}

	public boolean hasCloseMessage() {
		return true;
	}

	public int getInstanceId() {
		return this.instanceId;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Player getPlayer() {
		return this.owner.getPlayer();
	}

	public VanillaPlayer getOwner() {
		return this.owner;
	}

	/**
	 * Gets whether this window is opened to the player
	 * @return True if open, False if not
	 */
	public boolean isOpen() {
		return this.isOpen;
	}

	/**
	 * Opens this window
	 * @return True if opening was possible, False if not
	 */
	public boolean open() {
		if (this.isOpen()) {
			return false;
		}
		sendPacket(this.getPlayer(), new OpenWindowMessage(this.getInstanceId(), this.getId(), this.getTitle(), getInventorySize()));
		this.isOpen = true;
		this.inventory.addViewer(this);
		this.inventory.startWatching();
		this.inventory.notifyViewers();
		for (WindowOwner owner : this.windowOwners) {
			owner.addViewer(this.getOwner(), this);
		}
		return true;
	}

	/**
	 * Closes this window
	 * @return True if closing was possible, False if not
	 */
	public boolean close() {
		if (!this.isOpen()) {
			return false;
		}
		this.isOpen = false;
		if (this.hasCloseMessage()) {
			sendPacket(this.getPlayer(), new CloseWindowMessage(this.getInstanceId()));
		}
		this.inventory.removeViewer(this);
		this.inventory.stopWatching();
		for (WindowOwner owner : this.windowOwners) {
			owner.removeViewer(this.getOwner());
		}
		this.dropItemOnCursor();
		return true;
	}

	public boolean hasItemOnCursor() {
		return this.itemOnCursor != null;
	}

	public ItemStack getItemOnCursor() {
		return this.itemOnCursor;
	}

	public void setItemOnCursor(ItemStack item) {
		this.itemOnCursor = item;
	}

	public void setSlotIndexMap(SlotIndexMap map) {
		this.slotIndexMap = map;
	}

	public SlotIndexMap getSlotIndexMap() {
		return this.slotIndexMap;
	}

	public boolean onClick(int clickedSlot, boolean rightClick, boolean shift) {
		boolean result;
		if (rightClick) {
			result = onRightClick(clickedSlot, shift);
		} else {
			result = onLeftClick(clickedSlot, shift);
		}
		return result;
	}

	public void dropItemOnCursor() {
		if (this.hasItemOnCursor()) {
			ItemUtil.dropItemNaturally(this.getOwner().getParent().getPosition(), this.getItemOnCursor());
			this.setItemOnCursor(null);
		}
	}

	/**
	 * Called when the player clicks outside the window
	 * @return True to notify that the operation was allowed
	 */
	public boolean onOutsideClick() {
		this.dropItemOnCursor();
		return true;
	}

	/**
	 * Called when the player left or right clicks on an item while holding shift
	 * @param clickedSlot
	 * @return True to notify that the operation was allowed
	 */
	public boolean onShiftClick(int clickedSlot) {
		return false; //TODO: Implement shift-transferring
	}

	/**
	 * Called when the player left-clicks on a slot without holding shift
	 * @param clickedSlot
	 * @return True to notify that the operation was allowed
	 */
	public boolean onLeftClick(int clickedSlot) {
		ItemStack clickedItem = this.inventory.getItem(clickedSlot);
		if (clickedItem == null) {
			if (this.hasItemOnCursor()) {
				// cursor > clicked item
				this.inventory.setItem(clickedSlot, this.getItemOnCursor());
				this.setItemOnCursor(null);
				return true;
			}

			return true;
		}

		if (!this.hasItemOnCursor()) {
			// clicked item > cursor
			this.setItemOnCursor(clickedItem);
			this.inventory.setItem(clickedSlot, null);
			return true;
		}

		// clicked item + cursor
		ItemStack cursorItem = this.getItemOnCursor();
		if (cursorItem.equalsIgnoreSize(clickedItem)) {
			// stack
			clickedItem.stack(cursorItem);
			this.inventory.setItem(clickedSlot, clickedItem);
			this.setItemOnCursor(cursorItem.getAmount() <= 0 ? null : cursorItem);
			return true;
		}

		// swap
		this.setItemOnCursor(clickedItem);
		this.inventory.setItem(clickedSlot, cursorItem);
		return true;
	}

	/**
	 * Called when the player right-clicks on a slot without holding shift
	 * @param clickedSlot
	 * @return True to notify that the operation was allowed
	 */
	public boolean onRightClick(int clickedSlot) {
		ItemStack clickedItem = this.inventory.getItem(clickedSlot);
		if (clickedItem == null) {
			if (this.hasItemOnCursor()) {
				// cursor > clicked item
				ItemStack cursorItem = this.getItemOnCursor();
				clickedItem = cursorItem.clone();
				clickedItem.setAmount(1);
				cursorItem.setAmount(cursorItem.getAmount() - clickedItem.getAmount());
				this.inventory.setItem(clickedSlot, clickedItem);
				this.setItemOnCursor(cursorItem.getAmount() <= 0 ? null : cursorItem);
				return true;
			}

			return true;
		}
		if (this.hasItemOnCursor()) {
			// clicked item + cursor
			ItemStack cursorItem = this.getItemOnCursor();
			if (!cursorItem.equalsIgnoreSize(clickedItem)) {
				// swap
				this.setItemOnCursor(clickedItem);
				this.inventory.setItem(clickedSlot, cursorItem);
				return true;
			}

			if (clickedItem.getAmount() >= clickedItem.getMaxStackSize()) {
				return false;
			}

			// transfer one item
			clickedItem.setAmount(clickedItem.getAmount() + 1);
			cursorItem.setAmount(cursorItem.getAmount() - 1);
			this.inventory.setItem(clickedSlot, clickedItem);
			this.setItemOnCursor(cursorItem.getAmount() <= 0 ? null : cursorItem);
			return true;
		}

		// 1/2 clicked item > cursor
		ItemStack newItem = clickedItem.clone();
		newItem.setAmount(newItem.getAmount() / 2);
		clickedItem.setAmount(clickedItem.getAmount() - newItem.getAmount());
		this.inventory.setItem(clickedSlot, newItem.getAmount() <= 0 ? null : newItem);
		this.setItemOnCursor(clickedItem.getAmount() <= 0 ? null : clickedItem);
		return true;
	}

	/**
	 * Called when the player right-clicks on a slot in this window
	 * @param clickedSlot
	 * @param shift whether shift was pressed
	 * @return True to notify that the operation was allowed
	 */
	public boolean onRightClick(int clickedSlot, boolean shift) {
		if (shift) {
			return this.onShiftClick(clickedSlot);
		}

		return this.onRightClick(clickedSlot);
	}

	/**
	 * Called when the player left-clicks on a slot in this window
	 * @param clickedSlot
	 * @param shift whether shift was pressed
	 * @return True to notify that the operation was allowed
	 */
	public boolean onLeftClick(int clickedSlot, boolean shift) {
		if (shift) {
			return this.onShiftClick(clickedSlot);
		}

		return this.onLeftClick(clickedSlot);
	}

	@Override
	public void onSlotSet(InventoryBase inventory, int slot, ItemStack item) {
		this.getPlayer().getNetworkSynchronizer().onSlotSet(inventory, slot, item);
	}

	@Override
	public void updateAll(InventoryBase inventory, ItemStack[] slots) {
		this.getPlayer().getNetworkSynchronizer().updateAll(inventory, slots);
	}
}
