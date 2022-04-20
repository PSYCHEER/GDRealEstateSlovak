package me.EtienneDx.RealEstate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.data.PlayerData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

public class Utils
{
    public static boolean makePayment(UUID receiver, UUID giver, double amount, boolean msgSeller, boolean msgBuyer)
    {
    	// seller might be null if it is the server
    	User s = receiver != null ? Utils.getOfflinePlayer(receiver) : null, b = Utils.getOfflinePlayer(giver);
    	final UniqueAccount buyerAccount = RealEstate.econ.getOrCreateAccount(b.getUniqueId()).orElse(null);
    	final UniqueAccount sellerAccount = s!= null ? RealEstate.econ.getOrCreateAccount(s.getUniqueId()).orElse(null) : null;
    	final Currency defaultCurrency = RealEstate.econ.getDefaultCurrency();
    	final double balance = buyerAccount.getBalance(defaultCurrency).doubleValue();
    	if(balance < amount)
    	{
    		if(b.isOnline() && msgBuyer)
    		{
				Messages.sendMessage(b.getPlayer().get(), RealEstate.instance.messages.msgErrorNoMoneySelf);
    		}
    		if(s != null && s.isOnline() && msgSeller)
    		{
				Messages.sendMessage(s.getPlayer().get(), RealEstate.instance.messages.msgErrorNoMoneyOther, b.getName());
    		}
    		return false;
    	}
    	TransactionResult resp = buyerAccount.withdraw(defaultCurrency, BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
    	if(resp.getResult() != ResultType.SUCCESS)
    	{
    		if(b.isOnline() && msgBuyer)
    		{
				Messages.sendMessage(b.getPlayer().get(), RealEstate.instance.messages.msgErrorNoWithdrawSelf);
    		}
    		if(s != null && s.isOnline() && msgSeller)
    		{
				Messages.sendMessage(b.getPlayer().get(), RealEstate.instance.messages.msgErrorNoWithdrawOther);
    		}
    		return false;
    	}
    	if(s != null)
    	{
    		resp = sellerAccount.deposit(defaultCurrency, BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
    		if(resp.getResult() != ResultType.SUCCESS)
    		{
    			if(b.isOnline() && msgBuyer)
        		{
					Messages.sendMessage(b.getPlayer().get(), RealEstate.instance.messages.msgErrorNoDepositOther, s.getName());
        		}
        		if(s != null && s.isOnline() && msgSeller)
        		{
					Messages.sendMessage(b.getPlayer().get(), RealEstate.instance.messages.msgErrorNoDepositSelf, b.getName());
        		}
        		buyerAccount.deposit(defaultCurrency, BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
        		return false;
    		}
    	}
    	
    	return true;
    }
	
	public static String getTime(int days, Duration hours, boolean details)
	{
		String time = "";
		if(days >= 7)
		{
			time += (days / 7) + " week" + (days >= 14 ? "s" : "");
		}
		if(days % 7 > 0)
		{
			time += (time.isEmpty() ? "" : " ") + (days % 7) + " day" + (days % 7 > 1 ? "s" : "");
		}
		if((details || days < 7) && hours != null && hours.toHours() > 0)
		{
			time += (time.isEmpty() ? "" : " ") + hours.toHours() + " hour" + (hours.toHours() > 1 ? "s" : "");
		}
		if((details || days == 0) && hours != null && (time.isEmpty() || hours.toMinutes() % 60 > 0))
		{
			time += (time.isEmpty() ? "" : " ") + (hours.toMinutes() % 60) + " min" + (hours.toMinutes() % 60 > 1 ? "s" : "");
		}
		
		return time;
	}
	
	public static void transferClaim(Claim claim, UUID buyer, UUID seller)
	{
		// blocks transfer :
		// if transfert is true, the seller will lose the blocks he had
		// and the buyer will get them
		// (that means the buyer will keep the same amount of remaining blocks after the transaction)
		if(claim.getParent() == null && RealEstate.instance.config.cfgTransferClaimBlocks)
		{
			final PlayerData buyerData = GriefDefender.getCore().getPlayerData(claim.getWorldUniqueId(), buyer);
			if(seller != null)
			{
				final PlayerData sellerData = GriefDefender.getCore().getPlayerData(claim.getWorldUniqueId(), seller);
				
				// the seller has to provide the blocks
				sellerData.setBonusClaimBlocks(sellerData.getBonusClaimBlocks() - claim.getArea());
				if (sellerData.getBonusClaimBlocks() < 0)// can't have negative bonus claim blocks, so if need be, we take into the accrued 
				{
					sellerData.setAccruedClaimBlocks(sellerData.getAccruedClaimBlocks() + sellerData.getBonusClaimBlocks());
					sellerData.setBonusClaimBlocks(0);
				}
			}
			
			// the buyer receive them
			buyerData.setBonusClaimBlocks(buyerData.getBonusClaimBlocks() + claim.getArea());
		}
		
		// start to change owner
		if (seller != null) 
		{
			for(Claim child : claim.getChildren(true))
			{
				if (child.getOwnerUniqueId().equals(claim.getOwnerUniqueId())) 
				{
					child.removeUserTrust(seller, TrustTypes.NONE);
				}
			}
			claim.removeUserTrust(seller, TrustTypes.NONE);
		}

		final ClaimResult result = claim.transferOwner(buyer);
		if (!result.successful())// error occurs when trying to change subclaim owner
		{
			final Player player = Sponge.getServer().getPlayer(buyer).orElse(null);
			if (player != null) 
			{
				player.sendMessage(Text.of("Could not transfer claim! Result was '" + result.getResultType().toString() + "'."));
			}
		}
	}
	
	public static String getSignString(String str)
	{
		if(str.length() > 16)
			str = str.substring(0, 16);
		return str;
	}

    public static boolean isBlockSign(BlockType block) {
        return block instanceof BlockSign;
    }

    public static void dropBlockAsItem(Location<World> location) {
        final Block nmsBlock = (Block) location.getBlockType();
        final net.minecraft.world.World nmsWorld = (net.minecraft.world.World) location.getExtent();
        final BlockPos pos = toBlockPos(location);
        nmsBlock.dropBlockAsItem(nmsWorld, pos, (IBlockState) location.getBlock(), 0);
        nmsWorld.setBlockToAir(pos);
    }

    public static BlockPos toBlockPos(Vector3i vector) {
        if (vector == null) {
            return null;
        }
        return new BlockPos(vector.getX(), vector.getY(), vector.getZ());
    }

    public static BlockPos toBlockPos(Location<World> location) {
        if (location == null) {
            return null;
        }
        return new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static Sign getSign(Location<World> location) {
        if (location == null) {
            return null;
        }

        final TileEntity tileEntity = location.getTileEntity().orElse(null);
        if (tileEntity == null) {
            return null;
        }

        if (!(tileEntity instanceof Sign)) {
            return null;
        }

        return (Sign) tileEntity;
    }

    public static User getOfflinePlayer(UUID uuid) {
        return Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(uuid).orElse(null);
    }

    public static String formatPrice(double price) {
        return String.format("%.2f", price);
    }

    public static String posToString(Location<World> location) {
        return posToString(location.getBlockPosition());
    }

    public static String posToString(Vector3i pos) {
        return posToString(pos.getX(), pos.getY(), pos.getZ());
    }

    public static String posToString(int x, int y, int z) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(x);
        stringBuilder.append(";");
        stringBuilder.append(y);
        stringBuilder.append(";");
        stringBuilder.append(z);

        return stringBuilder.toString();
    }

    public static Vector3i posFromString(String pos) {
        String[] elements = pos.split(";");

        if (elements.length < 3) {
            return null;
        }

        String xString = elements[0];
        String yString = elements[1];
        String zString = elements[2];

        int x = Integer.parseInt(xString);
        int y = Integer.parseInt(yString);
        int z = Integer.parseInt(zString);

        return new Vector3i(x, y, z);
    }
}
