package me.EtienneDx.RealEstate.config.serializer;

import me.EtienneDx.RealEstate.Transactions.ClaimRent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimRentSerializer implements TypeSerializer<ClaimRent> {

    @Override
    public ClaimRent deserialize(Type type, ConfigurationNode node) throws SerializationException {
        final Map<String, Object> crMap = new HashMap<>();
        UUID claimId = null;
        try {
            claimId = UUID.fromString(node.key().toString());
        } catch (IllegalArgumentException e) {
            throw new SerializationException(e);
        }

        crMap.put("owner", node.node("owner").get(UUID.class));
        crMap.put("duration", node.node("duration").getInt());
        crMap.put("buildTrust", node.node("buildTrust").getBoolean());
        crMap.put("price", node.node("price").getDouble());
        crMap.put("periodCount", node.node("periodCount").getInt());
        crMap.put("autoRenew", node.node("autoRenew").getBoolean());
        if (!node.node("signLocation").virtual()) {
            int x = node.node("signLocation", "x").getInt();
            int y = node.node("signLocation", "y").getInt();
            int z = node.node("signLocation", "z").getInt();
            crMap.put("signLocation", x + ";" + y + ";" + z);
        }
        crMap.put("claimId", claimId);//node.node("claimId").get(UUID.class));
        crMap.put("maxPeriod", node.node("maxPeriod").getInt());
        crMap.put("destroyedSign", node.node("destroyedSign").getBoolean());
        return new ClaimRent(crMap);
    }

    @Override
    public void serialize(Type type, @Nullable ClaimRent cr, ConfigurationNode node) throws SerializationException {
        node.node("Rent", cr.claimId);
        if (cr.owner != null) {
            node.node("owner").set(cr.owner.toString());
        }
        node.node("duration").set(cr.duration);
        node.node("buildTrust").set(cr.buildTrust);
        node.node("price").set(cr.price);
        node.node("periodCount").set(cr.periodCount);
        node.node("autoRenew").set(cr.autoRenew);
        node.node("signLocation", "x").set(cr.sign.getBlockX());
        node.node("signLocation", "y").set(cr.sign.getBlockY());
        node.node("signLocation", "z").set(cr.sign.getBlockZ());
        node.node("maxPeriod").set(cr.maxPeriod);
        node.node("destroyedSign").set(cr.destroyedSign);
        // BoughtTransaction data
        if (cr.buyer != null) {
            node.node("buyer").set(cr.buyer.toString());
        }
        if (cr.exitOffer != null) {
            node.node("exitOffer", "offerBy").set(cr.exitOffer.offerBy.toString());
            node.node("exitOffer", "price").set(cr.exitOffer.price);
        }
    }

}
