package pw.ollie.enderbows;

import pw.ian.albkit.AlbPlugin;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class EnderBows extends AlbPlugin implements Listener {
    /**
     * The players who have recently shot an ender bow - used for cancelling
     * fall damage on landing
     */
    private final Set<UUID> current = new HashSet<UUID>();

    /**
     * The amount of fall damage to deal to a player using an ender bow
     */
    private int fallDamage;

    @Override
    public void onEnable() {
        register(this);

        int val = getConfig().getInt("fall-damage", -1);
        if (val == -1) {
            getConfig().set("fall-damage", "4");
            saveConfig();
            val = 4;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void bowShotByEntity(final EntityShootBowEvent event) {
        final Entity entity = event.getEntity();
        // We only care if a player is shooting the bow
        if (!(entity instanceof Player)) {
            return;
        }

        final Player player = (Player) entity;
        final PlayerInventory inventory = player.getInventory();
        final ItemStack bowStack = event.getBow();

        // Go through every inventory slot
        for (int i = 0; i < inventory.getSize(); i++) {
            // Get the ItemStack in the current slot
            final ItemStack stack = inventory.getItem(i);
            if (stack.equals(bowStack)) {
                // If it's the bow, get the next one, check if it's ender pearls
                final ItemStack next = inventory.getItem(i + 1);
                if (next.getData().getItemType() == Material.ENDER_PEARL) {
                    // If it is attach the player as a passenger to the arrow
                    event.getProjectile().setPassenger(player);
                    // Add the player to the list to stop them getting damaged
                    current.add(player.getUniqueId());

                    // Remove an ender pearl from the inventory
                    if (next.getAmount() == 1) {
                        inventory.setItem(i + 1, null);
                    } else {
                        next.setAmount(next.getAmount() - 1);
                    }
                }

                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void entityDamaged(final EntityDamageEvent event) {
        final Entity entity = event.getEntity();
        // We only care if a player is shooting the bow
        if (!(entity instanceof Player)) {
            return;
        }
        // And we only care about fall damage
        if (!(event.getCause() == DamageCause.FALL)) {
            return;
        }

        final Player player = (Player) entity;
        // They're taking fall damage from riding an arrow, so cancel damage
        if (current.contains(player.getUniqueId())) {
            event.setCancelled(true);
            current.remove(player.getUniqueId());
            // Deal the configured amount of fall damage
            player.setHealth(player.getHealth() - fallDamage);
        }
    }
}
