package me.EtienneDx.RealEstate.Transactions;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.UUID;

public interface Transaction
{
	public Location<World> getHolder();
	public UUID getClaimId();
	public UUID getOwner();
	public void setOwner(UUID newOwner);
	public void interact(Player player);
	public void preview(Player player);
	public boolean update();
	public boolean tryCancelTransaction(Player p);
	public boolean tryCancelTransaction(Player p, boolean force);
	public void msgInfo(CommandSource cs);
}
