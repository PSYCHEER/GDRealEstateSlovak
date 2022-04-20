package me.EtienneDx.RealEstate.Transactions;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
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
import com.griefdefender.lib.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

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
	
	public ClaimRent(Claim claim, Player player, double price, Location<World> sign, int duration, int rentPeriods, boolean buildTrust)
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
			if(Utils.isBlockSign(sign.getBlockType()))
			{
				Sign s = Utils.getSign(sign);
				final SignData signData = s.getOrCreate(SignData.class).orElse(null);
                if (signData == null) {
                    return false;
                }
				signData.addElement(0, LegacyHexSerializer.toText(Messages.getMessage(RealEstate.instance.config.cfgSignsHeader, false)));
				signData.addElement(1, LegacyHexSerializer.toText(NamedTextColor.DARK_GREEN + RealEstate.instance.config.cfgReplaceRent));
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
					signData.addElement(2, LegacyHexSerializer.toText(price_line));
					signData.addElement(3, LegacyHexSerializer.toText(period));
				} else {
					signData.addElement(2, LegacyHexSerializer.toText(RealEstate.instance.config.cfgContainerRentLine));
					signData.addElement(3, LegacyHexSerializer.toText(price_line + " - " + period));
				}
				Sponge.getScheduler().createTaskBuilder().execute(() -> {
				    s.offer(signData);
				}).delayTicks(1).submit(RealEstate.instance.pluginContainer);
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
			else if(Utils.isBlockSign(sign.getBlockType()))
			{
				Sign s = Utils.getSign(sign);
				final SignData signData = s.getOrCreate(SignData.class).orElse(null);
                if (signData == null) {
                    return false;
                }
				signData.addElement(0, LegacyHexSerializer.toText(NamedTextColor.GOLD + RealEstate.instance.config.cfgReplaceOngoingRent)); //Changed the header to "[Rented]" so that it won't waste space on the next line and allow the name of the player to show underneath.
				String playerName = Utils.getOfflinePlayer(buyer).getName();
				if (playerName == null) {
				    playerName = "unknown";
				}
				signData.addElement(1, LegacyHexSerializer.toText(Utils.getSignString(playerName)));//remove "Rented by"
				signData.addElement(2, LegacyHexSerializer.toText("Time remaining : "));
				
				int daysLeft = duration - days - 1;// we need to remove the current day
				Duration timeRemaining = Duration.ofHours(24).minus(hours);
				
				signData.addElement(3, LegacyHexSerializer.toText(Utils.getTime(daysLeft, timeRemaining, false)));
				Sponge.getScheduler().createTaskBuilder().execute(() -> {
                    s.offer(signData);
                }).delayTicks(1).submit(RealEstate.instance.pluginContainer);
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
                        String location = "[" + sign.getExtent().getName() + ", X: " + sign.getBlockX() + ", Y: " + 
					sign.getBlockY() + ", Z: " + sign.getBlockZ() + "]";
			String claimType = claim.getParent() == null ?
					RealEstate.instance.messages.keywordClaim :
					RealEstate.instance.messages.keywordSubclaim;

			Messages.sendMessage(Sponge.getServer().getPlayer(buyer).orElse(null), RealEstate.instance.messages.msgInfoClaimInfoRentCancelled,
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
                String location = "[" + sign.getExtent().getName() + ", X: " + sign.getBlockX() + ", Y: " + 
				sign.getBlockY() + ", Z: " + sign.getBlockZ() + "]";
		
		if((autoRenew || periodCount + 1 < maxPeriod) && Utils.makePayment(owner, this.buyer, price, false, false))
		{
			periodCount = (periodCount + 1) % maxPeriod;
			startDate = LocalDateTime.now();
			if(buyerPlayer.isOnline() && RealEstate.instance.config.cfgMessageBuyer)
			{
				Messages.sendMessage(buyerPlayer.getPlayer().get(), RealEstate.instance.messages.msgInfoClaimInfoRentPaymentBuyer,
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
					Messages.sendMessage(seller.getPlayer().get(), RealEstate.instance.messages.msgInfoClaimInfoRentPaymentOwner,
							buyerPlayer.getName(),
							claimType,
							location,
							Utils.formatPrice(price));
				}
				else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	        	{
	        		RealEstate.ess.addMail(buyerPlayer, seller, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentPaymentOwner,
							buyerPlayer.getName(),
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
				Messages.sendMessage(buyerPlayer.getPlayer().get(), RealEstate.instance.messages.msgInfoClaimInfoRentPaymentBuyerCancelled,
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
	public boolean tryCancelTransaction(Player p, boolean force)
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
	public void interact(Player player)
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
		
		if (owner != null && owner.equals(player.getUniqueId()))
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
		if(player.getUniqueId().equals(buyer) || buyer != null)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimAlreadyRented, claimTypeDisplay);
            return;
		}
		
		final ClaimRentEvent event = new ClaimRentEvent(claim, player, price, buildTrust, autoRenew);
	    Sponge.getEventManager().post(event);
	    if (event.isCancelled()) {
	        return;
	    }
	    price = event.getFinalPrice();
		if(Utils.makePayment(owner, player.getUniqueId(), price, false, true))// if payment succeed
		{
			buyer = player.getUniqueId();
			startDate = LocalDateTime.now();
			autoRenew = false;
			TrustType trustType = buildTrust ? TrustTypes.BUILDER : TrustTypes.CONTAINER;
			claim.addUserTrust(buyer, trustType);
			update();
			RealEstate.transactionsStore.saveData();
			
			RealEstate.instance.addLogEntry(
                    "[" + RealEstate.transactionsStore.dateFormat.format(RealEstate.transactionsStore.date) + "] " + player.getName() + 
                    " has rented a " + claimType + " at " +
                    "[" + player.getLocation().getExtent() + ", " +
                    "X: " + player.getLocation().getBlockX() + ", " +
                    "Y: " + player.getLocation().getBlockY() + ", " +
                    "Z: " + player.getLocation().getBlockZ() + "] " +
                    "Price: " + price + " " + RealEstate.instance.config.currencyNamePlural);

			if(owner != null)
			{
				User seller = Utils.getOfflinePlayer(owner);
				String location = "[" + sign.getExtent().getName() + ", " + 
						"X: " + sign.getBlockX() + ", " + 
						"Y: " + sign.getBlockY() + ", " + 
						"Z: " + sign.getBlockZ() + "]";
			
				if(RealEstate.instance.config.cfgMessageOwner && seller.isOnline())
				{
					Messages.sendMessage(seller.getPlayer().get(), RealEstate.instance.messages.msgInfoClaimOwnerRented,
						player.getName(),
						claimTypeDisplay,
						Utils.formatPrice(price),
						location);
				}
				else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	        	{
	        		RealEstate.ess.addMail(player, seller, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimOwnerRented,
						player.getName(),
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
	public void preview(Player player)
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
						Utils.getOfflinePlayer(buyer).getName(),
						Utils.formatPrice(price),
						Utils.getTime(daysLeft, timeRemaining, true),
						Utils.getTime(duration, null, true)) + "\n";
				
				if(maxPeriod > 1 && maxPeriod - periodCount > 0)
				{
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentRemainingPeriods,
							(maxPeriod - periodCount) + "") + "\n";
				}

				if((owner != null && owner.equals(player.getUniqueId()) || buyer.equals(player.getUniqueId())) && RealEstate.instance.config.cfgEnableAutoRenew)
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
	public void msgInfo(CommandSource cs)
	{
		final Claim claim = GriefDefender.getCore().getClaim(claimId);
        final World world = Sponge.getServer().getWorld(claim.getWorldUniqueId()).orElse(null);
		String location = "[" + world.getName() + ", " +
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
