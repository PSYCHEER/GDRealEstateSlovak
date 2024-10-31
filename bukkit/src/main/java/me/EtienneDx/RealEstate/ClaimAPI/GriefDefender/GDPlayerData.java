package me.EtienneDx.RealEstate.ClaimAPI.GriefDefender;

import java.util.Set;

import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimAttribute;
import com.griefdefender.api.data.PlayerData;

import me.EtienneDx.RealEstate.ClaimAPI.IPlayerData;

public class GDPlayerData implements IPlayerData {

    private static final ClaimAttribute ATTRIBUTE_CLAIM_PURCHASED = ClaimAttribute.builder().name("claim_purchased").id("realestate:claim_purchased").build();

    private PlayerData playerData;

    public GDPlayerData(PlayerData playerData) {
        this.playerData = playerData;
    }

    @Override
    public int getAccruedClaimBlocks() {
        return playerData.getAccruedClaimBlocks();
    }

    @Override
    public int getBonusClaimBlocks() {
        return playerData.getBonusClaimBlocks();
    }

    @Override
    public void setAccruedClaimBlocks(int accruedClaimBlocks) {
        playerData.setAccruedClaimBlocks(accruedClaimBlocks);
    }

    @Override
    public void setBonusClaimBlocks(int bonusClaimBlocks) {
        playerData.setBonusClaimBlocks(bonusClaimBlocks);
    }

    @Override
    public int getRemainingClaimBlocks() {
        return playerData.getRemainingClaimBlocks();
    }

    @Override
    public int getTotalPurchasedClaims() {
        final Set<Claim> claims = playerData.getClaims();
        int count = 0;
        for (Claim claim : claims) {
            if (claim.hasAttribute(ATTRIBUTE_CLAIM_PURCHASED)) {
                count++;
            }
        }
        return count;
    }
}
