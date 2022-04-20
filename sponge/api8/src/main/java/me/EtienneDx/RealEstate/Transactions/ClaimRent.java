package me.EtienneDx.RealEstate.Transactions;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;

import me.EtienneDx.RealEstate.Messages;
import me.EtienneDx.RealEstate.RealEstate;
import me.EtienneDx.RealEstate.Utils;
import me.EtienneDx.RealEstate.Events.ClaimRentEvent;
import me.EtienneDx.RealEstate.config.LegacyHexSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

public class ClaimRent extends BoughtTransaction
{
	LocalDateTime startDate = null;
	public int duration;
	public boolean autoRenew = false;
	public boolean buildTrust = true;
	public int periodCount = 0;
	public int maxPeriod;
	
	public ClaimRent(Map<String, Object> map)
	{
		super(map);
		if(map.get("startDate") != null)
			startDate = LocalDateTime.parse((String) map.get("startDate"), DateTimeFormatter.ISO_DATE_TIME);
		duration = (int)map.get("duration");
		autoRenew = (boolean) map.get("autoRenew");
		periodCount = (int) map.get("periodCount");
		maxPeriod = (int) map.get("maxPeriod");
		try {
			buildTrust = (boolean) map.get("buildTrust");
		}
		catch (Exception e) {
			buildTrust = true;
		}
	}
	
	public ClaimRent(Claim claim, ServerPlayer player, double price, ServerLocation sign, int duration, int rentPeriods, boolean buildTrust)
	{
		super(claim, player, price, sign);
		this.duration = duration;
		this.maxPeriod = RealEstate.instance.config.cfgEnableRentPeriod ? rentPeriods : 1;
		this.buildTrust = buildTrust;
	}

	@Override
	public boolean update()
	{
		if(buyer == null)
		{
			if(Utils.isBlockSign(sign.blockType()))
			{
				Sign s = Utils.getSign(sign);
				final List<Component> signData = new ArrayList<>();
				signData.add(0, LegacyHexSerializer.deserialize(Messages.getMessage(RealEstate.instance.config.cfgSignsHeader, false)));
				signData.add(1, LegacyHexSerializer.deserialize(RealEstate.instance.config.cfgReplaceRent).color(NamedTextColor.DARK_GREEN));
				String price_line = "";
				if(RealEstate.instance.config.cfgUseCurrencySymbol)
				{
					if(RealEstate.instance.config.cfgUseDecimalCurrency == false)
					{
						price_line = RealEstate.instance.config.cfgCurrencySymbol + " " + (int)Math.round(price);
					}
					else
					{
						price_line = RealEstate.instance.config.cfgCurrencySymbol + " " + price;
					}

				}
				else
				{
					if(RealEstate.instance.config.cfgUseDecimalCurrency == false)
					{
						price_line = (int)Math.round(price) + " " + RealEstate.instance.config.currencyNamePlural;
					}
					else
					{
						price_line = price + " " + RealEstate.instance.config.currencyNamePlural;
					}
				}
				String period = (maxPeriod > 1 ? maxPeriod + "x " : "") + Utils.getTime(duration, null, false);
				if(this.buildTrust) {
					signData.add(2, LegacyHexSerializer.deserialize(price_line));
					signData.add(3, LegacyHexSerializer.deserialize(period));
				} else {
					signData.add(2, LegacyHexSerializer.deserialize(RealEstate.instance.config.cfgContainerRentLine));
					signData.add(3, LegacyHexSerializer.deserialize(price_line + " - " + period));
				}
				Sponge.server().scheduler().submit(Task.builder().plugin(RealEstate.instance.pluginContainer).delay(Ticks.of(1)).execute(() -> {
				    s.offer(Keys.SIGN_LINES, signData);
				}).build());
			}
			else
			{
				return true;
			}
		}
		else
		{
			// we want to know how much time has gone by since startDate
			int days = Period.between(startDate.toLocalDate(), LocalDate.now()).getDays();
			Duration hours = Duration.between(startDate.toLocalTime(), LocalTime.now());
			if(hours.isNegative() && !hours.isZero())
	        {
	            hours = hours.plusHours(24);
	            days--;
	        }
			if(days >= duration)// we exceeded the time limit!
			{
				payRent();
			}
			else if(Utils.isBlockSign(sign.blockType()))
			{
				Sign s = Utils.getSign(sign);
				final List<Component> signData = new ArrayList<>();
				signData.add(0, LegacyHexSerializer.deserialize(RealEstate.instance.config.cfgReplaceOngoingRent).color(NamedTextColor.GOLD)); //Changed the header to "[Rented]" so that it won't waste space on the next line and allow the name of the player to show underneath.
				String playerName = Utils.getOfflinePlayer(buyer).name();
				if (playerName == null) {
				    playerName = "unknown";
				}
				signData.add(1, LegacyHexSerializer.deserialize(Utils.getSignString(playerName)));//remove "Rented by"
				signData.add(2, LegacyHexSerializer.deserialize("Time remaining : "));
				
				int daysLeft = duration - days - 1;// we need to remove the current day
				Duration timeRemaining = Duration.ofHours(24).minus(hours);
				
				signData.add(3, LegacyHexSerializer.deserialize(Utils.getTime(daysLeft, timeRemaining, false)));
				Sponge.server().scheduler().submit(Task.builder().plugin(RealEstate.instance.pluginContainer).delay(Ticks.of(1)).execute(() -> {
                    s.offer(Keys.SIGN_LINES, signData);
                }).build());
			}
		}
		return false;
		
	}

