package me.EtienneDx.RealEstate.config.serializer;

import me.EtienneDx.RealEstate.Transactions.ClaimSell;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimSellSerializer implements TypeSerializer<ClaimSell> {

    @Override
    public ClaimSell deserialize(Type type, ConfigurationNode node) throws SerializationException {
        final Map<String, Object> crMap = new HashMap<>();
        UUID claimId = null;
        try {
            claimId = UUID.fromString(node.key().toString());
        } catch (IllegalArgumentException e) {
            throw new SerializationException(e);
        }

        if (!node.node("owner").virtual()) {
            crMap.put("owner", node.node("owner").get(UUID.class));
        }

        crMap.put("price", node.node("price").getDouble());
        if (!node.node("signLocation").virtual()) {
            int x = node.node("signLocation", "x").getInt();
            int y = node.node("signLocation", "y").getInt();
            int z = node.node("signLocation", "z").getInt();
            crMap.put("signLocation", x + ";" + y + ";" + z);
        }
        crMap.put("claimId", claimId);
        return new ClaimSell(crMap);
    }

    @Override
    public void serialize(Type type, @Nullable ClaimSell cr, ConfigurationNode node) throws SerializationException {
        node.node("Sell", cr.claimId.toString());
        if (cr.owner != null) {
            node.node("owner").set(cr.owner.toString());
        }
        node.node("price").set(cr.price);
        node.node("signLocation", "x").set(cr.sign.blockX());
        node.node("signLocation", "y").set(cr.sign.blockY());
        node.node("signLocation", "z").set(cr.sign.blockZ());
    }

}
