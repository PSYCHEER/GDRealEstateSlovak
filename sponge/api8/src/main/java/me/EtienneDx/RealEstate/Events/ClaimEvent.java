package me.EtienneDx.RealEstate.Events;


import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.Event;
import com.griefdefender.api.claim.Claim;

public class ClaimEvent implements Event {

    private final Claim claim;

    public ClaimEvent(Claim claim) {
        this.claim = claim;
    }

    public Claim getClaim() {
        return this.claim;
    }

    @Override
    public Cause cause() {
        return Sponge.server().causeStackManager().currentCause();
    }
}
