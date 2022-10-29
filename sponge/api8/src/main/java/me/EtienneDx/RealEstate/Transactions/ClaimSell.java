package me.EtienneDx.RealEstate.Transactions;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;

import me.EtienneDx.RealEstate.Messages;
import me.EtienneDx.RealEstate.RealEstate;
import me.EtienneDx.RealEstate.Utils;
import me.EtienneDx.RealEstate.Events.ClaimSellEvent;
import me.EtienneDx.RealEstate.config.LegacyHexSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClaimSell extends ClaimTransaction
{
	public ClaimSell(Claim claim, ServerPlayer player, double price, ServerLocation sign)
	{
		super(claim, player, price, sign);
	}
	
	public ClaimSell(Map<String, Object> map)
	{
		super(map);
	}

	@Override
	public boolean update()
	{
	    if(Utils.isBlockSign(sign.blockType()))
		{
		    Sign s = Utils.getSign(sign);
		    final List<Component> signData = new ArrayList<>(Collections.nCopies(4, Component.empty()));
			signData.add(0, LegacyHexSerializer.deserialize(Messages.getMessage(RealEstate.instance.config.cfgSignsHeader, false)));
			signData.add(1, LegacyHexSerializer.deserialize(Messages.getMessage(RealEstate.instance.config.cfgReplaceSell, false)));
			signData.add(2, LegacyHexSerializer.deserialize(owner != null ? Utils.getSignString(Utils.getOfflinePlayer(owner).name()) : "SERVER"));
			if(RealEstate.instance.config.cfgUseCurrencySymbol)
			{
				if(RealEstate.instance.config.cfgUseDecimalCurrency == false)
				{
					signData.add(3, LegacyHexSerializer.deserialize(RealEstate.instance.config.cfgCurrencySymbol + " " + (int)Math.round(price)));
				}
				else
				{
					signData.add(3, LegacyHexSerializer.deserialize(RealEstate.instance.config.cfgCurrencySymbol + " " + price));
				}
			}
			else
			{
				if(RealEstate.instance.config.cfgUseDecimalCurrency == false)
				{
					signData.add(3, LegacyHexSerializer.deserialize((int)Math.round(price) + " " + RealEstate.instance.config.currencyNamePlural));
				}
				else
				{
					signData.add(3, LegacyHexSerializer.deserialize(price + " " + RealEstate.instance.config.currencyNamePlural));
				}
			}
			Sponge.server().scheduler().submit(Task.builder().plugin(RealEstate.instance.pluginContainer).delay(Ticks.of(1)).execute(() -> {
                s.offer(Keys.SIGN_LINES, signData);
            }).build());
		}
		else
		{
			RealEstate.transactionsStore.cancelTransaction(this);
		}
		return false;
	}
	
	@Override
	public boolean tryCancelTransaction(ServerPlayer p, boolean force)
	{
		// nothing special here, this transaction can only be waiting for a buyer
		RealEstate.transactionsStore.cancelTransaction(this);
		return true;
	}

	@Override
	public void interact(ServerPlayer player)
	{
		final Claim claim = GriefDefender.getCore().getClaimAt(sign);// getting by id creates errors for subclaims
		if(claim == null || claim.isWilderness())
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimDoesNotExist);
            RealEstate.transactionsStore.cancelTransaction(claim);
            return;
		}
		String claimType = claim.getParent() == null ? "claim" : "subclaim";
                String claimTypeDisplay = claim.getParent() == null ? 
			RealEstate.instance.messages.keywordClaim : RealEstate.instance.messages.keywordSubclaim;
		
		if (player.uniqueId().equals(owner))
        {
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimAlreadyOwner, claimTypeDisplay);
            return;
        }
		if(claim.getParent() == null && owner != null && !owner.equals(claim.getOwnerUniqueId()))
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimNotSoldByOwner, claimTypeDisplay);
            RealEstate.transactionsStore.cancelTransaction(claim);
            return;
		}
		if(!player.hasPermission("realestate." + claimType + ".buy"))
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimNoBuyPermission, claimTypeDisplay);
            return;
		}
		// for real claims, you may need to have enough claim blocks in reserve to purchase it (if transferClaimBlocks is false)
		if(claimType.equalsIgnoreCase("claim") && !RealEstate.instance.config.cfgTransferClaimBlocks && 
				GriefDefender.getCore().getPlayerData(player.world().uniqueId(), player.uniqueId()).getRemainingClaimBlocks() < claim.getArea())
		{
			int remaining = GriefDefender.getCore().getPlayerData(player.world().uniqueId(), player.uniqueId()).getRemainingClaimBlocks();
			int area = claim.getArea();
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimNoClaimBlocks,
				area + "",
				remaining + "",
				(area - remaining) + "");
            return;			
		}
		// the player has the right to buy, let's make the payment
		final ClaimSellEvent event = new ClaimSellEvent(claim, player, price);
		Sponge.eventManager().post(event);
		if (event.isCancelled()) {
		    return;
		}
		price = event.getFinalPrice();
		if(Utils.makePayment(owner, player.uniqueId(), price, false, true))// if payment succeed
		{
			Utils.transferClaim(claim, player.uniqueId(), owner);
			// normally, this is always the case, so it's not necessary, but until I proven my point, here
			if(claim.getParent() != null || claim.getOwnerUniqueId().equals(player.uniqueId()))
			{
				String location = "[" + Utils.getWorldName(player.serverLocation().world()) + ", " +
					"X: " + player.serverLocation().blockX() + ", " +
					"Y: " + player.serverLocation().blockY() + ", " +
					"Z: " + player.serverLocation().blockZ() + "]";

				Messages.sendMessage(player, RealEstate.instance.messages.msgInfoClaimBuyerSold,
						claimTypeDisplay,
						Utils.formatPrice(price));
						
                RealEstate.instance.addLogEntry(
                        "[" + RealEstate.transactionsStore.dateFormat.format(RealEstate.transactionsStore.date) + "] " + player.name() + 
                        " has purchased a " + claimType + " at " +
                                "[" + Utils.getWorldName(player.serverLocation().world()) + ", " +
                                "X: " + player.serverLocation().blockX() + ", " +
                                "Y: " + player.serverLocation().blockY() + ", " +
                                "Z: " + player.serverLocation().blockZ() + "] " +
                                "Price: " + price + " " + RealEstate.instance.config.currencyNamePlural);
                
                if(RealEstate.instance.config.cfgMessageOwner && owner != null)
                {
                	User oldOwner = Utils.getOfflinePlayer(owner);
                	if(oldOwner.isOnline())
                	{
						Messages.sendMessage(oldOwner.player().get(), RealEstate.instance.messages.msgInfoClaimOwnerSold,
								player.name(),
								claimTypeDisplay,
								Utils.formatPrice(price),
								location);
                	}
                	else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
                	{
                		User u = Utils.getOfflinePlayer(owner);
                		RealEstate.ess.addMail(player, u, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimOwnerSold,
								player.name(),
								claimTypeDisplay,
								Utils.formatPrice(price),
								location));
                	}
                }
			}
            else
            {
				Messages.sendMessage(player, RealEstate.instance.messages.msgErrorUnexpected);
                return;
            }
			RealEstate.transactionsStore.cancelTransaction(claim);
		}
	}
	
	@Override
	public void preview(ServerPlayer player)
	{
		final Claim claim = GriefDefender.getCore().getClaimAt(sign);
		if(player.hasPermission("realestate.info"))
		{
			String claimType = claim.getParent() == null ? "claim" : "subclaim";
			String claimTypeDisplay = claim.getParent() == null ? 
				RealEstate.instance.messages.keywordClaim : RealEstate.instance.messages.keywordSubclaim;

			String msg = Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoSellHeader) + "\n";

			msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoSellGeneral,
					claimTypeDisplay,
					Utils.formatPrice(price)) + "\n";

			if(claimType.equalsIgnoreCase("claim"))
			{
				msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoOwner,
						claim.getOwnerName()) + "\n";
			}
			else
			{
				msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoMainOwner,
						claim.getOwnerName()) + "\n";
				msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoNote) + "\n";
			}
			Messages.sendMessage(player, msg);
		}
		else
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimNoInfoPermission);
		}
	}

	@Override
	public void setOwner(UUID newOwner)
	{
		this.owner = newOwner;
	}

	@Override
	public void msgInfo(Subject cs)
	{
		final Claim claim = GriefDefender.getCore().getClaim(claimId);
		if(claim == null) {
			tryCancelTransaction(null, true);
			return;
		}
		final ServerWorld world = Utils.getWorldByUniqueId(claim.getWorldUniqueId()).orElse(null);
		String location = "[" + Utils.getWorldName(world) + ", " +
		"X: " + claim.getLesserBoundaryCorner().getX() + ", " +
		"Y: " + claim.getLesserBoundaryCorner().getY() + ", " +
		"Z: " + claim.getLesserBoundaryCorner().getZ() + "]";

		Messages.sendMessage(cs, RealEstate.instance.messages.msgInfoClaimInfoSellOneline,
				claim.getArea() + "",
				location,
				Utils.formatPrice(price));
	}
}
