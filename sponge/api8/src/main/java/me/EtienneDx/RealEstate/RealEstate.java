package me.EtienneDx.RealEstate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

import com.google.inject.Inject;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.SpongeCommandManager;
import me.EtienneDx.RealEstate.Transactions.BoughtTransaction;
import me.EtienneDx.RealEstate.Transactions.ClaimLease;
import me.EtienneDx.RealEstate.Transactions.ClaimRent;
import me.EtienneDx.RealEstate.Transactions.ClaimSell;
import me.EtienneDx.RealEstate.Transactions.Transaction;
import me.EtienneDx.RealEstate.Transactions.TransactionsStore;
import me.EtienneDx.RealEstate.config.MessageConfig;
import me.EtienneDx.RealEstate.config.RealEstateConfig;
import me.EtienneDx.RealEstate.config.RealEstateConfigData;
import me.EtienneDx.RealEstate.config.RealEstateNodeResolver;
import me.EtienneDx.RealEstate.config.serializer.ClaimLeaseSerializer;
import me.EtienneDx.RealEstate.config.serializer.ClaimRentSerializer;
import me.EtienneDx.RealEstate.config.serializer.ClaimSellSerializer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.ObjectMapper.Factory;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.util.NamingSchemes;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("realestate")
public class RealEstate
{
    @Inject public PluginContainer pluginContainer;
    @Inject public Logger log;
    @Inject @ConfigDir(sharedRoot = false)
    private Path configPath;
    public RealEstateConfig rootConfig;
    public RealEstateConfigData config;
    public MessageConfig messageConfig;
    public TypeSerializerCollection typeSerializerCollection;
    public static Factory OBJECTMAPPER_FACTORY = ObjectMapper.factoryBuilder()
            .defaultNamingScheme(NamingSchemes.PASSTHROUGH)
            .addNodeResolver(new RealEstateNodeResolver()).build();

    public static final String MOD_ID = "realestate";

	public Messages messages;
	SpongeCommandManager manager;
	public final static String pluginDirPath = "config" + File.separator + MOD_ID + File.separator;
	final static String languagesDirectory = RealEstate.pluginDirPath + "languages";
    public static EconomyService econ = null;
    public static PermissionService perms = null;
    public static NucleusProvider ess;
    
    public static RealEstate instance = null;
    
    public static TransactionsStore transactionsStore = null;

    @Listener(order = Order.LAST)
    public void onEnable(StartedEngineEvent<Server> event)
	{
		RealEstate.instance = this;

		this.log.info("RealEstate boot...");
        if (setupEconomy())
        {
            this.log.info("Vault is using " + econ + " as the economy plugin.");
        }
        else
        {
            this.log.warn("No compatible economy plugin detected.");
            this.log.warn("Disabling plugin.");
            return;
        }
        if (setupPermissions())
        {
            this.log.info("Vault is using " + perms + " for the permissions.");
        }
        else
        {
            this.log.warn("No compatible permissions plugin detected [Vault].");
            this.log.warn("Disabling plugin.");
            return;
        }

        /*if((ess = (Essentials)getServer().getPluginManager().getPlugin("Essentials")) != null)
        {
        	this.log.info("Found Essentials, using version " + ess.getDescription().getVersion());
        }*/

        if (this.typeSerializerCollection == null) {
            this.typeSerializerCollection = TypeSerializerCollection.defaults().childBuilder()
                // We use registerExact here to avoid overwriting the ClaimData/EconomyData serializers from our object mapper
                .register(io.leangen.geantyref.TypeToken.get(ClaimLease.class), new ClaimLeaseSerializer())
                .register(io.leangen.geantyref.TypeToken.get(ClaimRent.class), new ClaimRentSerializer())
                .register(io.leangen.geantyref.TypeToken.get(ClaimSell.class), new ClaimSellSerializer())
                .registerAnnotatedObjects(OBJECTMAPPER_FACTORY)
                .build();
        }

        this.rootConfig = new RealEstateConfig(this.configPath.resolve("config.yml"));
        this.config = this.rootConfig.getData();

        this.messageConfig = new MessageConfig(this.configPath.resolve("languages").resolve(this.config.languageFile));
		this.messages = this.messageConfig.getData();
		this.log.info("Customizable messages loaded.");
        
        RealEstate.transactionsStore = new TransactionsStore();
        
        new REListener().registerEvents();
        new GD_RealEstateHook();
        this.log.info("Successfully loaded RealEstate.");
	}

    @Listener
    public void registerCommands(RegisterCommandEvent<Command> event) {
        manager = new SpongeCommandManager(this.pluginContainer);
        manager.enableUnstableAPI("help");
        registerConditions();
        manager.registerCommand(new RECommand());
    }

