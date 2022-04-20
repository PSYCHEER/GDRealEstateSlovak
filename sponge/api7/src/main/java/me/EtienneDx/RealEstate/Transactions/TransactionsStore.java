package me.EtienneDx.RealEstate.Transactions;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;

import me.EtienneDx.RealEstate.Messages;
import me.EtienneDx.RealEstate.RealEstate;
import me.EtienneDx.RealEstate.Utils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

public class TransactionsStore
{
    public final String dataFilePath = RealEstate.pluginDirPath + "transactions.data";
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();

    public HashMap<UUID, ClaimSell> claimSell;
    public HashMap<UUID, ClaimRent> claimRent;
    public HashMap<UUID, ClaimLease> claimLease;

    private static final String header = "RealEstate wiki and newest versions are available at http://www.github.com/EtienneDx/RealEstate";
    private static final ConfigurationOptions LOADER_OPTIONS = ConfigurationOptions.defaults().serializers(RealEstate.instance.typeSerializerCollection);
    private HoconConfigurationLoader loader;
    private CommentedConfigurationNode root;

    public TransactionsStore()
    {
    	loadData();
    	Sponge.getScheduler().createTaskBuilder().delay(60, TimeUnit.SECONDS).execute(
    	new Runnable()
    	{
			
			@Override
			public void run()
			{
				Iterator<ClaimRent> ite = claimRent.values().iterator();
				while(ite.hasNext())
				{
					if(ite.next().update())
						ite.remove();
				}

				Iterator<ClaimLease> it = claimLease.values().iterator();
				while(it.hasNext())
				{
					if(it.next().update())
						it.remove();
				}
				saveData();
			}
		}).interval(60, TimeUnit.SECONDS).submit(RealEstate.instance);
    }
    
