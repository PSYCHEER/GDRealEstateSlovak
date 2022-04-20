package me.EtienneDx.RealEstate;

import io.github.nucleuspowered.nucleus.api.service.NucleusMailService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;

public class NucleusProvider {

    private final NucleusMailService MAIL_SERVICE;

    public NucleusProvider() {
        MAIL_SERVICE = Sponge.getServiceManager().provide(NucleusMailService.class).get();
    }

    public void addMail(User playerFrom, User playerTo, String message) {
        MAIL_SERVICE.sendMail(playerFrom, playerTo, message);
    }
}
