package me.EtienneDx.RealEstate.ClaimAPI.GriefDefender;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.User;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustResult;
import com.griefdefender.api.claim.TrustResultTypes;
import com.griefdefender.api.data.PlayerData;
import com.griefdefender.api.event.ChangeClaimEvent;
import com.griefdefender.api.event.Event;
import com.griefdefender.api.event.ProcessTrustUserEvent;
import com.griefdefender.api.event.QueryPermissionEvent;
import com.griefdefender.api.event.RemoveClaimEvent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import me.EtienneDx.RealEstate.RealEstate;
import me.EtienneDx.RealEstate.Transactions.BoughtTransaction;
import me.EtienneDx.RealEstate.Transactions.Transaction;
import com.griefdefender.lib.kyori.adventure.text.Component;
import com.griefdefender.lib.kyori.event.EventBus;
import com.griefdefender.lib.kyori.event.EventSubscriber;

public class GDPermissionListener
{

    public GDPermissionListener() {
        new ProcessTrustUserEventListener();
        new ChangeClaimEventListener();
        new RemoveClaimEventListener();
        new QueryPermissionEventListener();
    }

    private class ProcessTrustUserEventListener {

        public ProcessTrustUserEventListener() {
            final EventBus<Event> eventBus = GriefDefender.getEventManager().getBus();

            eventBus.subscribe(ProcessTrustUserEvent.class, new EventSubscriber<ProcessTrustUserEvent>() {

                @Override
                public void on(@NonNull ProcessTrustUserEvent event) throws Throwable {
                    final User user = event.getUser();
                    final Claim claim = event.getClaim();
                    if (user == null) {
                        return;
                    }
                    final Player player = Bukkit.getPlayer(user.getUniqueId());
                    if (player == null) {
                        return;
                    }
                    Transaction b = RealEstate.transactionsStore.getTransaction(new GDClaim(claim));
                    if(b != null && player.getUniqueId().equals(b.getOwner()) && b instanceof BoughtTransaction)
                    {
                        if(((BoughtTransaction)b).getBuyer() != null) {
                            final String identifier = claim.getFriendlyIdentifier() != null ? claim.getFriendlyIdentifier() : claim.getDisplayName() != null ? claim.getDisplayName() : claim.getType().getName().toLowerCase();
                            event.setMessage(Component.text("This claim '" + identifier + "' is currently involved in a transaction, you can't access it!"));
                            final TrustResult trustResult = TrustResult.builder().user(event.getUser()).claims(event.getClaims()).trust(event.getTrustType()).type(TrustResultTypes.NOT_TRUSTED).build();
                            event.setNewTrustResult(trustResult);
                        }
                    }
                }
            });
        }
    }

    private class QueryPermissionEventListener {

        public QueryPermissionEventListener() {
            final EventBus<Event> eventBus = GriefDefender.getEventManager().getBus();

            eventBus.subscribe(QueryPermissionEvent.class, new EventSubscriber<QueryPermissionEvent>() {

                @Override
                public void on(@NonNull QueryPermissionEvent event) throws Throwable {
                    final User user = event.getCause().first(User.class).orElse(null);
                    if (user == null) {
                        return;
                    }
                    final Player player = Bukkit.getPlayer(user.getUniqueId());
                    if (player == null || event.getLocation() == null) {
                        return;
                    }
                    final Claim claim = GriefDefender.getCore().getClaimAt(event.getLocation());
                    if (claim == null || claim.isWilderness()) {
                        return;
                    }

                    if (event.getPermission().contains("block") || event.getPermission().contains("item")) {
                        final Transaction b = RealEstate.transactionsStore.getTransaction(new GDClaim(claim));
                        if(b != null && player.getUniqueId().equals(b.getOwner()) && b instanceof BoughtTransaction) {
                            if(((BoughtTransaction)b).getBuyer() != null) {
                                final String identifier = claim.getFriendlyIdentifier() != null ? claim.getFriendlyIdentifier() : claim.getDisplayName() != null ? claim.getDisplayName() : claim.getType().getName().toLowerCase();
                                event.setMessage(Component.text("This claim '" + identifier + "' is currently involved in a transaction, you can't resize it!"));
                                event.cancelled(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class ChangeClaimEventListener {

        public ChangeClaimEventListener() {
            final EventBus<Event> eventBus = GriefDefender.getEventManager().getBus();

            eventBus.subscribe(ChangeClaimEvent.class, new EventSubscriber<ChangeClaimEvent>() {

                @Override
                public void on(@NonNull ChangeClaimEvent event) throws Throwable {
                    final User user = event.getCause().first(User.class).orElse(null);
                    if (user == null) {
                        return;
                    }
                    final Player player = Bukkit.getPlayer(user.getUniqueId());
                    if (player == null) {
                        return;
                    }
                    for (Claim claim : event.getClaims()) {
                        if (!canChangeClaim(event, player, claim)) {
                            return;
                        }
                        for (Claim child : claim.getChildren(true)) {
                            if (!canChangeClaim(event, player, child)) {
                                return;
                            }
                        }
                    }
                }
            });
        }

        private boolean canChangeClaim(ChangeClaimEvent event, Player player, Claim claim) {
            Transaction b = RealEstate.transactionsStore.getTransaction(new GDClaim(claim));
            if(b != null && b instanceof BoughtTransaction)
            {
                if(((BoughtTransaction)b).getBuyer() != null) {
                    final String identifier = claim.getFriendlyIdentifier() != null ? claim.getFriendlyIdentifier() : claim.getDisplayName() != null ? claim.getDisplayName() : claim.getType().getName().toLowerCase();
                    event.setMessage(Component.text("This claim '" + identifier + "' is currently involved in a transaction, you can't resize it!"));
                    event.cancelled(true);
                    return false;
                }
            }
            return true;
        }
    }

    private class RemoveClaimEventListener {

        public RemoveClaimEventListener() {
            final EventBus<Event> eventBus = GriefDefender.getEventManager().getBus();

            eventBus.subscribe(RemoveClaimEvent.class, new EventSubscriber<RemoveClaimEvent>() {

                @Override
                public void on(@NonNull RemoveClaimEvent event) throws Throwable {
                    final User user = event.getCause().first(User.class).orElse(null);
                    if (user == null) {
                        return;
                    }
                    final Player player = Bukkit.getPlayer(user.getUniqueId());
                    if (player == null) {
                        return;
                    }
                    final PlayerData playerData = GriefDefender.getCore().getPlayerData(player.getWorld().getUID(), player.getUniqueId());
                    if (playerData.canIgnoreClaim(event.getClaim())) {
                        return;
                    }

                    for (Claim claim : event.getClaims()) {
                        if (!canRemoveClaim(event, player, claim)) {
                            return;
                        }
                        for (Claim child : claim.getChildren(true)) {
                            if (!canRemoveClaim(event, player, child)) {
                                return;
                            }
                        }
                    }
                }
            });
        }

        private boolean canRemoveClaim(RemoveClaimEvent event, Player player, Claim claim) {
            final Transaction b = RealEstate.transactionsStore.getTransaction(new GDClaim(claim));
            if(b != null && b instanceof BoughtTransaction)
            {
                if(((BoughtTransaction)b).getBuyer() != null) {
                    final String identifier = claim.getFriendlyIdentifier() != null ? claim.getFriendlyIdentifier() : claim.getDisplayName() != null ? claim.getDisplayName() : claim.getType().getName().toLowerCase();
                    event.setMessage(Component.text("The claim '" + identifier + "' is currently involved in a transaction, you can't remove it!"));
                    event.cancelled(true);
                    return false;
                }
            }
            return true;
        }
    }
}