    private void registerConditions()
    {
        manager.getCommandConditions().addCondition("inClaim", (context) -> {
            final Claim claim = GriefDefender.getCore().getClaimAt(context.getIssuer().getPlayer().serverLocation());
        	if(context.getIssuer().isPlayer() && 
        			claim == null || !claim.isWilderness())
        	{
        		return;
        	}
        	throw new ConditionFailedException(Messages.getMessage(messages.msgErrorOutOfClaim));
        });
        manager.getCommandConditions().addCondition("claimHasTransaction", (context) -> {
        	if(!context.getIssuer().isPlayer())
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorPlayerOnly));
        	}
        	final Claim claim = GriefDefender.getCore().getClaimAt(context.getIssuer().getPlayer().serverLocation());
        	if(claim == null || claim.isWilderness())
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorOutOfClaim));
        	}
        	Transaction tr = transactionsStore.getTransaction(claim);
        	if(tr == null)
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorNoOngoingTransaction));
        	}
        });
        manager.getCommandConditions().addCondition("inPendingTransactionClaim", (context) -> {
        	if(!context.getIssuer().isPlayer())
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorPlayerOnly));
        	}
        	final Claim claim = GriefDefender.getCore().getClaimAt(context.getIssuer().getPlayer().serverLocation());
        	if(claim == null || claim.isWilderness())
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorOutOfClaim));
        	}
        	Transaction tr = transactionsStore.getTransaction(claim);
        	if(tr == null)
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorNotRentNorLease));
        	}
        	else if(tr instanceof BoughtTransaction && ((BoughtTransaction)tr).getBuyer() != null)
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorAlreadyBought));
        	}
        });
        manager.getCommandConditions().addCondition("inBoughtClaim", (context) -> {
        	if(!context.getIssuer().isPlayer())
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorPlayerOnly));
        	}
        	final Claim claim = GriefDefender.getCore().getClaimAt(context.getIssuer().getPlayer().serverLocation());
        	if(claim == null || claim.isWilderness())
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorOutOfClaim));
        	}
        	Transaction tr = transactionsStore.getTransaction(claim);
        	if(tr == null || !(tr instanceof BoughtTransaction))
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorNotRentNorLease));
        	}
        });
        manager.getCommandConditions().addCondition("partOfBoughtTransaction", context -> {
        	if(!context.getIssuer().isPlayer())
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorPlayerOnly));
        	}
        	final Claim claim = GriefDefender.getCore().getClaimAt(context.getIssuer().getPlayer().serverLocation());
        	if(claim == null || claim.isWilderness())
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorOutOfClaim));
        	}
        	Transaction tr = transactionsStore.getTransaction(claim);
        	if(tr == null)
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorNoOngoingTransaction));
        	}
        	if(!(tr instanceof BoughtTransaction))
        	{
            	throw new ConditionFailedException(Messages.getMessage(messages.msgErrorNotRentNorLease));
        	}
        	if((((BoughtTransaction)tr).buyer != null && ((BoughtTransaction)tr).buyer.equals(context.getIssuer().getPlayer().uniqueId())) || 
        			(tr.getOwner() != null && (tr.getOwner().equals(context.getIssuer().getPlayer().uniqueId()))) || 
        			(claim.isAdminClaim() && context.getIssuer().getPlayer().hasPermission("realestate.admin")))
        	{
        		return;
        	}
        	throw new ConditionFailedException(Messages.getMessage(messages.msgErrorNotPartOfTransaction));
        });
        manager.getCommandConditions().addCondition("partOfRent", context -> {
        	if(!context.getIssuer().isPlayer())
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorPlayerOnly));
        	}
        	final Claim claim = GriefDefender.getCore().getClaimAt(context.getIssuer().getPlayer().serverLocation());
        	if(claim == null || claim.isWilderness())
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorOutOfClaim));
        	}
        	Transaction tr = transactionsStore.getTransaction(claim);
        	if(tr == null)
        	{
        		throw new ConditionFailedException(Messages.getMessage(messages.msgErrorNoOngoingTransaction));
        	}
        	if(!(tr instanceof ClaimRent))
        	{
            	throw new ConditionFailedException(Messages.getMessage(messages.msgErrorRentOnly));
        	}
        	if((((ClaimRent)tr).buyer != null && ((ClaimRent)tr).buyer.equals(context.getIssuer().getPlayer().uniqueId())) || 
        			(tr.getOwner() != null && (tr.getOwner().equals(context.getIssuer().getPlayer().uniqueId()))) || 
        			(claim.isAdminClaim() && context.getIssuer().getPlayer().hasPermission("realestate.admin")))
        	{
        		return;
        	}
        	throw new ConditionFailedException(Messages.getMessage(messages.msgErrorNotPartOfTransaction));
        });
        manager.getCommandConditions().addCondition(Double.class, "positiveDouble", (c, exec, value) -> {
        	if(value > 0) return;
        	throw new ConditionFailedException(Messages.getMessage(messages.msgErrorValueGreaterThanZero));
        });
	}

	public void addLogEntry(String entry)
    {
        try
        {
            File logFile = new File(this.config.logFilePath);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            FileWriter fw = new FileWriter(logFile, true);
            PrintWriter pw = new PrintWriter(fw);

            pw.println(entry);
            pw.flush();
            pw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private boolean setupEconomy()
    {
        econ = Sponge.server().serviceProvider().economyService().orElse(null);
        return econ != null;
    }

    private boolean setupPermissions()
    {
        perms = Sponge.server().serviceProvider().permissionService();
        return perms != null;
    }

    public Logger getLogger() {
        return this.log;
    }
}
