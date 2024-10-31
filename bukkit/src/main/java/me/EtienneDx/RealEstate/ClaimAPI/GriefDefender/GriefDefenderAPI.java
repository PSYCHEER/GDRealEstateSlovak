package me.EtienneDx.RealEstate.ClaimAPI.GriefDefender;

import java.util.HashSet;
import java.util.UUID;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.User;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.lib.geantyref.TypeToken;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import me.EtienneDx.RealEstate.RealEstate;
import me.EtienneDx.RealEstate.ClaimAPI.IClaim;
import me.EtienneDx.RealEstate.ClaimAPI.IClaimAPI;
import me.EtienneDx.RealEstate.ClaimAPI.IPlayerData;
import me.EtienneDx.RealEstate.Transactions.BoughtTransaction;
import me.EtienneDx.RealEstate.Transactions.Transaction;

public class GriefDefenderAPI implements IClaimAPI{

    private final Option<Integer> BUYER_PURCHASE_LIMIT;
    private final Option<Integer> BUYER_LEASE_LIMIT;
    private final Option<Integer> BUYER_RENT_LIMIT;
    private final Option<Integer> OWNER_LEASE_LIMIT;
    private final Option<Integer> OWNER_RENT_LIMIT;
    private final Option<Integer> OWNER_SELL_LIMIT;

    public GriefDefenderAPI() {
        BUYER_LEASE_LIMIT = Option.builder(Integer.class)
                .id("realestate:buyer-lease-limit")
                .name("realestate:buyer-lease-limit")
                .defaultValue(-1)
                .build();
        GriefDefender.getRegistry().getRegistryModuleFor(Option.class).get().registerCustomType(BUYER_LEASE_LIMIT);
        RealEstate.instance.getLogger().info("Registered RealEstate option '" + BUYER_LEASE_LIMIT.getName() + "'.");

        BUYER_RENT_LIMIT = Option.builder(Integer.class)
                .id("realestate:buyer-rent-limit")
                .name("realestate:buyer-rent-limit")
                .defaultValue(-1)
                .build();
        GriefDefender.getRegistry().getRegistryModuleFor(Option.class).get().registerCustomType(BUYER_RENT_LIMIT);
        RealEstate.instance.getLogger().info("Registered RealEstate option '" + BUYER_RENT_LIMIT.getName() + "'.");

        BUYER_PURCHASE_LIMIT = Option.builder(Integer.class)
                .id("realestate:buyer-purchase-limit")
                .name("realestate:buyer-purchase-limit")
                .defaultValue(-1)
                .build();
        GriefDefender.getRegistry().getRegistryModuleFor(Option.class).get().registerCustomType(BUYER_PURCHASE_LIMIT);
        RealEstate.instance.getLogger().info("Registered RealEstate option '" + BUYER_PURCHASE_LIMIT.getName() + "'.");

        OWNER_SELL_LIMIT = Option.builder(Integer.class)
                .id("realestate:owner-sell-limit")
                .name("realestate:owner-sell-limit")
                .defaultValue(-1)
                .build();
        GriefDefender.getRegistry().getRegistryModuleFor(Option.class).get().registerCustomType(OWNER_SELL_LIMIT);
        RealEstate.instance.getLogger().info("Registered RealEstate option '" + OWNER_SELL_LIMIT.getName() + "'.");

        OWNER_LEASE_LIMIT = Option.builder(Integer.class)
                .id("realestate:owner-lease-limit")
                .name("realestate:owner-lease-limit")
                .defaultValue(-1)
                .build();
        GriefDefender.getRegistry().getRegistryModuleFor(Option.class).get().registerCustomType(OWNER_LEASE_LIMIT);
        RealEstate.instance.getLogger().info("Registered RealEstate option '" + OWNER_LEASE_LIMIT.getName() + "'.");

        OWNER_RENT_LIMIT = Option.builder(Integer.class)
                .id("realestate:owner-rent-limit")
                .name("realestate:owner-rent-limit")
                .defaultValue(-1)
                .build();
        GriefDefender.getRegistry().getRegistryModuleFor(Option.class).get().registerCustomType(OWNER_RENT_LIMIT);
        RealEstate.instance.getLogger().info("Registered RealEstate option '" + OWNER_RENT_LIMIT.getName() + "'.");
    }

    @Override
    public IClaim getClaimAt(Location location) {
        return new GDClaim(GriefDefender.getCore().getClaimAt(location));
    }

    @Override
    public void saveClaim(IClaim claim) {
        // GD auto saves
    }

    @Override
    public IPlayerData getPlayerData(UUID player) {
        return new GDPlayerData(GriefDefender.getCore().getPlayerData(Bukkit.getPlayer(player).getWorld().getUID(), player));
    }

    @Override
    public void changeClaimOwner(IClaim claim, UUID newOwner) {
        if(claim instanceof GDClaim) {
            ClaimResult res = ((GDClaim) claim).getClaim().transferOwner(newOwner);
            if(!res.successful()) {
                throw new RuntimeException(res.getResultType().toString());
            }
        }
    }

    @Override
    public void registerEvents() {
        new GDPermissionListener();
    }

    public Integer getBuyerLeaseLimit(UUID player) {
        final User user = GriefDefender.getCore().getUser(player);
        if (user == null) {
            return null;
        }
        return GriefDefender.getPermissionManager().getActiveOptionValue(TypeToken.get(Integer.class), BUYER_LEASE_LIMIT, user, null, new HashSet<>());
    }

    public Integer getBuyerPurchaseLimit(UUID player) {
        final User user = GriefDefender.getCore().getUser(player);
        if (user == null) {
            return null;
        }
        return GriefDefender.getPermissionManager().getActiveOptionValue(TypeToken.get(Integer.class), BUYER_PURCHASE_LIMIT, user, null);
    }

    public Integer getBuyerRentalLimit(UUID player) {
        final User user = GriefDefender.getCore().getUser(player);
        if (user == null) {
        	System.out.println("COULD NOT FIND USER!!");
            return null;
        }
        return GriefDefender.getPermissionManager().getActiveOptionValue(TypeToken.get(Integer.class), BUYER_RENT_LIMIT, user, null);
    }

    public Integer getOwnerLeaseLimit(UUID player) {
        final User user = GriefDefender.getCore().getUser(player);
        if (user == null) {
            return null;
        }
        return GriefDefender.getPermissionManager().getActiveOptionValue(TypeToken.get(Integer.class), OWNER_LEASE_LIMIT, user, null);
    }

    public Integer getOwnerRentLimit(UUID player) {
        final User user = GriefDefender.getCore().getUser(player);
        if (user == null) {
            return null;
        }
        return GriefDefender.getPermissionManager().getActiveOptionValue(TypeToken.get(Integer.class), OWNER_RENT_LIMIT, user, null);
    }

    public Integer getOwnerSellLimit(UUID player) {
        final User user = GriefDefender.getCore().getUser(player);
        if (user == null) {
            return null;
        }
        return GriefDefender.getPermissionManager().getActiveOptionValue(TypeToken.get(Integer.class), OWNER_SELL_LIMIT, user, null);
    }

    @Override
    public Transaction getTransaction(UUID claimUniqueId) {
        final Claim claim = GriefDefender.getCore().getClaim(claimUniqueId);
        if (claim == null) {
            return null;
        }
        return RealEstate.transactionsStore.getTransaction(new GDClaim(claim));
    }
}
