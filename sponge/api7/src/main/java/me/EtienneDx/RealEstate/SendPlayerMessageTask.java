package me.EtienneDx.RealEstate;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

public class SendPlayerMessageTask implements Runnable
{
	private CommandSource player;
	private String message;


	public SendPlayerMessageTask(CommandSource player, String message)
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
            player.sendMessage(Text.of(message));
        }
	}
}
