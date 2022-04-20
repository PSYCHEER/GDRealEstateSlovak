package me.EtienneDx.RealEstate;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;

import me.EtienneDx.RealEstate.Transactions.Transaction;
import me.EtienneDx.RealEstate.config.LegacyHexSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class REListener
{
	void registerEvents()
	{
		Sponge.getEventManager().registerListeners(RealEstate.instance, this);
	}

	@Listener
	public void onSignChange(ChangeSignEvent event, @First Player player)
	{
	    final Sign sign = event.getTargetTile();
	    final List<Text> lines = event.getText().asList();
	    final String header = lines.get(0).toPlain();
	    if (header == null) {
	        return;
	    }
	    final SignData signData = event.getText();
	    final String line1 = lines.get(1).toPlain();
        String line2 = lines.get(2).toPlain();
        String line3 = lines.get(3).toPlain();
		if(RealEstate.instance.config.cfgSellKeywords.contains(header.toLowerCase()) || 
				RealEstate.instance.config.cfgLeaseKeywords.contains(header.toLowerCase()) || 
				RealEstate.instance.config.cfgRentKeywords.contains(header.toLowerCase()) || 
				RealEstate.instance.config.cfgContainerRentKeywords.contains(header.toLowerCase()))
		{
			Location<World> loc = sign.getLocation();

			final Claim claim = GriefDefender.getCore().getClaimAt(loc);
			if(claim == null || claim.isWilderness())// must have something to sell
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNotInClaim);
				event.setCancelled(true);
				Utils.dropBlockAsItem(loc);
				return;
			}
			if(RealEstate.transactionsStore.anyTransaction(claim))
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignOngoingTransaction);
				event.setCancelled(true);
				Utils.dropBlockAsItem(loc);
				return;
			}
			if(RealEstate.transactionsStore.anyTransaction(claim.getParent()))
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignParentOngoingTransaction);
				event.setCancelled(true);
				Utils.dropBlockAsItem(loc);
				return;
			}
			for(Claim c : claim.getChildren(true))
			{
			    if (c.getOwnerUniqueId().equals(claim.getOwnerUniqueId())) {
    				if(RealEstate.transactionsStore.anyTransaction(c))
    				{
    					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignSubclaimOngoingTransaction);
    					event.setCancelled(true);
    					Utils.dropBlockAsItem(loc);
    					return;
    				}
			    }
			}

			// empty is considered a wish to sell
			if(RealEstate.instance.config.cfgSellKeywords.contains(header.toLowerCase()))
			{
				if(!RealEstate.instance.config.cfgEnableSell)
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignSellingDisabled);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}

				String type = claim.getParent() == null ? "claim" : "subclaim";
				String typeDisplay = claim.getParent() == null ?
						RealEstate.instance.messages.keywordClaim : RealEstate.instance.messages.keywordSubclaim;
				if(!player.hasPermission("realestate." + type + ".sell"))
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNoSellPermission, typeDisplay);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}

				// check for a valid price
				double price;
				try
				{
					price = getDouble(event, 1, RealEstate.instance.config.cfgPriceSellPerBlock * claim.getArea());
				}
				catch (NumberFormatException e)
				{
				    e.printStackTrace();
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorInvalidNumber, line1);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}
				if(price <= 0)
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorNegativePrice, line1);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}
				if((price%1)!=0 && !RealEstate.instance.config.cfgUseDecimalCurrency) //if the price has a decimal number AND Decimal currency is disabled
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorNonIntegerPrice, line1);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}
				
				if(claim.isAdminClaim())
				{
					if(!player.hasPermission("realestate.admin"))// admin may sell admin claims
					{
						Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNoAdminSellPermission, typeDisplay);
						event.setCancelled(true);
						Utils.dropBlockAsItem(loc);
						return;
					}
				}
				else if(type.equals("claim") && !player.getUniqueId().equals(claim.getOwnerUniqueId()))// only the owner may sell his claim
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNotOwner, typeDisplay);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}

				// we should be good to sell it now
				event.setCancelled(true);// need to cancel the event, so we can update the sign elsewhere
				RealEstate.transactionsStore.sell(claim, GriefDefender.getCore().getAdminUser().getUniqueId().equals(claim.getOwnerUniqueId()) ? null : player, price, loc);
			}
			else if(RealEstate.instance.config.cfgRentKeywords.contains(header.toLowerCase()) ||
					RealEstate.instance.config.cfgContainerRentKeywords.contains(header.toLowerCase()))// we want to rent it
			{
				if(!RealEstate.instance.config.cfgEnableRent)
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignRentingDisabled);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}
				String type = claim.getParent() == null ? "claim" : "subclaim";
				String typeDisplay = claim.getParent() == null ?
						RealEstate.instance.messages.keywordClaim : RealEstate.instance.messages.keywordSubclaim;
				if(!player.hasPermission("realestate." + type + ".rent"))
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNoRentPermission, typeDisplay);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}

				// check for a valid price
				double price;
				try
				{
					price = getDouble(event, 1, RealEstate.instance.config.cfgPriceRentPerBlock * claim.getArea());
				}
				catch (NumberFormatException e)
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorInvalidNumber, line1);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}
				if(price <= 0)
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorNegativePrice, line1);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}
				if((price%1)!=0 && !RealEstate.instance.config.cfgUseDecimalCurrency) //if the price has a decimal number AND Decimal currency is disabled
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorNonIntegerPrice, line1);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}

				if(line2.isEmpty())
				{
				    line2 = RealEstate.instance.config.cfgRentTime;
					signData.addElement(2, LegacyHexSerializer.toText(line2));
				}
				int duration = parseDuration(line2);
				if(duration == 0)
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorInvalidDuration, line2,
						"10 weeks",
						"3 days",
						"1 week 3 days");
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}
				int rentPeriods = 1;
				if(RealEstate.instance.config.cfgEnableRentPeriod)
				{
					if(line3.isEmpty())
					{
					    line3 = "1";
						signData.addElement(3, Text.of(line3));
					}
					try
					{
						rentPeriods = Integer.parseInt(line3);
					}
					catch (NumberFormatException e)
					{
						Messages.sendMessage(player, RealEstate.instance.messages.msgErrorInvalidNumber, line3);
						event.setCancelled(true);
						Utils.dropBlockAsItem(loc);
						return;
					}
					if(rentPeriods <= 0)
					{
						Messages.sendMessage(player, RealEstate.instance.messages.msgErrorNegativeNumber, line3);
						event.setCancelled(true);
						Utils.dropBlockAsItem(loc);
						return;
					}
				}

				if(claim.isAdminClaim())
				{
					if(!player.hasPermission("realestate.admin"))// admin may sell admin claims
					{
						Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNoAdminRentPermission, typeDisplay);
						event.setCancelled(true);
						Utils.dropBlockAsItem(loc);
						return;
					}
				}
				else if(type.equals("claim") && !player.getUniqueId().equals(claim.getOwnerUniqueId()))// only the owner may sell his claim
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNotOwner, typeDisplay);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}

				// all should be good, we can create the rent
				event.setCancelled(true);
				RealEstate.transactionsStore.rent(claim, player, price, loc, duration, rentPeriods,
						RealEstate.instance.config.cfgRentKeywords.contains(header.toLowerCase()));
			}
			else if(RealEstate.instance.config.cfgLeaseKeywords.contains(header.toLowerCase()))// we want to rent it
			{
				if(!RealEstate.instance.config.cfgEnableLease)
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignLeasingDisabled);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}
				String type = claim.getParent() == null ? "claim" : "subclaim";
				String typeDisplay = claim.getParent() == null ?
					RealEstate.instance.messages.keywordClaim :
					RealEstate.instance.messages.keywordSubclaim;
				if(!player.hasPermission("realestate." + type + ".lease"))
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNoLeasePermission, typeDisplay);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}

				// check for a valid price
				double price;
				try
				{
					price = getDouble(event, 1, RealEstate.instance.config.cfgPriceLeasePerBlock * claim.getArea());
				}
				catch (NumberFormatException e)
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorInvalidNumber, line1);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}
				if(price <= 0)
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorNegativePrice, line1);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}
				if((price%1)!=0 && !RealEstate.instance.config.cfgUseDecimalCurrency) //if the price has a decimal number AND Decimal currency is disabled
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorNonIntegerPrice, line1);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}

				if(line2.isEmpty())
				{
				    line2 = "" + RealEstate.instance.config.cfgLeasePayments;
					signData.addElement(2, LegacyHexSerializer.toText(line2));
				}
				int paymentsCount;
				try
				{
					paymentsCount = Integer.parseInt(line2);
				}
				catch(Exception e)
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorInvalidNumber, line2);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}

				if(line3.isEmpty())
				{
				    line3 = RealEstate.instance.config.cfgLeaseTime;
					signData.addElement(3, LegacyHexSerializer.toText(line3));
				}
				int frequency = parseDuration(line3);
				if(frequency == 0)
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorInvalidDuration, line3,
						"10 weeks",
						"3 days",
						"1 week 3 days");
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}

				if(claim.isAdminClaim())
				{
					if(!player.hasPermission("realestate.admin"))// admin may sell admin claims
					{
						Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNoAdminLeasePermission, typeDisplay);
						event.setCancelled(true);
						Utils.dropBlockAsItem(loc);
						return;
					}
				}
				else if(type.equals("claim") && !player.getUniqueId().equals(claim.getOwnerUniqueId()))// only the owner may sell his claim
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNotOwner, typeDisplay);
					event.setCancelled(true);
					Utils.dropBlockAsItem(loc);
					return;
				}

				// all should be good, we can create the rent
				event.setCancelled(true);
				RealEstate.transactionsStore.lease(claim, player, price, loc, frequency, paymentsCount);
			}
		}
	}

	private int parseDuration(String line)
	{
		Pattern p = Pattern.compile("^(?:(?<weeks>\\d{1,2}) ?w(?:eeks?)?)? ?(?:(?<days>\\d{1,2}) ?d(?:ays?)?)?$", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(line);
		if(!line.isEmpty() && m.matches()) 
		{
			int ret = 0;
			if(m.group("weeks") != null)
				ret += 7 * Integer.parseInt(m.group("weeks"));
			if(m.group("days") != null)
				ret += Integer.parseInt(m.group("days"));
			return ret;
		}
		return 0;
	}

	private double getDouble(ChangeSignEvent event, int line, double defaultValue) throws NumberFormatException
	{
	    final List<Text> lines = event.getText().asList();
		if(lines.get(line).isEmpty())// if no price precised, make it the default one
		{
		    final SignData signData = event.getText();
			signData.addElement(line, Text.of(Double.toString(defaultValue)));
		}
		return Double.parseDouble(lines.get(line).toPlain());
	}

	@Listener
	public void onPlayerInteract(InteractBlockEvent.Secondary event, @First Player player)
	{
	    final Sign sign = Utils.getSign(event.getTargetBlock().getLocation().orElse(null));
		if(sign != null)
		{
		    final List<Text> lines = sign.getSignData().asList();
	        final String header = lines.get(0).toPlain();
	        if (header == null) {
	            return;
	        }

	        final Location<World> location = sign.getLocation();
			RealEstate.instance.log.info(header);
			// it is a real estate sign
			if(header.equalsIgnoreCase(Messages.getMessage(RealEstate.instance.config.cfgSignsHeader, false)))
			{
				final Claim claim = GriefDefender.getCore().getClaimAt(location);

				if(!RealEstate.transactionsStore.anyTransaction(claim))
				{
					Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNoTransaction);
					//event.getClickedBlock().breakNaturally();
					location.setBlockType(BlockTypes.AIR);
					
					event.setCancelled(true);
					return;
				}

				Transaction tr = RealEstate.transactionsStore.getTransaction(claim);
				if(player.get(Keys.IS_SNEAKING).get())
					tr.preview(player);
				else
					tr.interact(player);
			}
		}
	}

	@Listener
	public void onBreakBlock(ChangeBlockEvent.Break event, @First Player player)
	{
	    for (org.spongepowered.api.data.Transaction<BlockSnapshot> blockTransaction : event.getTransactions()) {
	        final Location<World> location = blockTransaction.getFinal().getLocation().orElse(null);
    	    final Sign sign = Utils.getSign(location);
    		if(sign != null)
    		{
    			final Claim claim = GriefDefender.getCore().getClaimAt(location);
    			if(claim != null)
    			{
    				Transaction tr = RealEstate.transactionsStore.getTransaction(claim);
    				if(tr != null && location.equals(tr.getHolder()))
    				{
    					if(tr.getOwner() != null  && !player.getUniqueId().equals(tr.getOwner()) && 
    							!player.hasPermission("realestate.destroysigns"))
    					{
    						Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNotAuthor);
    						event.setCancelled(true);
    						return;
    					}
    					else if(tr.getOwner() == null && !player.hasPermission("realestate.admin"))
    					{
    						Messages.sendMessage(player, RealEstate.instance.messages.msgErrorSignNotAdmin);
    						event.setCancelled(true);
    						return;
    					}
    					// the sign has been destroy, we can try to cancel the transaction
    					if(!tr.tryCancelTransaction(player))
    					{
    						event.setCancelled(true);
    					}
    				}
    			}
    		}
	    }
	}
}
