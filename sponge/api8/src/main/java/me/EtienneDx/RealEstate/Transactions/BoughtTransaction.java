package me.EtienneDx.RealEstate.Transactions;

import java.util.Map;
import java.util.UUID;

import com.griefdefender.api.claim.Claim;

import me.EtienneDx.RealEstate.RealEstate;
import me.EtienneDx.RealEstate.Utils;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;

public abstract class BoughtTransaction extends ClaimTransaction
{
	public UUID buyer = null;
	public ExitOffer exitOffer = null;
	public boolean destroyedSign = false;
	
	public BoughtTransaction(Map<String, Object> map)
	{
		super(map);
		if(map.get("buyer") != null)
			buyer = UUID.fromString((String)map.get("buyer"));
		if(map.get("exitOffer") != null)
			exitOffer = (ExitOffer) map.get("exitOffer");
		if(map.get("destroyedSign") != null)// may be the case on upgrading from 0.0.1-SNAPSHOT
			destroyedSign = (boolean) map.get("destroyedSign");
	}
	
	public BoughtTransaction(Claim claim, ServerPlayer player, double price, ServerLocation sign)
	{
		super(claim, player, price, sign);
	}
	
	public void destroySign()
	{
		if((this instanceof ClaimRent && RealEstate.instance.config.cfgDestroyRentSigns) || 
				(this instanceof ClaimLease && RealEstate.instance.config.cfgDestroyLeaseSigns))
		{
			if(!destroyedSign && Utils.isBlockSign(getHolder().blockType()))
				Utils.dropBlockAsItem(getHolder());
			destroyedSign = true;
		}
	}
	
	public UUID getBuyer()
	{
		return buyer;
	}
	
	public void setOwner(UUID newOwner)
	{
		this.owner = newOwner;
	}
}
