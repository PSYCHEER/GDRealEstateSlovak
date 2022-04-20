package me.EtienneDx.RealEstate.Events;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import com.griefdefender.api.claim.Claim;

public class ClaimSellEvent extends ClaimEvent implements Cancellable {

    private final double originalPrice;
    private final Player buyer;
    private double price;

    public ClaimSellEvent(Claim claim, Player buyer, double price) {
        super(claim);
        this.buyer = buyer;
        this.originalPrice = price;
        this.price = price;
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
