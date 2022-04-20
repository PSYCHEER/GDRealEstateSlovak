package me.EtienneDx.RealEstate.Events;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import com.griefdefender.api.claim.Claim;

public class ClaimLeaseEvent extends ClaimEvent implements Cancellable {

    private final double originalPrice;
    private final Player buyer;
    private final int frequency;
    private final int paymentsLeft;
    private double price;

    public ClaimLeaseEvent(Claim claim, Player buyer, double price, int frequency, int paymentsLeft) {
        super(claim);
        this.buyer = buyer;
        this.originalPrice = price;
        this.price = price;
        this.frequency = frequency;
        this.paymentsLeft = paymentsLeft;
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

    public int getFrequency() {
        return this.frequency;
    }

    public int getPaymentsLeft() {
        return this.paymentsLeft;
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
