package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageMerchantTravel implements Message<MessageMerchantTravel> {
    private UUID worker;
    private boolean travel;
    private boolean returning;

    public MessageMerchantTravel() {
    }

    public MessageMerchantTravel(UUID recruit, boolean travel, boolean returning) {
        this.worker = recruit;
        this.travel = travel;
        this.returning = returning;
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
                .ifPresent(merchant -> this.setTraveling(player, merchant, travel, returning));

    }

    private void setTraveling(ServerPlayer player, MerchantEntity merchant, boolean travel, boolean returning) {


        if(!returning){
            merchant.setTraveling(travel);
            merchant.setCurrentWayPointIndex(0);
            if (travel){
                merchant.setIsWorking(true); // to activate the AI
                merchant.tellPlayer(player, Component.literal("Im now traveling."));
            }
            else{
                merchant.setIsWorking(false);
                merchant.tellPlayer(player, Component.literal("I stopped traveling."));
            }
        }
        else{
            merchant.setIsWorking(true); // to activate the AI
            merchant.setReturning(true);
            merchant.setTraveling(true);
            merchant.setCurrentWayPointIndex(merchant.WAYPOINTS.size() -1);
            merchant.tellPlayer(player, Component.literal("Im now returning to start position."));
        }
    }

    public MessageMerchantTravel fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        this.travel = buf.readBoolean();
        this.returning = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
        buf.writeBoolean(this.travel);
        buf.writeBoolean(this.returning);
    }
}