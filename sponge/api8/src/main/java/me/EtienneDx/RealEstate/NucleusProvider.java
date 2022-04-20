package me.EtienneDx.RealEstate;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import io.github.nucleuspowered.nucleus.api.module.mail.NucleusMailService;

public class NucleusProvider {

    private final NucleusMailService MAIL_SERVICE;

    public NucleusProvider() {
        MAIL_SERVICE = Sponge.serviceProvider().provide(NucleusMailService.class).get();
    }

    public void addMail(ServerPlayer playerFrom, User playerTo, String message) {
        MAIL_SERVICE.sendMail(playerFrom.uniqueId(), playerTo.uniqueId(), message);
    }

    public void addMail(User playerFrom, User playerTo, String message) {
        MAIL_SERVICE.sendMail(playerFrom.uniqueId(), playerTo.uniqueId(), message);
    }
}
