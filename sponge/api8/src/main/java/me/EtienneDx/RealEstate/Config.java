package me.EtienneDx.RealEstate;

import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Arrays;
import java.util.List;

//(header = "RealEstate wiki and newest versions are available at http://www.github.com/EtienneDx/RealEstate")
@ConfigSerializable
public class Config
{

    public final String configFilePath = RealEstate.pluginDirPath + "config.yml";
    public final String logFilePath = RealEstate.pluginDirPath + "GriefProtection_RealEstate.log";

    @Setting("RealEstate.Keywords.ChatPrefix")
    @Comment("What is displayed before any chat message")
    public String chatPrefix = "$f[$6RealEstate$f] ";
    
    @Setting("RealEstate.Keywords.SignsHeader")
    @Comment("What is displayed in top of the signs")
    public String cfgSignsHeader = "$6[RealEstate]";

    @Setting("RealEstate.Keywords.Sell")
    @Comment("List of all possible possible signs headers to sell a claim")
    public List<String> cfgSellKeywords = Arrays.asList("[sell]", "[sell claim]", "[sc]", "[re]", "[realestate]");
    @Setting("RealEstate.Keywords.Rent")
    @Comment("List of all possible possible signs headers to rent a claim")
    public List<String> cfgRentKeywords = Arrays.asList("[rent]", "[rent claim]", "[rc]");
    @Setting("RealEstate.Keywords.ContainerRent")
    @Comment("List of all possible possible signs headers to rent a claim")
    public List<String> cfgContainerRentKeywords = Arrays.asList("[container rent]", "[crent]");
    @Setting("RealEstate.Keywords.Lease")
    @Comment("List of all possible possible signs headers to lease a claim")
    public List<String> cfgLeaseKeywords = Arrays.asList("[lease]", "[lease claim]", "[lc]");

    @Setting("RealEstate.Keywords.Replace.Sell")
    @Comment("What is displayed on signs for preoperties to sell")
    public String cfgReplaceSell = "FOR SALE";
    @Setting("RealEstate.Keywords.Replace.Rent")
    @Comment("What is displayed on signs for preoperties to rent")
    public String cfgReplaceRent = "FOR RENT";
    @Setting("RealEstate.Keywords.Replace.Lease")
    @Comment("What is displayed on signs for preoperties to lease")
    public String cfgReplaceLease = "FOR LEASE";
    @Setting("RealEstate.Keywords.Replace.Ongoing.Rent")
    @Comment("What is displayed on the first line of the sign once someone rents a claim.")
    public String cfgReplaceOngoingRent = "[Rented]";
    @Setting("RealEstate.Keywords.Replace.ContainerRent")
    @Comment("What is displayed on the third line of the sign when renting container access only.")
    public String cfgContainerRentLine = NamedTextColor.BLUE + "Containers only";

    @Setting("RealEstate.Rules.Sell")
    @Comment("Is selling claims enabled?")
    public boolean cfgEnableSell = true;
    @Setting("RealEstate.Rules.Rent")
    @Comment("Is renting claims enabled?")
    public boolean cfgEnableRent = true;
    @Setting("RealEstate.Rules.Lease")
    @Comment("Is leasing claims enabled?")
    public boolean cfgEnableLease = true;

    @Setting("RealEstate.Rules.AutomaticRenew")
    @Comment("Can players renting claims enable automatic renew of their contracts?")
    public boolean cfgEnableAutoRenew = true;
    @Setting("RealEstate.Rules.RentPeriods")
    @Comment("Can a rent contract last multiple periods?")
    public boolean cfgEnableRentPeriod = true;
    @Setting("RealEstate.Rules.DestroySigns.Rent")
    @Comment("Should the rent signs get destroyed once the claim is rented?")
    public boolean cfgDestroyRentSigns = false;
    @Setting("RealEstate.Rules.DestroySigns.Lease")
    @Comment("Should the lease signs get destroyed once the claim is leased?")
    public boolean cfgDestroyLeaseSigns = true;

    @Setting("RealEstate.Rules.TransferClaimBlocks")
    @Comment("Are the claim blocks transfered to the new owner on purchase or should the buyer provide them?")
    public boolean cfgTransferClaimBlocks = true;

    @Setting("RealEstate.Rules.UseCurrencySymbol")
    @Comment("Should the signs display the prices with a currency symbol instead of the full currency name?")
    public boolean cfgUseCurrencySymbol = false;
    @Setting("RealEstate.Rules.CurrencySymbol")
    @Comment("In case UseCurrencySymbol is true, what symbol should be used?")
    public String cfgCurrencySymbol = "$";
    @Setting("RealEstate.Rules.UseDecimalCurrency")
    @Comment("Allow players to use decimal currency e.g. $10.15")
    public boolean cfgUseDecimalCurrency = true;

    @Setting("RealEstate.Messaging.MessageOwner")
    @Comment("Should the owner get messaged once one of his claim is rented/leased/bought and on end of contracts?")
    public boolean cfgMessageOwner = true;
    @Setting("RealEstate.Messaging.MessageBuyer")
    @Comment("Should the buyer get messaged once one of his claim is rented/leased/bought and on end of contracts?")
    public boolean cfgMessageBuyer = true;
    @Setting("RealEstate.Messaging.BroadcastSell")
    @Comment("Should a message get broadcasted when a player put a claim for rent/lease/sell?")
    public boolean cfgBroadcastSell = true;
    @Setting("RealEstate.Messaging.MailOffline")
    @Comment("Should offline owner/buyers receive mails (using the Essentials plugin) when they're offline?")
    public boolean cfgMailOffline = true;

    @Setting("RealEstate.Default.PricesPerBlock.Sell")
    @Comment("Chat is the default price per block when selling a claim")
    public double cfgPriceSellPerBlock = 5.0;
    @Setting("RealEstate.Default.PricesPerBlock.Rent")
    @Comment("Chat is the default price per block when renting a claim")
    public double cfgPriceRentPerBlock = 2.0;
    @Setting("RealEstate.Default.PricesPerBlock.Lease")
    @Comment("Chat is the default price per block when leasing a claim")
    public double cfgPriceLeasePerBlock = 2.0;

    @Setting("RealEstate.Default.Duration.Rent")
    @Comment("How long is a rent period by default")
    public String cfgRentTime = "7D";
    @Setting("RealEstate.Default.Duration.Lease")
    @Comment("How long is a lease period by default")
    public String cfgLeaseTime = "7D";

    @Setting("RealEstate.Default.LeasePaymentsCount")
    @Comment("How many lease periods are required before the buyer gets the claim's ownership by default")
    public int cfgLeasePayments = 5;
    
    @Setting("RealEstate.Settings.PageSize")
    @Comment("How many Real Estate offer should be shown by page using the '/re list' command")
    public int cfgPageSize = 8;
    
    @Setting("RealEstate.Settings.MessagesFiles")
    @Comment("Language file to be used. You can see all languages files in the languages directory. If the language file does not exist, it will be created and you'll be able to modify it later on.")
    public String languageFile = "en.yml";
    
    public Config()
    {

    }
    
    public String getString(List<String> li)
    {
    	return String.join(";", li);
    }
    
    public List<String> getList(String str)
    {
    	return Arrays.asList(str.split(";"));
    }
}
