package com.talhanation.workers.network;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public class MessageMerchantRemoveWayPoint implements Message<MessageMerchantRemoveWayPoint> {
    private UUID worker;

    public MessageMerchantRemoveWayPoint() {
    }

    public MessageMerchantRemoveWayPoint(UUID recruit) {
        this.worker = recruit;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.level.getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(MerchantEntity::isAlive)
                .findAny()
                .ifPresent(merchant -> this.removeLastWayPoint(player, merchant));
    }

    private void removeLastWayPoint(ServerPlayer player, MerchantEntity merchant){
        //BlockPos pos = merchant.WAYPOINTS.get(merchant.WAYPOINTS.size() - 1);

        //merchant.tellPlayer(player, Component.literal("Pos: " + pos + " was removed."));

        merchant.WAYPOINTS.remove(merchant.WAYPOINTS.size() - 1);
        merchant.WAYPOINT_ITEMS.remove(merchant.WAYPOINT_ITEMS.size() - 1);
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateMerchantScreen(merchant.WAYPOINTS, merchant.WAYPOINT_ITEMS, merchant.getCurrentTrades(), merchant.getTradeLimits(), merchant.getTraveling(), merchant.getReturning()));
    }

    public MessageMerchantRemoveWayPoint fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
    }

}