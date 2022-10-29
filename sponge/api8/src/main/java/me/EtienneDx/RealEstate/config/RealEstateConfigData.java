/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.EtienneDx.RealEstate.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import me.EtienneDx.RealEstate.RealEstate;

@ConfigSerializable
public class RealEstateConfigData {

    public final String configFilePath = RealEstate.pluginDirPath + "config.yml";
    public final String logFilePath = RealEstate.pluginDirPath + "GriefDefender_RealEstate.log";

    @Setting("Keywords.ChatPrefix")
    @Comment("What is displayed before any chat message")
    public String chatPrefix = "$f[$6RealEstate$f] ";
    
    @Setting("Keywords.SignsHeader")
    @Comment("What is displayed in top of the signs")
    public String cfgSignsHeader = "$6[RealEstate]";

    @Setting("Keywords.Sell")
    @Comment("List of all possible possible signs headers to sell a claim")
    public List<String> cfgSellKeywords = Arrays.asList("[sell]", "[sell claim]", "[sc]", "[re]", "[realestate]");
    @Setting("Keywords.Rent")
    @Comment("List of all possible possible signs headers to rent a claim")
    public List<String> cfgRentKeywords = Arrays.asList("[rent]", "[rent claim]", "[rc]");
    @Setting("Keywords.ContainerRent")
    @Comment("List of all possible possible signs headers to rent a claim")
    public List<String> cfgContainerRentKeywords = Arrays.asList("[container rent]", "[crent]");
    @Setting("Keywords.Lease")
    @Comment("List of all possible possible signs headers to lease a claim")
    public List<String> cfgLeaseKeywords = Arrays.asList("[lease]", "[lease claim]", "[lc]");

    @Setting("Keywords.Replace.Sell")
    @Comment("What is displayed on signs for preoperties to sell")
    public String cfgReplaceSell = "$2FOR SALE";
    @Setting("Keywords.Replace.Rent")
    @Comment("What is displayed on signs for preoperties to rent")
    public String cfgReplaceRent = "$2FOR RENT";
    @Setting("Keywords.Replace.Lease")
    @Comment("What is displayed on signs for preoperties to lease")
    public String cfgReplaceLease = "$2FOR LEASE";
    @Setting("Keywords.Replace.Ongoing.Rent")
    @Comment("What is displayed on the first line of the sign once someone rents a claim.")
    public String cfgReplaceOngoingRent = "$6[Rented]";
    @Setting("Keywords.Replace.ContainerRent")
    @Comment("What is displayed on the third line of the sign when renting container access only.")
    public String cfgContainerRentLine = "$9Containers only";

    @Setting("Rules.Sell")
    @Comment("Is selling claims enabled?")
    public boolean cfgEnableSell = true;
    @Setting("Rules.Rent")
    @Comment("Is renting claims enabled?")
    public boolean cfgEnableRent = true;
    @Setting("Rules.Lease")
    @Comment("Is leasing claims enabled?")
    public boolean cfgEnableLease = true;

    @Setting("Rules.AutomaticRenew")
    @Comment("Can players renting claims enable automatic renew of their contracts?")
    public boolean cfgEnableAutoRenew = true;
    @Setting("Rules.RentPeriods")
    @Comment("Can a rent contract last multiple periods?")
    public boolean cfgEnableRentPeriod = true;
    @Setting("Rules.DestroySigns.Rent")
    @Comment("Should the rent signs get destroyed once the claim is rented?")
    public boolean cfgDestroyRentSigns = false;
    @Setting("Rules.DestroySigns.Lease")
    @Comment("Should the lease signs get destroyed once the claim is leased?")
    public boolean cfgDestroyLeaseSigns = true;

    @Setting("Rules.TransferClaimBlocks")
    @Comment("Are the claim blocks transfered to the new owner on purchase or should the buyer provide them?")
    public boolean cfgTransferClaimBlocks = true;

    @Setting("Rules.UseCurrencySymbol")
    @Comment("Should the signs display the prices with a currency symbol instead of the full currency name?")
    public boolean cfgUseCurrencySymbol = false;
    @Setting("Rules.CurrencySymbol")
    @Comment("In case UseCurrencySymbol is true, what symbol should be used?")
    public String cfgCurrencySymbol = "$";
    @Setting("Rules.UseDecimalCurrency")
    @Comment("Allow players to use decimal currency e.g. $10.15")
    public boolean cfgUseDecimalCurrency = true;

    @Setting("Messaging.MessageOwner")
    @Comment("Should the owner get messaged once one of his claim is rented/leased/bought and on end of contracts?")
    public boolean cfgMessageOwner = true;

    @Setting("Messaging.MessageBuyer")
    @Comment("Should the buyer get messaged once one of his claim is rented/leased/bought and on end of contracts?")
    public boolean cfgMessageBuyer = true;
    @Setting("Messaging.BroadcastSell")
    @Comment("Should a message get broadcasted when a player put a claim for rent/lease/sell?")
    public boolean cfgBroadcastSell = true;
    @Setting("Messaging.MailOffline")
    @Comment("Should offline owner/buyers receive mails (using the Essentials plugin) when they're offline?")
    public boolean cfgMailOffline = true;

    @Setting("Default.PricesPerBlock.Sell")
    @Comment("Chat is the default price per block when selling a claim")
    public double cfgPriceSellPerBlock = 5.0;
    @Setting("Default.PricesPerBlock.Rent")
    @Comment("Chat is the default price per block when renting a claim")
    public double cfgPriceRentPerBlock = 2.0;
    @Setting("Default.PricesPerBlock.Lease")
    @Comment("Chat is the default price per block when leasing a claim")
    public double cfgPriceLeasePerBlock = 2.0;

    @Setting("Default.Duration.Rent")
    @Comment("How long is a rent period by default")
    public String cfgRentTime = "7D";
    @Setting("Default.Duration.Lease")
    @Comment("How long is a lease period by default")
    public String cfgLeaseTime = "7D";

    @Setting("Default.LeasePaymentsCount")
    @Comment("How many lease periods are required before the buyer gets the claim's ownership by default")
    public int cfgLeasePayments = 5;
    
    @Setting("Settings.PageSize")
    @Comment("How many Real Estate offer should be shown by page using the '/re list' command")
    public int cfgPageSize = 8;
    
    @Setting("Settings.MessagesFiles")
    @Comment("Language file to be used. You can see all languages files in the languages directory. If the language file does not exist, it will be created and you'll be able to modify it later on.")
    public String languageFile = "en.yml";

    @Setting("Settings.AllowAdminClaims")
    @Comment("Allow admin claims to be used by RealEstate.")
    public boolean allowAdminClaims = false;

    @Setting("Settings.AllowTownClaims")
    @Comment("Allow town claims to be used by RealEstate.")
    public boolean allowTownClaims = false;

    @Setting("Settings.CurrencyNamePlural")
    @Comment("The currency name plural used in economy.")
    public String currencyNamePlural = "dollars";

    public RealEstateConfigData() {

    }
}
