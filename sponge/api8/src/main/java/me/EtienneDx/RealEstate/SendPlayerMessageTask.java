package me.EtienneDx.RealEstate;

import org.spongepowered.api.service.permission.Subject;

import com.griefdefender.api.GriefDefender;

import com.griefdefender.lib.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class SendPlayerMessageTask implements Runnable
{
    // Use GD kyori lib to workaround relocation issues
    private static LegacyComponentSerializer HEX_SERIALIZER = LegacyComponentSerializer.builder().extractUrls().hexColors().character(LegacyComponentSerializer.AMPERSAND_CHAR).build();

	private Subject player;
	private String message;


	public SendPlayerMessageTask(Subject player, String message)
	{
		this.player = player;
		this.message = message;
	}

	@Override
	public void run()
	{
        if (message == null || message.length() == 0) return;

        if (player == null) {
            RealEstate.instance.log.info(message);
        } else {
            GriefDefender.getAudienceProvider().getSender(player).sendMessage(HEX_SERIALIZER.deserialize(message));
        }
	}
}
