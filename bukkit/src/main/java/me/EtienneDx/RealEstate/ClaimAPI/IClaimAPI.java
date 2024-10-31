package me.EtienneDx.RealEstate.ClaimAPI;

import java.util.UUID;

import org.bukkit.Location;

import me.EtienneDx.RealEstate.Transactions.Transaction;

public interface IClaimAPI
{
    public IClaim getClaimAt(Location location);

    public void saveClaim(IClaim claim);

    public IPlayerData getPlayerData(UUID player);

    public void changeClaimOwner(IClaim claim, UUID newOwner);

    public void registerEvents();

    public Integer getBuyerLeaseLimit(UUID player);

    public Integer getBuyerPurchaseLimit(UUID player);

    public Integer getBuyerRentalLimit(UUID player);

    public Integer getOwnerLeaseLimit(UUID player);

    public Integer getOwnerRentLimit(UUID player);

    public Integer getOwnerSellLimit(UUID player);

    public Transaction getTransaction(UUID claimUniqueId);
}
