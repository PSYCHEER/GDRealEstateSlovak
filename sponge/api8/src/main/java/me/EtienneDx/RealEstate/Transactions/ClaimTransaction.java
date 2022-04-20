package me.EtienneDx.RealEstate.Transactions;

import java.util.Map;
import java.util.UUID;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import me.EtienneDx.RealEstate.Utils;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

public abstract class ClaimTransaction implements Transaction
{
	public UUID claimId;
	public UUID owner = null;
	public double price;
	public ServerLocation sign = null;
	
	public ClaimTransaction(Claim claim, Player player, double price, ServerLocation sign)
	{
		this.claimId = claim.getUniqueId();
		if (claim.isAdminClaim() || GriefDefender.getCore().getAdminUser().getUniqueId().equals(claim.getOwnerUniqueId())) {
		    this.owner = null;
		} else {
		    this.owner = player != null ? player.uniqueId() : null;
		}
		this.price = price;
		this.sign = sign;
	}
	
	public ClaimTransaction(Map<String, Object> map)
	{
		this.claimId = (UUID) map.get("claimId");
		if(map.get("owner") != null)
			this.owner = (UUID) map.get("owner");
		this.price = (double) map.get("price");
		if(map.get("signLocation") != null) {
		    final Claim claim = GriefDefender.getCore().getClaim(this.claimId);
		    if (claim != null) {
    		    final ServerWorld world = Utils.getWorldByUniqueId(claim.getWorldUniqueId()).orElse(null);
    		    if (world != null) {
        		    String pos = (String) map.get("signLocation");
        			this.sign = ServerLocation.of(world, Utils.posFromString(pos));
    		    }
		    }
		}
	}
	
	public ClaimTransaction()
	{
		
	}

	@Override
	public ServerLocation getHolder()
	{
		return sign;
	}

	@Override
	public UUID getOwner()
	{
		return owner;
	}
	
	@Override
	public boolean tryCancelTransaction(ServerPlayer p)
	{
		return this.tryCancelTransaction(p, false);
	}

	@Override
	public UUID getClaimId() {
	    return this.claimId;
	}
}
