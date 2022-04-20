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
import com.griefdefender.api.claim.TrustTypes;

import me.EtienneDx.RealEstate.Messages;
import me.EtienneDx.RealEstate.RealEstate;
import me.EtienneDx.RealEstate.Utils;
import me.EtienneDx.RealEstate.Events.ClaimLeaseEvent;
import me.EtienneDx.RealEstate.config.LegacyHexSerializer;
import com.griefdefender.lib.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class ClaimLease extends BoughtTransaction
{
	public LocalDateTime lastPayment = null;
	public int frequency;
	public int paymentsLeft;
	
	public ClaimLease(Map<String, Object> map)
	{
		super(map);
		if(map.get("lastPayment") != null)
			lastPayment = LocalDateTime.parse((String) map.get("lastPayment"), DateTimeFormatter.ISO_DATE_TIME);
		frequency = (int)map.get("frequency");
		paymentsLeft = (int)map.get("paymentsLeft");
	}
	
	public ClaimLease(Claim claim, Player player, double price, Location<World> sign, int frequency, int paymentsLeft)
	{
		super(claim, player, price, sign);
		this.frequency = frequency;
		this.paymentsLeft = paymentsLeft;
	}
	
	@Override
	public boolean update()
	{
		if(buyer == null)// not yet leased
		{
			if(Utils.isBlockSign(sign.getBlockType()))
			{
				Sign s = Utils.getSign(sign);
				final SignData signData = s.getOrCreate(SignData.class).orElse(null);
				if (signData == null) {
				    return false;
				}
				signData.addElement(0, LegacyHexSerializer.toText(Messages.getMessage(RealEstate.instance.config.cfgSignsHeader, false)));
				signData.addElement(1, LegacyHexSerializer.toText(NamedTextColor.DARK_GREEN + RealEstate.instance.config.cfgReplaceLease));
				if(RealEstate.instance.config.cfgUseCurrencySymbol)
				{
					if(RealEstate.instance.config.cfgUseDecimalCurrency == false)
					{
						signData.addElement(2, LegacyHexSerializer.toText(paymentsLeft + "x " + RealEstate.instance.config.cfgCurrencySymbol + " " + (int)Math.round(price)));
					}
					else
					{
						signData.addElement(2, LegacyHexSerializer.toText(paymentsLeft + "x " + RealEstate.instance.config.cfgCurrencySymbol + " " + price));
					}
				}
				else
				{
					if(RealEstate.instance.config.cfgUseDecimalCurrency == false)
					{
						signData.addElement(2, LegacyHexSerializer.toText(paymentsLeft + "x " + (int)Math.round(price) + " " + RealEstate.instance.config.currencyNamePlural));
					}
					else
					{
						signData.addElement(2, LegacyHexSerializer.toText(paymentsLeft + "x " + price + " " + RealEstate.instance.config.currencyNamePlural));
					}
				}
				signData.addElement(3, LegacyHexSerializer.toText(Utils.getTime(frequency, null, false)));
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
			int days = Period.between(lastPayment.toLocalDate(), LocalDate.now()).getDays();
			Duration hours = Duration.between(lastPayment.toLocalTime(), LocalTime.now());
			if(hours.isNegative() && !hours.isZero())
	        {
	            hours = hours.plusHours(24);
	            days--;
	        }
			if(days >= frequency)// we exceeded the time limit!
			{
				payLease();
			}
		}
		return false;
	}

	private void payLease()
	{
		if(buyer == null) return;

		User buyerPlayer = Utils.getOfflinePlayer(buyer);
		User seller = owner == null ? null : Utils.getOfflinePlayer(owner);
		
		String claimType = GriefDefender.getCore().getClaimAt(sign).getParent() == null ? RealEstate.instance.messages.keywordClaim : RealEstate.instance.messages.keywordSubclaim;
                String location = "[" + sign.getExtent().getName() + ", X: " + sign.getBlockX() + 
				", Y: " + sign.getBlockY() + ", Z: " + sign.getBlockZ() + "]";
		
		if(Utils.makePayment(owner, buyer, price, false, false))
		{
			lastPayment = LocalDateTime.now();
			paymentsLeft--;
			if(paymentsLeft > 0)
			{
				if(buyerPlayer.isOnline() && RealEstate.instance.config.cfgMessageBuyer)
				{
					Messages.sendMessage(buyerPlayer.getPlayer().get(), RealEstate.instance.messages.msgInfoClaimInfoLeasePaymentBuyer, 
							claimType,
							location, 
							Utils.formatPrice(price), 
							paymentsLeft + "");
				}
				else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	        	{
	        		User u = Utils.getOfflinePlayer(this.buyer);
	        		RealEstate.ess.addMail(buyerPlayer, u, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoLeasePaymentBuyer, 
							claimType,
							location, 
							Utils.formatPrice(price), 
							paymentsLeft + ""));
	        	}
				
				if(owner != null)
				{
					if(seller.isOnline() && RealEstate.instance.config.cfgMessageOwner)
					{
						Messages.sendMessage(seller.getPlayer().get(), RealEstate.instance.messages.msgInfoClaimInfoLeasePaymentOwner, 
								buyerPlayer.getName(),
								claimType,
								location, 
								Utils.formatPrice(price), 
								paymentsLeft + "");
					}
					else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
		        	{
		        		User u = Utils.getOfflinePlayer(this.owner);
						RealEstate.ess.addMail(buyerPlayer, u, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoLeasePaymentOwner,
								buyerPlayer.getName(),
								claimType,
								location,
								Utils.formatPrice(price),
								paymentsLeft + ""));
		        	}
				}
			}
			else
			{
				if(buyerPlayer.isOnline() && RealEstate.instance.config.cfgMessageBuyer)
				{
					Messages.sendMessage(buyerPlayer.getPlayer().get(), RealEstate.instance.messages.msgInfoClaimInfoLeasePaymentBuyerFinal, 
							claimType,
							location, 
							Utils.formatPrice(price));
				}
				else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	        	{
	        		User u = Utils.getOfflinePlayer(this.buyer);
	        		RealEstate.ess.addMail(buyerPlayer, u, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoLeasePaymentBuyerFinal,
							claimType,
							location,
							Utils.formatPrice(price)));
	        	}
				
				if(seller.isOnline() && RealEstate.instance.config.cfgMessageOwner)
				{
					Messages.sendMessage(seller.getPlayer().get(), RealEstate.instance.messages.msgInfoClaimInfoLeasePaymentOwnerFinal, 
							buyerPlayer.getName(),
							claimType,
							location, 
							Utils.formatPrice(price));
				}
				else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	        	{
	        		User u = Utils.getOfflinePlayer(this.owner);
	        		RealEstate.ess.addMail(buyerPlayer, u, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoLeasePaymentOwnerFinal,
							buyerPlayer.getName(),
							claimType,
							location,
							Utils.formatPrice(price)));
	        	}
				final Claim claim = GriefDefender.getCore().getClaimAt(sign);
				
				Utils.transferClaim(claim, buyer, owner);
				RealEstate.transactionsStore.cancelTransaction(this);// the transaction is finished
			}
		}
		else
		{
			this.exitLease();
		}
		// no need to re update, since there's no sign 
		RealEstate.transactionsStore.saveData();
	}
	
	private void exitLease()
	{
		if(buyer != null)
		{
			User buyerPlayer = Utils.getOfflinePlayer(buyer);
			User seller = owner == null ? null : Utils.getOfflinePlayer(owner);
			
			final Claim claim = GriefDefender.getCore().getClaimAt(sign);
			
			String claimType = claim.getParent() == null ? RealEstate.instance.messages.keywordClaim :
					RealEstate.instance.messages.keywordSubclaim;
			String location = "[" + sign.getExtent().getName() + ", X: " + 
					sign.getBlockX() + ", Y: " + 
					sign.getBlockY() + ", Z: " + sign.getBlockZ() + "]";
			
			if(buyerPlayer.isOnline() && RealEstate.instance.config.cfgMessageBuyer)
			{
				Messages.sendMessage(buyerPlayer.getPlayer().get(), RealEstate.instance.messages.msgInfoClaimInfoLeasePaymentBuyerCancelled, 
						claimType,
						location, 
						Utils.formatPrice(price));
			}
			else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	    	{
	    		User u = Utils.getOfflinePlayer(this.buyer);
	    		RealEstate.ess.addMail(buyerPlayer, u, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoLeasePaymentBuyerCancelled,
						claimType,
						location,
						Utils.formatPrice(price)));
	    	}
			if(seller.isOnline() && RealEstate.instance.config.cfgMessageOwner)
			{
				Messages.sendMessage(seller.getPlayer().get(), RealEstate.instance.messages.msgInfoClaimInfoLeasePaymentOwnerCancelled, 
						buyerPlayer.getName(),
						claimType,
						location, 
						Utils.formatPrice(price));
			}
			else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	    	{
	    		User u = Utils.getOfflinePlayer(this.owner);
	    		RealEstate.ess.addMail(buyerPlayer, u, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoLeasePaymentOwnerCancelled,
						buyerPlayer.getName(),
						claimType,
						location,
						Utils.formatPrice(price)));
	    	}
			
			claim.removeUserTrust(buyer, TrustTypes.NONE);
		}
		else
		{
			Utils.dropBlockAsItem(getHolder()); // the sign should still be there since the lease has never begun
		}
		RealEstate.transactionsStore.cancelTransaction(this);
	}

	@Override
	public boolean tryCancelTransaction(Player p, boolean force)
	{
		if(buyer != null)
		{
			if(p.hasPermission("realestate.admin") && force == true)
			{
				this.exitLease();
				return true;
			}
			else
			{
				final Claim claim = GriefDefender.getCore().getClaimAt(sign);
				if(p != null) {
					Messages.sendMessage(p, RealEstate.instance.messages.msgErrorCantCancelAlreadyLeased,
						claim.getParent() == null ?
							RealEstate.instance.messages.keywordClaim :
							RealEstate.instance.messages.keywordSubclaim
						);
				}
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
			RealEstate.instance.messages.keywordClaim :
			RealEstate.instance.messages.keywordSubclaim;
		
		if (owner != null && owner.equals(player.getUniqueId()))
        {
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimAlreadyOwner, claimTypeDisplay);
            return;
        }
		if(claim.getParent() == null && owner != null && !owner.equals(claim.getOwnerUniqueId()))
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimNotLeasedByOwner, claimTypeDisplay);
            RealEstate.transactionsStore.cancelTransaction(claim);
            return;
		}
		if(!player.hasPermission("realestate." + claimType + ".lease"))
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimNoLeasePermission, claimTypeDisplay);
            return;
		}
		if(player.getUniqueId().equals(buyer) || buyer != null)
		{
                    Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimAlreadyLeased, claimTypeDisplay);
            return;
		}
		
		final ClaimLeaseEvent event = new ClaimLeaseEvent(claim, player, price, frequency, paymentsLeft);
	    Sponge.getEventManager().post(event);
	    if (event.isCancelled()) {
	        return;
	    }
	    price = event.getFinalPrice();
		if(Utils.makePayment(owner, player.getUniqueId(), price, false, true))// if payment succeed
		{
			buyer = player.getUniqueId();
			lastPayment = LocalDateTime.now();
			paymentsLeft--;
			claim.addUserTrust(buyer, TrustTypes.BUILDER);
			Utils.dropBlockAsItem(getHolder());// leases don't have signs indicating the remaining time
			update();
			RealEstate.transactionsStore.saveData();

			String location = "[" + player.getLocation().getExtent() + ", " +
				"X: " + player.getLocation().getBlockX() + ", " +
				"Y: " + player.getLocation().getBlockY() + ", " +
				"Z: " + player.getLocation().getBlockZ() + "]";
			
			RealEstate.instance.addLogEntry(
                    "[" + RealEstate.transactionsStore.dateFormat.format(RealEstate.transactionsStore.date) + "] " + player.getName() + 
                    " has started leasing a " + claimType + " at " +
                    location +
                    " Price: " + price + " " + RealEstate.instance.config.currencyNamePlural);

			if(owner != null)
			{
				User seller = Utils.getOfflinePlayer(owner);
				if(RealEstate.instance.config.cfgMessageOwner && seller.isOnline())
				{
					Messages.sendMessage(seller.getPlayer().get(), RealEstate.instance.messages.msgInfoClaimOwnerLeaseStarted,
						player.getName(),
						claimTypeDisplay,
						Utils.formatPrice(price),
						location,
						paymentsLeft + "");
				}
				else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	        	{
	        		User u = Utils.getOfflinePlayer(this.owner);
	        		RealEstate.ess.addMail(player, u, Messages.getMessage(RealEstate.instance.messages.msgInfoClaimOwnerLeaseStarted,
						player.getName(),
						claimTypeDisplay,
						Utils.formatPrice(price),
						location,
						paymentsLeft + ""));
	        	}
			}
			
			Messages.sendMessage(player, RealEstate.instance.messages.msgInfoClaimBuyerLeaseStarted,
					claimTypeDisplay,
					Utils.formatPrice(price),
					paymentsLeft + "");
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
			String msg;
			msg = Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoLeaseHeader) + "\n";
			if(buyer == null)
			{
				msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoGeneralLeaseNoBuyer,
						claimTypeDisplay,
						paymentsLeft + "",
						Utils.formatPrice(price),
						Utils.getTime(frequency, null, true)) + "\n";

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
				int days = Period.between(lastPayment.toLocalDate(), LocalDate.now()).getDays();
				Duration hours = Duration.between(lastPayment.toLocalTime(), LocalTime.now());
				if(hours.isNegative() && !hours.isZero())
		        {
		            hours = hours.plusHours(24);
		            days--;
		        }
				int daysLeft = frequency - days - 1;// we need to remove the current day
				Duration timeRemaining = Duration.ofHours(24).minus(hours);
				
				msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoGeneralLeaseBuyer,
						claimTypeDisplay,
						Utils.getOfflinePlayer(buyer).getName(),
						Utils.formatPrice(price),
						paymentsLeft + "",
						Utils.getTime(daysLeft, timeRemaining, true),
						Utils.getTime(frequency, null, true)) + "\n";
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
            Messages.sendMessage(cs, RealEstate.instance.messages.msgInfoClaimInfoLeaseOneline,
				claim.getArea() + "",
				location,
				Utils.formatPrice(price),
				paymentsLeft + "");
	}

}