	private void unRent(boolean msgBuyer)
	{
		final Claim claim = GriefDefender.getCore().getClaimAt(sign);
		claim.removeUserTrust(buyer, TrustTypes.NONE);
		if(msgBuyer && Utils.getOfflinePlayer(buyer).isOnline() && RealEstate.instance.config.cfgMessageBuyer)
		{
                        String location = "[" + Utils.getWorldName(sign.world()) + ", X: " + sign.blockX() + ", Y: " + 
					sign.blockY() + ", Z: " + sign.blockZ() + "]";
			String claimType = claim.getParent() == null ?
					RealEstate.instance.messages.keywordClaim :
					RealEstate.instance.messages.keywordSubclaim;

			Messages.sendMessage(Sponge.server().player(buyer).orElse(null), RealEstate.instance.messages.msgInfoClaimInfoRentCancelled,
					claimType,
					location);
		}
		buyer = null;
		RealEstate.transactionsStore.saveData();
		update();
	}

	private void payRent()
	{
		if(buyer == null) return;

		User buyerPlayer = Utils.getOfflinePlayer(this.buyer);
		User seller = owner == null ? null : Utils.getOfflinePlayer(owner);
		
		String claimType = GriefDefender.getCore().getClaimAt(sign).getParent() == null ?
				RealEstate.instance.messages.keywordClaim :
				RealEstate.instance.messages.keywordSubclaim;
                String location = "[" + Utils.getWorldName(sign.world()) + ", X: " + sign.blockX() + ", Y: " + 
				sign.blockY() + ", Z: " + sign.blockZ() + "]";
		
		if((autoRenew || periodCount + 1 < maxPeriod) && Utils.makePayment(owner, this.buyer, price, false, false))
		{
			periodCount = (periodCount + 1) % maxPeriod;
			startDate = LocalDateTime.now();
			if(buyerPlayer.isOnline() && RealEstate.instance.config.cfgMessageBuyer)
			{
				Messages.sendMessage(buyerPlayer.player().get(), RealEstate.instance.messages.msgInfoClaimInfoRentPaymentBuyer,
						claimType,
						location,
						Utils.formatPrice(price));
			}
			else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
        	{
				RealEstate.ess.addMail(buyerPlayer, seller, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentPaymentBuyer,
						claimType,
						location,
						Utils.formatPrice(price)));
        	}
			
			if(seller != null)
			{
				if(seller.isOnline() && RealEstate.instance.config.cfgMessageOwner)
				{
					Messages.sendMessage(seller.player().get(), RealEstate.instance.messages.msgInfoClaimInfoRentPaymentOwner,
							buyerPlayer.name(),
							claimType,
							location,
							Utils.formatPrice(price));
				}
				else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	        	{
	        		RealEstate.ess.addMail(buyerPlayer, seller, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentPaymentOwner,
							buyerPlayer.name(),
							claimType,
							location,
							Utils.formatPrice(price)));
	        	}
			}
			
		}
		else if (autoRenew)
		{
			if(buyerPlayer.isOnline() && RealEstate.instance.config.cfgMessageBuyer)
			{
				Messages.sendMessage(buyerPlayer.player().get(), RealEstate.instance.messages.msgInfoClaimInfoRentPaymentBuyerCancelled,
						claimType,
						location,
						Utils.formatPrice(price));
			}
			else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
        	{
        		User u = Utils.getOfflinePlayer(this.buyer);
        		RealEstate.ess.addMail(buyerPlayer, seller, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentPaymentBuyerCancelled,
						claimType,
						location,
						Utils.formatPrice(price)));
        	}
			unRent(false);
			return;
		}
		else
		{
			unRent(true);
			return;
		}
		update();
		RealEstate.transactionsStore.saveData();
	}
	
	@Override
	public boolean tryCancelTransaction(ServerPlayer p, boolean force)
	{
		if(buyer != null)
		{
			if(p.hasPermission("realestate.admin") && force == true)
			{
				this.unRent(true);
				RealEstate.transactionsStore.cancelTransaction(this);
				return true;
			}
			else
			{
				final Claim claim = GriefDefender.getCore().getClaimAt(sign);
				if(p != null)
					Messages.sendMessage(p, RealEstate.instance.messages.msgErrorCantCancelAlreadyRented,
						claim.getParent() == null ?
							RealEstate.instance.messages.keywordClaim :
							RealEstate.instance.messages.keywordSubclaim
						);
	            return false;
			}
		}
		else
		{
			RealEstate.transactionsStore.cancelTransaction(this);
			return true;
		}
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
		
		if (owner != null && owner.equals(player.uniqueId()))
        {
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimAlreadyOwner, claimTypeDisplay);
            return;
        }
		if(claim.getParent() == null && owner != null && !owner.equals(claim.getOwnerUniqueId()))
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimNotRentedByOwner, claimTypeDisplay);
            RealEstate.transactionsStore.cancelTransaction(claim);
            return;
		}
		if(!player.hasPermission("realestate." + claimType + ".rent"))
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimNoRentPermission, claimTypeDisplay);
            return;
		}
		if(player.uniqueId().equals(buyer) || buyer != null)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimAlreadyRented, claimTypeDisplay);
            return;
		}
		
		final ClaimRentEvent event = new ClaimRentEvent(claim, player, price, buildTrust, autoRenew);
	    Sponge.eventManager().post(event);
	    if (event.isCancelled()) {
	        return;
	    }
	    price = event.getFinalPrice();
		if(Utils.makePayment(owner, player.uniqueId(), price, false, true))// if payment succeed
		{
			buyer = player.uniqueId();
			startDate = LocalDateTime.now();
			autoRenew = false;
			TrustType trustType = buildTrust ? TrustTypes.BUILDER : TrustTypes.CONTAINER;
			claim.addUserTrust(buyer, trustType);
			update();
			RealEstate.transactionsStore.saveData();
			
			RealEstate.instance.addLogEntry(
                    "[" + RealEstate.transactionsStore.dateFormat.format(RealEstate.transactionsStore.date) + "] " + player.name() + 
                    " has rented a " + claimType + " at " +
                    "[" + Utils.getWorldName(player.serverLocation().world()) + ", " +
                    "X: " + player.serverLocation().blockX() + ", " +
                    "Y: " + player.serverLocation().blockY() + ", " +
                    "Z: " + player.serverLocation().blockZ() + "] " +
                    "Price: " + price + " " + RealEstate.instance.config.currencyNamePlural);

			if(owner != null)
			{
				User seller = Utils.getOfflinePlayer(owner);
				String location = "[" + Utils.getWorldName(player.serverLocation().world()) + ", " + 
						"X: " + sign.blockX() + ", " + 
						"Y: " + sign.blockY() + ", " + 
						"Z: " + sign.blockZ() + "]";
			
				if(RealEstate.instance.config.cfgMessageOwner && seller.isOnline())
				{
					Messages.sendMessage(seller.player().get(), RealEstate.instance.messages.msgInfoClaimOwnerRented,
						player.name(),
						claimTypeDisplay,
						Utils.formatPrice(price),
						location);
				}
				else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	        	{
	        		RealEstate.ess.addMail(player, seller, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimOwnerRented,
						player.name(),
						claimTypeDisplay,
						Utils.formatPrice(price),
						location));
	        	}
			}
			
			Messages.sendMessage(player, RealEstate.instance.messages.msgInfoClaimBuyerRented,
				claimTypeDisplay,
				Utils.formatPrice(price));
			
			destroySign();
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
				RealEstate.instance.messages.keywordClaim :
				RealEstate.instance.messages.keywordSubclaim;
			String msg = Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentHeader) + "\n";
			if(buyer == null)
			{
				msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoGeneralRentNoBuyer,
						claimTypeDisplay,
						Utils.formatPrice(price),
						Utils.getTime(duration, null, true)) + "\n";
				if(maxPeriod > 1)
				{
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentMaxPeriod,
							maxPeriod + "") + "\n";
				}

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
			}
			else
			{
				int days = Period.between(startDate.toLocalDate(), LocalDate.now()).getDays();
				Duration hours = Duration.between(startDate.toLocalTime(), LocalTime.now());
				if(hours.isNegative() && !hours.isZero())
		        {
		            hours = hours.plusHours(24);
		            days--;
		        }
				int daysLeft = duration - days - 1;// we need to remove the current day
				Duration timeRemaining = Duration.ofHours(24).minus(hours);
				
				msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoGeneralRentBuyer,
						claimTypeDisplay,
						Utils.getOfflinePlayer(buyer).name(),
						Utils.formatPrice(price),
						Utils.getTime(daysLeft, timeRemaining, true),
						Utils.getTime(duration, null, true)) + "\n";
				
				if(maxPeriod > 1 && maxPeriod - periodCount > 0)
				{
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentRemainingPeriods,
							(maxPeriod - periodCount) + "") + "\n";
				}

				if((owner != null && owner.equals(player.uniqueId()) || buyer.equals(player.uniqueId())) && RealEstate.instance.config.cfgEnableAutoRenew)
				{
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentAutoRenew,
						autoRenew ? 
							RealEstate.instance.messages.keywordEnabled :
							RealEstate.instance.messages.keywordDisabled) + "\n";
				}
				if(claimType.equalsIgnoreCase("claim"))
				{
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoOwner,
							claim.getOwnerName()) + "\n";
				}
				else
				{
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoMainOwner,
							claim.getOwnerName()) + "\n";
				}
			}
			Messages.sendMessage(player, msg);
		}
		else
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimNoInfoPermission);
		}
	}

	@Override
	public void msgInfo(Subject cs)
	{
		final Claim claim = GriefDefender.getCore().getClaim(claimId);
        final ServerWorld world = Utils.getWorldByUniqueId(claim.getWorldUniqueId()).orElse(null);
		String location = "[" + Utils.getWorldName(world) + ", " +
		"X: " + claim.getLesserBoundaryCorner().getX() + ", " +
		"Y: " + claim.getLesserBoundaryCorner().getY() + ", " +
		"Z: " + claim.getLesserBoundaryCorner().getZ() + "]";

		Messages.sendMessage(cs, RealEstate.instance.messages.msgInfoClaimInfoRentOneline,
				claim.getArea() + "",
				location,
				Utils.formatPrice(price),
				Utils.getTime(duration, Duration.ZERO, false));
	}

}
