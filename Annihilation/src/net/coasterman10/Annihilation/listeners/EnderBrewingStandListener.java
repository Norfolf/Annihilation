package net.coasterman10.Annihilation.listeners;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R1.inventory.CraftInventoryBrewer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import net.coasterman10.Annihilation.AnnihilationTeam;
import net.coasterman10.Annihilation.PlayerMeta;
import net.minecraft.server.v1_7_R1.EntityHuman;
import net.minecraft.server.v1_7_R1.EntityPlayer;
import net.minecraft.server.v1_7_R1.TileEntityBrewingStand;

public class EnderBrewingStandListener implements Listener {
	private HashMap<AnnihilationTeam, Location> locations;
	private HashMap<String, VirtualBrewingStand> brewingStands;

	public EnderBrewingStandListener() {
		locations = new HashMap<AnnihilationTeam, Location>();
		brewingStands = new HashMap<String, VirtualBrewingStand>();
	}

	public void setBrewingStandLocation(AnnihilationTeam team, Location loc) {
		locations.put(team, loc);
	}

	@EventHandler
	public void onFurnaceOpen(PlayerInteractEvent e) {
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		
		Block b = e.getClickedBlock();
		if (b.getType() != Material.BREWING_STAND)
			return;

		Location loc = b.getLocation();
		Player player = e.getPlayer();
		AnnihilationTeam team = PlayerMeta.getMeta(player).getTeam();
		if (team == null || !locations.containsKey(team))
			return;
		
		e.setCancelled(true);
		if (locations.get(team).equals(loc)) {
			EntityPlayer handle = ((CraftPlayer) player).getHandle();
			handle.openBrewingStand(getBrewingStand(player));
			player.sendMessage(ChatColor.DARK_AQUA
					+ "This is your team's Ender Brewing Stand. Any items you brew here are safe from all other players.");
		}
	}

	@EventHandler
	public void onFurnaceBreak(BlockBreakEvent e) {
		if (locations.values().contains(e.getBlock().getLocation()))
			e.setCancelled(true);
	}

	private VirtualBrewingStand getBrewingStand(Player player) {
		if (!brewingStands.containsKey(player.getName())) {
			EntityPlayer handle = ((CraftPlayer) player).getHandle();
			brewingStands.put(player.getName(), new VirtualBrewingStand(handle));
		}
		return brewingStands.get(player.getName());
	}

	private class VirtualBrewingStand extends TileEntityBrewingStand {
		public VirtualBrewingStand(EntityHuman entity) {
			world = entity.world;
		}
		
		@Override
		public boolean a(EntityHuman entity) {
			return true;
		}

		@Override
		public int p() {
			return 0;
		}

		@Override
		public net.minecraft.server.v1_7_R1.Block q() {
			return net.minecraft.server.v1_7_R1.Blocks.BREWING_STAND;
		}

		@Override
		public void update() {

		}

		@Override
		public InventoryHolder getOwner() {
			return new InventoryHolder() {
				@Override
				public Inventory getInventory() {
					return new CraftInventoryBrewer(VirtualBrewingStand.this);
				}
			};
		}
	}
}
