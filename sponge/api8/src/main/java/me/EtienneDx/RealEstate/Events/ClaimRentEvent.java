package me.EtienneDx.RealEstate.Events;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import com.griefdefender.api.claim.Claim;

public class ClaimRentEvent extends ClaimEvent implements Cancellable {

    private final double originalPrice;
    private final Player buyer;
    private final boolean buildTrust;
    private final boolean autoRenew;
    private double price;

    public ClaimRentEvent(Claim claim, Player buyer, double price, boolean buildTrust, boolean autoRenew) {
        super(claim);
        this.buyer = buyer;
        this.originalPrice = price;
        this.price = price;
        this.buildTrust = buildTrust;
        this.autoRenew = autoRenew;
    }

    public double getOriginalPrice() {
        return this.originalPrice;
    }

    public double getFinalPrice() {
        return this.price;
    }

    public void setNewPrice(double newPrice) {
        this.price = newPrice;
    }

    public Player getBuyer() {
        return this.buyer;
    }

    public boolean hasBuildTrust() {
        return this.buildTrust;
    }

    public boolean isAutoRenew() {
        return this.autoRenew;
    }

    private boolean cancelled = false;

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

}