    public void loadData()
    {
    	claimSell = new HashMap<>();
    	claimRent = new HashMap<>();
    	claimLease = new HashMap<>();
    	
    	File file = new File(this.dataFilePath);
    	
    	if(file.exists())
    	{
            try {
                this.loader = HoconConfigurationLoader.builder().path(file.toPath()).defaultOptions(LOADER_OPTIONS).build();
                this.root = this.loader.load();
                this.loader.save(this.root);
            } catch (Exception e) {
                RealEstate.instance.getLogger().error("Failed to load transaction data in '" + this.dataFilePath + "'", e);
                return;
            }

	    	try {
				RealEstate.instance.addLogEntry(new String(Files.readAllBytes(FileSystems.getDefault().getPath(this.dataFilePath))));
			} catch (IOException e) {
				e.printStackTrace();
			}

	    	CommentedConfigurationNode sell = root.node("Sell");
	    	CommentedConfigurationNode rent = root.node("Rent");
	    	CommentedConfigurationNode lease = root.node("Lease");
	    	
	    	if(sell != null)
	    	{
	    		RealEstate.instance.addLogEntry(sell.toString());
	    		RealEstate.instance.addLogEntry(sell.childrenMap().size() + "");
		    	for(Entry<Object, CommentedConfigurationNode> entry : sell.childrenMap().entrySet())
				{
					//ClaimSell cs = (ClaimSell)sell.get(TypeToken<ClaimSell>, key);
		    	    ClaimSell cs = null;
		    	    try {
		    	        cs = entry.getValue().get(ClaimSell.class);
		    	    } catch (IOException e) {
		    	        e.printStackTrace();
		    	        continue;
		    	    }
		    	    UUID uuid = null;
		    	    try {
		    	        uuid = UUID.fromString(entry.getKey().toString());
		    	        claimSell.put(uuid, cs);
		    	    } catch (IllegalArgumentException e) {
		    	        
		    	    }
				}
	    	}
	    	if(rent != null)
	    	{
	    	    for(Entry<Object, CommentedConfigurationNode> entry : rent.childrenMap().entrySet())
				{
	    	        ClaimRent cr = null;
                    try {
                        cr = entry.getValue().get(ClaimRent.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(entry.getKey().toString());
                        claimRent.put(uuid, cr);
                    } catch (IllegalArgumentException e) {
                        
                    }
				}
			}
	    	if(lease != null)
	    	{
	    	    for(Entry<Object, CommentedConfigurationNode> entry : lease.childrenMap().entrySet())
				{
	    	        ClaimLease cl = null;
                    try {
                        cl = entry.getValue().get(ClaimLease.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(entry.getKey().toString());
                        claimLease.put(uuid, cl);
                    } catch (IllegalArgumentException e) {
                        
                    }
		    	}
			}
    	}
    }
    
    public void saveData()
    {
        if (this.root == null) {
            try {
                this.loader = HoconConfigurationLoader.builder().path(Paths.get(this.dataFilePath)).defaultOptions(LOADER_OPTIONS).build();
                this.root = this.loader.load();
                this.loader.save(this.root);
            } catch (Exception e) {
                RealEstate.instance.getLogger().error("Failed to load transaction data in '" + this.dataFilePath + "'", e);
                return;
            }
        }
        try {
            for (ClaimSell cs : claimSell.values()) {
                this.root.node("Sell").node(cs.claimId.toString()).set(cs);
            }
            for (ClaimRent cr : claimRent.values()) {
                this.root.node("Rent").node(cr.claimId.toString()).set(cr);
            }
            for (ClaimLease cl : claimLease.values()) {
                this.root.node("Lease").node(cl.claimId.toString()).set(cl);
            }
    
            this.loader.save(this.root);
        }
        catch (IOException e)
        {
            e.printStackTrace();
			RealEstate.instance.log.info("Unable to write to the data file at \"" + this.dataFilePath + "\"");
		}
    }
	
	public boolean anyTransaction(Claim claim)
	{
		return claim != null && 
				(claimSell.containsKey(claim.getUniqueId()) || 
						claimRent.containsKey(claim.getUniqueId()) || 
						claimLease.containsKey(claim.getUniqueId()));
	}

	public Transaction getTransaction(Claim claim)
	{
		if(claimSell.containsKey(claim.getUniqueId()))
			return claimSell.get(claim.getUniqueId());
		if(claimRent.containsKey(claim.getUniqueId()))
			return claimRent.get(claim.getUniqueId());
		if(claimLease.containsKey(claim.getUniqueId()))
			return claimLease.get(claim.getUniqueId());
		return null;
	}

	public void cancelTransaction(Claim claim)
	{
		if(anyTransaction(claim))
		{
			Transaction tr = getTransaction(claim);
			cancelTransaction(tr);
		}
		saveData();
	}

	public void cancelTransaction(Transaction tr)
	{
		if(tr.getHolder() != null) {
		    Utils.dropBlockAsItem(tr.getHolder());
		}
		if(tr instanceof ClaimSell)
		{
			claimSell.remove(tr.getClaimId());
			this.root.node("Sell").removeChild(tr.getClaimId().toString());
		}
		if(tr instanceof ClaimRent)
		{
			claimRent.remove(tr.getClaimId());
			this.root.node("Rent").removeChild(tr.getClaimId().toString());
		}
		if(tr instanceof ClaimLease)
		{
			claimLease.remove(tr.getClaimId());
			this.root.node("Lease").removeChild(tr.getClaimId().toString());
		}
		saveData();
	}
	
	public boolean canCancelTransaction(Transaction tr)
	{
		return tr instanceof ClaimSell || (tr instanceof ClaimRent && ((ClaimRent)tr).buyer == null) || 
				(tr instanceof ClaimLease && ((ClaimLease)tr).buyer == null);
	}

	public void sell(Claim claim, Player player, double price, Location<World> sign)
	{
		ClaimSell cs = new ClaimSell(claim, claim.isAdminClaim() ? null : player, price, sign);
		claimSell.put(claim.getUniqueId(), cs);
		cs.update();
		saveData();
		
        final World world = Sponge.getServer().getWorld(claim.getWorldUniqueId()).orElse(null);
		RealEstate.instance.addLogEntry("[" + this.dateFormat.format(this.date) + "] " + (player == null ? "The Server" : player.getName()) + 
				" has made " + (claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.getParent() == null ? "claim" : "subclaim") + " for sale at " +
                "[" + world.getName() + ", " +
                "X: " + claim.getGreaterBoundaryCorner().getX() + ", " +
                "Y: " + claim.getGreaterBoundaryCorner().getY() + ", " +
                "Z: " + claim.getGreaterBoundaryCorner().getZ() + "] " +
                "Price: " + price + " " + RealEstate.instance.config.currencyNamePlural);
	
		String claimPrefix = claim.isAdminClaim() ? RealEstate.instance.messages.keywordAdminClaimPrefix :
				RealEstate.instance.messages.keywordClaimPrefix;
		String claimTypeDisplay = claim.getParent() == null ? RealEstate.instance.messages.keywordClaim :
				RealEstate.instance.messages.keywordSubclaim;

		if(player != null)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgInfoClaimCreatedSell,
					claimPrefix,
					claimTypeDisplay,
					Utils.formatPrice(price));
		}
		if(RealEstate.instance.config.cfgBroadcastSell)
		{
			for(Player p : Sponge.getServer().getOnlinePlayers())
			{
				if(p != player)
				{
					Messages.sendMessage(p, RealEstate.instance.messages.msgInfoClaimCreatedSellBroadcast,
							player == null ? RealEstate.instance.messages.keywordTheServer : player.getName(),
							claimPrefix,
							claimTypeDisplay,
							Utils.formatPrice(price));
				}
			}
		}
	}

