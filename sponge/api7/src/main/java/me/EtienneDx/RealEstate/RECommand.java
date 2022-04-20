package me.EtienneDx.RealEstate;

import java.util.ArrayList;
import java.util.UUID;


import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustTypes;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import me.EtienneDx.RealEstate.Transactions.BoughtTransaction;
import me.EtienneDx.RealEstate.Transactions.ClaimRent;
import me.EtienneDx.RealEstate.Transactions.ExitOffer;
import me.EtienneDx.RealEstate.Transactions.Transaction;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

@CommandAlias("re|realestate")
public class RECommand extends BaseCommand
{
	@Subcommand("info")
	@Description("Gives the player informations about the claim he is standing in")
	@CommandPermission("realestate.info")
	public static void info(Player player)
	{
		if(RealEstate.transactionsStore.anyTransaction(
				GriefDefender.getCore().getClaimAt(player.getLocation())))
		{
			Transaction tr = RealEstate.transactionsStore.getTransaction(
					GriefDefender.getCore().getClaimAt(player.getLocation()));
			tr.preview((Player)player);
		}
		else
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgNoTransactionFoundHere);
		}

	}
	
	@Subcommand("list")
	@Description("Displays the list of all real estate offers currently existing")
	@CommandCompletion("all|sell|rent|lease")
	@Syntax("[all|sell|rent|lease] <page>")
	public static void list(CommandSource sender, @Optional String type, @Default("1") int page)
	{
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}
		if(page <= 0)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgPageMustBePositive);
			return;
		}
		int count = 0;
		int start = (page - 1) * RealEstate.instance.config.cfgPageSize;
		String typeMsg;
		if(type == null || type.equalsIgnoreCase("all"))
		{
			count = RealEstate.transactionsStore.claimSell.values().size() + RealEstate.transactionsStore.claimRent.values().size() +
					RealEstate.transactionsStore.claimLease.values().size();
			typeMsg = "Real Estate offers";
		}
		else if(type.equalsIgnoreCase("sell"))
		{
			count = RealEstate.transactionsStore.claimSell.values().size();
			typeMsg = "Sell offers";
		}
		else if(type.equalsIgnoreCase("rent"))
		{
			count = RealEstate.transactionsStore.claimRent.values().size();
			typeMsg = "Rent offers";
		}
		else if(type.equalsIgnoreCase("lease"))
		{
			count = RealEstate.transactionsStore.claimLease.values().size();
			typeMsg = "Lease offers";
		}
		else
		{
			Messages.sendMessage(sender, RealEstate.instance.messages.msgErrorInvalidOption);
			return;
		}
		if(count == 0)
		{
			Messages.sendMessage(sender, RealEstate.instance.messages.msgNoTransactionFound);
		}
		else
		{
			ArrayList<Transaction> transactions = new ArrayList<Transaction>(count);
			if(type == null || type.equalsIgnoreCase("all"))
			{
				transactions.addAll(RealEstate.transactionsStore.claimSell.values());
				transactions.addAll(RealEstate.transactionsStore.claimRent.values());
				transactions.addAll(RealEstate.transactionsStore.claimLease.values());
			}
			else if(type.equalsIgnoreCase("sell"))
			{
				transactions.addAll(RealEstate.transactionsStore.claimSell.values());
			}
			else if(type.equalsIgnoreCase("rent"))
			{
				transactions.addAll(RealEstate.transactionsStore.claimRent.values());
			}
			else if(type.equalsIgnoreCase("lease"))
			{
				transactions.addAll(RealEstate.transactionsStore.claimLease.values());
			}
			
			int max = Math.min(start + RealEstate.instance.config.cfgPageSize, count);
			if(start <= max)
			{
				int pageCount = (int)Math.ceil(count / (double)RealEstate.instance.config.cfgPageSize);
				Messages.sendMessage(sender, RealEstate.instance.messages.msgListTransactionsHeader, 
						typeMsg, String.valueOf(page), String.valueOf(pageCount));
				for(int i = start; i < max; i++)
				{
					RealEstate.instance.log.info("transaction " + i);
					transactions.get(i).msgInfo(sender);
				}
				if(page < pageCount)
				{
					Messages.sendMessage(sender, RealEstate.instance.messages.msgListNextPage, (type != null ? type : "all"), String.valueOf(page + 1));
				}
			}
			else
			{
				Messages.sendMessage(sender, RealEstate.instance.messages.msgPageNotExists);
			}
		}
	}
	
	@Subcommand("renewrent")
	@Description("Allows the player renting a claim or subclaim to enable or disable the automatic renew of his rent")
	@Conditions("partOfRent")
    @CommandCompletion("enable|disable")
	@Syntax("[enable|disable]")
	public static void renewRent(Player player, @Optional String newStatus)
	{
		Location<World> loc = player.getLocation();
		final Claim claim = GriefDefender.getCore().getClaimAt(loc);
		ClaimRent cr = (ClaimRent)RealEstate.transactionsStore.getTransaction(claim);
		String claimType = claim.getParent() == null ? 
			RealEstate.instance.messages.keywordClaim : RealEstate.instance.messages.keywordSubclaim;
		if(!RealEstate.instance.config.cfgEnableAutoRenew)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorAutoRenewDisabled);
			return;
		}
		if(newStatus == null)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgRenewRentCurrently, cr.autoRenew ? 
					RealEstate.instance.messages.keywordEnabled :
					RealEstate.instance.messages.keywordDisabled,
				claimType);
		}
		else if(!newStatus.equalsIgnoreCase("enable") && !newStatus.equalsIgnoreCase("disable"))
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorCommandUsage, "/re renewrent [enable|disable]");
		}
		else if(cr.buyer.equals(player.getUniqueId()))
		{
			cr.autoRenew = newStatus.equalsIgnoreCase("enable");
			RealEstate.transactionsStore.saveData();
			Messages.sendMessage(player, RealEstate.instance.messages.msgRenewRentNow, cr.autoRenew ? 
					RealEstate.instance.messages.keywordEnabled :
					RealEstate.instance.messages.keywordDisabled,
				claimType);
		}
		else
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorBuyerOnly);
		}
	}
	
	@Subcommand("exitoffer")
	@Conditions("partOfBoughtTransaction")
	public class ExitOfferCommand extends BaseCommand
	{
		@Subcommand("info")
		@Default
		@Description("View informations about the exit offer")
		public void info(Player player)
		{
			BoughtTransaction bt = (BoughtTransaction)RealEstate.transactionsStore.getTransaction(player);
			if(bt.exitOffer == null)
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgInfoExitOfferNone);
			}
			else if(bt.exitOffer.offerBy.equals(player.getUniqueId()))
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgInfoExitOfferMadeByStatus, 
						String.valueOf(bt.exitOffer.price));
				Messages.sendMessage(player, RealEstate.instance.messages.msgInfoExitOfferCancel, 
						"/re exitoffer cancel");
			}
			else// it is the other player
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgInfoExitOfferMadeToStatus, 
					Utils.getOfflinePlayer(bt.exitOffer.offerBy).getName(), String.valueOf(bt.exitOffer.price));
				Messages.sendMessage(player, RealEstate.instance.messages.msgInfoExitOfferAccept, 
						"/re exitoffer accept");
				Messages.sendMessage(player, RealEstate.instance.messages.msgInfoExitOfferReject,
						"/re exitoffer refuse");
			}
		}
		
		@Subcommand("create")
		@Description("Creates an offer to break an ongoing transaction")
		@Syntax("<price>")
		public void create(Player player, @Conditions("positiveDouble") Double price)
		{
			BoughtTransaction bt = (BoughtTransaction)RealEstate.transactionsStore.getTransaction(player);
			if(bt.exitOffer != null)
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgErrorExitOfferAlreadyExists);
				return;
			}
			if(bt.buyer == null)
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgErrorExitOfferNoBuyer);
				return;
			}
			bt.exitOffer = new ExitOffer(player.getUniqueId(), price);

			Messages.sendMessage(player, RealEstate.instance.messages.msgInfoExitOfferCreatedBySelf, 
					String.valueOf(price));

			UUID other = player.getUniqueId().equals(bt.owner) ? bt.buyer : bt.owner;
			if(other != null)// not an admin claim
			{
				User otherP = Utils.getOfflinePlayer(other);
				Location<World> loc = player.getLocation();
				String claimType = GriefDefender.getCore().getClaimAt(loc).getParent() == null ? 
					RealEstate.instance.messages.keywordClaim : RealEstate.instance.messages.keywordSubclaim;
				String location = "[" + loc.getExtent().getName() + ", X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: "
					+ loc.getBlockZ() + "]";

				if(otherP.isOnline())
				{
					Messages.sendMessage(otherP.getPlayer().get(), RealEstate.instance.messages.msgInfoExitOfferCreatedByOther, 
							player.getName(), claimType, String.valueOf(price), location);
				}
				else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	        	{
	        		User u = Utils.getOfflinePlayer(other);
	        		RealEstate.ess.addMail(player, u, Messages.getMessage(RealEstate.instance.messages.msgInfoExitOfferCreatedByOther, 
							player.getName(), claimType, String.valueOf(price), location));
	        	}
			}
		}
		
		@Subcommand("accept")
		@Description("Accepts an offer to break an ongoing transaction")
		public void accept(Player player)
		{
			Location<World> loc = player.getLocation();
			final Claim claim = GriefDefender.getCore().getClaimAt(loc);
			BoughtTransaction bt = (BoughtTransaction)RealEstate.transactionsStore.getTransaction(claim);
			String claimType = claim.getParent() == null ? "claim" : "subclaim";
			if(bt.exitOffer == null)
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgErrorExitOfferNone);
			}
			else if(bt.exitOffer.offerBy.equals(player.getUniqueId()))
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgErrorExitOfferCantAcceptSelf);
			}
			else if(Utils.makePayment(player.getUniqueId(), bt.exitOffer.offerBy, bt.exitOffer.price, true, false))
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgInfoExitOfferAcceptedBySelf, 
						claimType, String.valueOf(bt.exitOffer.price));

				UUID other = player.getUniqueId().equals(bt.owner) ? bt.buyer : bt.owner;
				String location = "[" + loc.getExtent().getName() + ", X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + 
					", Z: " + loc.getBlockZ() + "]";
				if(other != null)
				{
					User otherP = Utils.getOfflinePlayer(other);
					if(otherP.isOnline())
					{
						Messages.sendMessage(otherP.getPlayer().get(), RealEstate.instance.messages.msgInfoExitOfferAcceptedByOther, 
								player.getName(), claimType, String.valueOf(bt.exitOffer.price), location);
					}
					else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
		        	{
					    User u = Utils.getOfflinePlayer(other);
	                    RealEstate.ess.addMail(player, u, Messages.getMessage(RealEstate.instance.messages.msgInfoExitOfferAcceptedByOther,
								player.getName(), claimType, String.valueOf(bt.exitOffer.price), location));
		        	}
				}
				bt.exitOffer = null;
		        claim.removeUserTrust(bt.buyer, TrustTypes.NONE);
				bt.buyer = null;
				bt.update();// eventual cancel is contained in here
			}
			// the make payment takes care of sending error if need be
		}
		
		@Subcommand("refuse")
		@Description("Refuses an offer to break an ongoing transaction")
		public void refuse(Player player)
		{
			Location<World> loc = player.getLocation();
			final Claim claim = GriefDefender.getCore().getClaimAt(loc);
			BoughtTransaction bt = (BoughtTransaction)RealEstate.transactionsStore.getTransaction(claim);
			String claimType = claim.getParent() == null ? "claim" : "subclaim";
			if(bt.exitOffer == null)
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgErrorExitOfferNone);
			}
			else if(bt.exitOffer.offerBy.equals(player.getUniqueId()))
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgErrorExitOfferCantRefuseSelf);
			}
			else
			{
				bt.exitOffer = null;
				Messages.sendMessage(player, RealEstate.instance.messages.msgInfoExitOfferRejectedBySelf);
				UUID other = player.getUniqueId().equals(bt.owner) ? bt.buyer : bt.owner;
				String location = "[" + loc.getExtent().getName() + ", X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + 
					", Z: " + loc.getBlockZ() + "]";
				if(other != null)
				{
					User otherP = Utils.getOfflinePlayer(other);
					if(otherP.isOnline())
					{
						Messages.sendMessage(otherP.getPlayer().get(), RealEstate.instance.messages.msgInfoExitOfferRejectedByOther, 
								player.getName(), claimType, location);
					}
					else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
		        	{
					    User u = Utils.getOfflinePlayer(other);
	                    RealEstate.ess.addMail(player, u, Messages.getMessage(RealEstate.instance.messages.msgInfoExitOfferRejectedByOther,
								player.getName(), claimType, location));
		        	}
				}
			}
		}
		
		@Subcommand("cancel")
		@Description("Cancels an offer to break an ongoing transaction")
		public void cancel(Player player)
		{
			Location<World> loc = player.getLocation();
			final Claim claim = GriefDefender.getCore().getClaimAt(loc);
			BoughtTransaction bt = (BoughtTransaction)RealEstate.transactionsStore.getTransaction(claim);
			String claimType = claim.getParent() == null ? "claim" : "subclaim";
			if(bt.exitOffer.offerBy.equals(player.getUniqueId()))
			{
				bt.exitOffer = null;
				Messages.sendMessage(player, RealEstate.instance.messages.msgInfoExitOfferCancelledBySelf);
				
				UUID other = player.getUniqueId().equals(bt.owner) ? bt.buyer : bt.owner;
				String location = "[" + loc.getExtent().getName() + ", X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + 
					", Z: " + loc.getBlockZ() + "]";
				if(other != null)
				{
					User otherP = Utils.getOfflinePlayer(other);
					if(otherP.isOnline())
					{
						Messages.sendMessage(otherP.getPlayer().get(), RealEstate.instance.messages.msgInfoExitOfferCancelledByOther, 
								player.getName(), claimType, location);
					}
					else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
		        	{
					    User u = Utils.getOfflinePlayer(other);
	                    RealEstate.ess.addMail(player, u, Messages.getMessage(RealEstate.instance.messages.msgInfoExitOfferCancelledByOther,
								player.getName(), claimType, location));
		        	}
				}
			}
			else
			{
				Messages.sendMessage(player, RealEstate.instance.messages.msgErrorExitOfferCantCancelOther);
			}
		}
	}
	
	@Subcommand("cancel")
	@Conditions("claimHasTransaction")
	@CommandPermission("realestate.admin")
	public static void cancelTransaction(Player player)
	{
		Location<World> loc = player.getLocation();
		final Claim claim = GriefDefender.getCore().getClaimAt(loc);
		Transaction t = RealEstate.transactionsStore.getTransaction(claim);
		t.tryCancelTransaction(player, true);
	}
	
	@HelpCommand
	public static void onHelp(CommandSource sender, CommandHelp help)
	{
        help.showHelp();
	}
}
