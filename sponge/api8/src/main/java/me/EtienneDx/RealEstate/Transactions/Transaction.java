package me.EtienneDx.RealEstate.Transactions;

import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.server.ServerLocation;

import java.util.UUID;

public interface Transaction
{
	public ServerLocation getHolder();
	public UUID getClaimId();
	public UUID getOwner();
	public void setOwner(UUID newOwner);
	public void interact(ServerPlayer player);
	public void preview(ServerPlayer player);
	public boolean update();
	public boolean tryCancelTransaction(ServerPlayer p);
	public boolean tryCancelTransaction(ServerPlayer p, boolean force);
	public void msgInfo(Subject cs);
}