	public void rent(Claim claim, Player player, double price, Location<World> sign, int duration, int rentPeriods, boolean buildTrust)
	{
		ClaimRent cr = new ClaimRent(claim, claim.isAdminClaim() ? null : player, price, sign, duration, rentPeriods, buildTrust);
		claimRent.put(claim.getUniqueId(), cr);
		cr.update();
		saveData();
		
		final World world = Sponge.getServer().getWorld(claim.getWorldUniqueId()).orElse(null);
		RealEstate.instance.addLogEntry("[" + this.dateFormat.format(this.date) + "] " + (player == null ? "The Server" : player.getName()) + 
				" has made " + (claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.getParent() == null ? "claim" : "subclaim") + " for" + (buildTrust ? "" : " container") + " rent at " +
				"[" + world.getName() + ", " +
				"X: " + claim.getLesserBoundaryCorner().getX() + ", " +
				"Y: " + claim.getLesserBoundaryCorner().getY() + ", " +
				"Z: " + claim.getLesserBoundaryCorner().getZ() + "] " +
				"Price: " + price + " " + RealEstate.instance.config.currencyNamePlural);
	
		String claimPrefix = claim.isAdminClaim() ? RealEstate.instance.messages.keywordAdminClaimPrefix :
				RealEstate.instance.messages.keywordClaimPrefix;
		String claimTypeDisplay = claim.getParent() == null ? RealEstate.instance.messages.keywordClaim :
				RealEstate.instance.messages.keywordSubclaim;
		
		if(player != null)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgInfoClaimCreatedRent,
					claimPrefix,
					claimTypeDisplay,
					Utils.formatPrice(price),
					Utils.getTime(duration, null, false));
		}
		if(RealEstate.instance.config.cfgBroadcastSell)
		{
			for(Player p : Sponge.getServer().getOnlinePlayers())
			{
				if(p != player)
				{
					Messages.sendMessage(p, RealEstate.instance.messages.msgInfoClaimCreatedRentBroadcast,
							player == null ? RealEstate.instance.messages.keywordTheServer : player.getName(),
							claimPrefix,
							claimTypeDisplay,
							Utils.formatPrice(price),
							Utils.getTime(duration, null, false));
				}
			}
		}
	}

	public void lease(Claim claim, Player player, double price, Location<World> sign, int frequency, int paymentsCount)
	{
		ClaimLease cl = new ClaimLease(claim, claim.isAdminClaim() ? null : player, price, sign, frequency, paymentsCount);
		claimLease.put(claim.getUniqueId(), cl);
		cl.update();
		saveData();
		
		final World world = Sponge.getServer().getWorld(claim.getWorldUniqueId()).orElse(null);
		RealEstate.instance.addLogEntry("[" + this.dateFormat.format(this.date) + "] " + (player == null ? "The Server" : player.getName()) + 
				" has made " + (claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.getParent() == null ? "claim" : "subclaim") + " for lease at " +
				"[" + world.getName() + ", " +
				"X: " + claim.getLesserBoundaryCorner().getX() + ", " +
				"Y: " + claim.getLesserBoundaryCorner().getY() + ", " +
				"Z: " + claim.getLesserBoundaryCorner().getZ() + "] " +
				"Payments Count : " + paymentsCount + " " + 
				"Price: " + price + " " + RealEstate.instance.config.currencyNamePlural);
	
		String claimPrefix = claim.isAdminClaim() ? RealEstate.instance.messages.keywordAdminClaimPrefix :
				RealEstate.instance.messages.keywordClaimPrefix;
		String claimTypeDisplay = claim.getParent() == null ? RealEstate.instance.messages.keywordClaim :
				RealEstate.instance.messages.keywordSubclaim;
		
		if(player != null)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgInfoClaimCreatedLease,
					claimPrefix,
					claimTypeDisplay,
					Utils.formatPrice(price),
					paymentsCount + "",
					Utils.getTime(frequency, null, false));
		}
		if(RealEstate.instance.config.cfgBroadcastSell)
		{
			for(Player p : Sponge.getServer().getOnlinePlayers())
			{
				if(p != player)
				{
					Messages.sendMessage(p, RealEstate.instance.messages.msgInfoClaimCreatedLeaseBroadcast,
							player == null ? RealEstate.instance.messages.keywordTheServer : player.getName(),
							claimPrefix,
							claimTypeDisplay,
							Utils.formatPrice(price),
							paymentsCount + "",
							Utils.getTime(frequency, null, false));
				}
			}
		}
	}

	public Transaction getTransaction(Player player)
	{
		if(player == null) return null;
		final Claim c = GriefDefender.getCore().getClaimAt(player.getLocation());
		return getTransaction(c);
	}
}
