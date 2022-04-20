package me.EtienneDx.RealEstate.Events;


import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.cause.Cause;
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
    public Cause getCause() {
        return Sponge.getCauseStackManager().getCurrentCause();
    }
}
