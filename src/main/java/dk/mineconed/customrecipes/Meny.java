package dk.mineconed.customrecipes;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Made by Mineconed
 *
 * Marks our menus so clicks can be handled.
 */
public final class Meny implements InventoryHolder {

    public enum Slags { LISTE, REDIGER, VIS }

    final Slags slags;
    final boolean admin;
    int side;
    Opskrift kladde;
    String gammeltId;
    Inventory inv;

    Meny(Slags slags, boolean admin) {
        this.slags = slags;
        this.admin = admin;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
