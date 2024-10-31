package me.EtienneDx.RealEstate.ClaimAPI.GriefDefender;

import java.util.Iterator;
import java.util.UUID;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimAttribute;
import com.griefdefender.api.claim.ClaimSchematic;
import com.griefdefender.api.claim.ClaimSnapshot;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;

import org.bukkit.Bukkit;
import org.bukkit.World;

import me.EtienneDx.RealEstate.ClaimAPI.ClaimPermission;
import me.EtienneDx.RealEstate.ClaimAPI.IClaim;

public class GDClaim implements IClaim{

    private static final ClaimAttribute ATTRIBUTE_CLAIM_PURCHASED = ClaimAttribute.builder().name("claim_purchased").id("realestate:claim_purchased").build();

    private Claim claim;

    public GDClaim(Claim claim) {
        this.claim = claim;
    }

    public Claim getClaim() {
        return claim;
    }

    @Override
    public String getId() {
        return claim.getUniqueId().toString();
    }

    @Override
    public int getArea() {
        return claim.getArea();
    }

    @Override
    public World getWorld() {
        return Bukkit.getWorld(claim.getWorldUniqueId());
    }

    @Override
    public int getX() {
        return claim.getLesserBoundaryCorner().getX();
    }

    @Override
    public int getY() {
        return claim.getLesserBoundaryCorner().getY();
    }

    @Override
    public int getZ() {
        return claim.getLesserBoundaryCorner().getZ();
    }

    @Override
    public boolean isAdminClaim() {
        return claim.isAdminClaim();
    }

    @Override
    public boolean isTownClaim() {
        return claim.isTown();
    }

    @Override
    public Iterable<IClaim> getChildren() {
        return new Iterable<IClaim>() {
            @Override
            public Iterator<IClaim> iterator() {
                return new Iterator<IClaim>() {
                    private Iterator<Claim> it = claim.getChildren(true).iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public IClaim next() {
                        return new GDClaim(it.next());
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }
        };
    }

    @Override
    public boolean isWilderness() {
        return claim.isWilderness();
    }

    @Override
    public boolean isSubClaim() {
        return claim.getParent() != null && !claim.getParent().isWilderness();
    }

    @Override
    public boolean isParentClaim() {
        return claim.getParent() == null || claim.getParent().isWilderness();
    }

    @Override
    public IClaim getParent() {
        return isParentClaim() ? null : new GDClaim(claim.getParent());
    }

    @Override
    public void dropPlayerPermissions(UUID player) {
        claim.removeUserTrust(player, TrustTypes.NONE);
    }

    @Override
    public void addPlayerPermissions(UUID player, ClaimPermission permission) {
        TrustType trust = TrustTypes.NONE;
        switch (permission) {
            case ACCESS:
                trust = TrustTypes.ACCESSOR;
                break;
            case BUILD:
                trust = TrustTypes.BUILDER;
                break;
            case CONTAINER:
                trust = TrustTypes.CONTAINER;
                break;
            case EDIT:
                trust = TrustTypes.MANAGER;
                break;
            case MANAGE:
                trust = TrustTypes.MANAGER;
                break;
        }

        claim.addUserTrust(player, trust);
    }

    @Override
    public void clearPlayerPermissions() {
        claim.removeAllUserTrusts();
    }

    @Override
    public void removeManager(UUID player) {
        // No equivalent in GD
    }

    @Override
    public void addManager(UUID player) {
        // No equivalent in GD
    }

    @Override
    public void clearManagers() {
        // No equivalent in GD
    }

    @Override
    public UUID getOwner() {
        return claim.getOwnerUniqueId();
    }

    @Override
    public String getOwnerName() {
        return claim.getOwnerName();
    }

    @Override
    public void setInheritPermissions(boolean inherit) {
        claim.getData().setInheritParent(inherit);
    }

    @Override
    public boolean createSnapshot(String name) {
        final ClaimSnapshot snapshot = claim.createSnapshot(name, false);
        return snapshot != null;
    }

    @Override
    public boolean restoreSnapshot(String name) {
        final ClaimSnapshot snapshot = claim.getSnapshots().get(name);
        if (snapshot != null) {
            return snapshot.apply(claim);
        }
        return false;
    }

    @Override
    public boolean createSchematic(String name) {
        if (!this.claim.isAdminClaim() || GriefDefender.getCore().getWorldEditProvider() == null) {
            return false;
        }
        final ClaimSchematic schematic = ClaimSchematic.builder().claim(this.claim).name(name).build();
        return schematic != null;
    }

    @Override
    public boolean restoreSchematic(String name) {
        if (!this.claim.isAdminClaim() || GriefDefender.getCore().getWorldEditProvider() == null) {
            return false;
        }
        final ClaimSchematic schematic = this.claim.getSchematics().get(name);
        if (schematic != null) {
            schematic.apply();
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteSchematic(String name) {
        if (!this.claim.isAdminClaim() || GriefDefender.getCore().getWorldEditProvider() == null) {
            return false;
        }

        return this.claim.deleteSchematic(name);
    }

    @Override
    public void addPurchasedAttribute() {
        this.claim.addAttribute(ATTRIBUTE_CLAIM_PURCHASED);
    }
}
