package me.EtienneDx.RealEstate;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.EtienneDx.RealEstate.ClaimAPI.IClaim;
import me.EtienneDx.RealEstate.Transactions.ClaimLease;
import me.EtienneDx.RealEstate.Transactions.ClaimRent;
import me.EtienneDx.RealEstate.Transactions.ClaimSell;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderProvider implements Configurable {

    public PlaceholderProvider() {
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new RealEstatePlaceholderExpansion().register();
        }
    }

    @Override
    public Map<String, Object> getDefaults() {
        Map<String, Object> defaults = new HashMap<String, Object>();
        return defaults;
    }

    private class RealEstatePlaceholderExpansion extends PlaceholderExpansion {

        public RealEstatePlaceholderExpansion() {
            
        }

        @Override
        public String onPlaceholderRequest(Player player, String identifier){
            return onRequest(player, identifier);
        }

        @Override
        public String onRequest(OfflinePlayer offlinePlayer, String identifier) {
            final Player player = offlinePlayer instanceof Player ? (Player) offlinePlayer : null;
            if (player == null) {
                return "";
            }

            final IClaim claim = RealEstate.claimAPI.getClaimAt(player.getLocation());
            if (claim == null) {
                return "";
            }

            switch (identifier) {
                case "claim_rent_amount" :
                    if (!(claim instanceof ClaimRent)) {
                        return "";
                    }

                    final ClaimRent rentedClaim = (ClaimRent) claim;
                    return RealEstate.econ.format(rentedClaim.price);
                case "claim_sell_amount" :
                    if (!(claim instanceof ClaimSell)) {
                        return "";
                    }

                    final ClaimSell saleClaim = (ClaimSell) claim;
                    return RealEstate.econ.format(saleClaim.price);
                case "claim_lease_amount" :
                    if (!(claim instanceof ClaimLease)) {
                        return "";
                    }

                    final ClaimLease leasedClaim = (ClaimLease) claim;
                    return RealEstate.econ.format(leasedClaim.price);
                default :
                    return null;
            }
        }

        @Override
        public boolean canRegister(){
            return true;
        }

        @Override
        public String getIdentifier() {
            return "RealEstate";
        }

        @Override
        public String getVersion() {
            return "0.1";
        }

        @Override
        public String getAuthor(){
            return RealEstate.instance.getDescription().getAuthors().toString();
        }

        @Override
        public boolean persist(){
            return true;
        }
    }
}
