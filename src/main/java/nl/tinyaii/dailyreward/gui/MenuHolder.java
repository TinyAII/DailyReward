package nl.tinyaii.dailyreward.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 月历 GUI 标识。
 */
public class MenuHolder implements InventoryHolder {
    private final int page;
    private final int pages;

    public MenuHolder(int page, int pages) {
        this.page = page;
        this.pages = pages;
    }

    @Override
    public Inventory getInventory() { return null; }

    public int getPage() { return page; }
    public int getPages() { return pages; }
}
