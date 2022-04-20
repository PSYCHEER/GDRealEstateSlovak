package me.EtienneDx.RealEstate.config.serializer;

import me.EtienneDx.RealEstate.Transactions.ClaimLease;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimLeaseSerializer implements TypeSerializer<ClaimLease> {

    @Override
    public ClaimLease deserialize(Type type, ConfigurationNode node) throws SerializationException {
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
        if (!node.node("lastPayment").virtual()) {
            LocalDateTime date = null;
            try {
                date = LocalDateTime.parse(node.node("lastPayment").getString(), DateTimeFormatter.ISO_DATE_TIME);
                crMap.put("lastPayment", date);
            } catch (DateTimeParseException e) {
                
            }
        }
        crMap.put("frequency", node.node("frequency").getInt());
        crMap.put("paymentsLeft", node.node("paymentsLeft").getInt());
        crMap.put("price", node.node("price").getDouble());
        if (!node.node("signLocation").virtual()) {
            int x = node.node("signLocation", "x").getInt();
            int y = node.node("signLocation", "y").getInt();
            int z = node.node("signLocation", "z").getInt();
            crMap.put("signLocation", x + ";" + y + ";" + z);
        }
        crMap.put("claimId", claimId);//node.node("claimId").get(UUID.class));
        crMap.put("destroyedSign", node.node("destroyedSign").getBoolean());
        return new ClaimLease(crMap);
    }

    @Override
    public void serialize(Type type, @Nullable ClaimLease cr, ConfigurationNode node) throws SerializationException {
        node.node("Lease", cr.claimId.toString());
        if (cr.owner != null) {
            node.node("owner").set(cr.owner.toString());
        }
        if (cr.lastPayment != null) {
            node.node("lastPayment").set(cr.lastPayment.format(DateTimeFormatter.ISO_DATE_TIME));
        }
        node.node("frequency").set(cr.frequency);
        node.node("paymentsLeft").set(cr.paymentsLeft);
        node.node("price").set(cr.price);
        node.node("signLocation", "x").set(cr.sign.blockX());
        node.node("signLocation", "y").set(cr.sign.blockY());
        node.node("signLocation", "z").set(cr.sign.blockZ());
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
