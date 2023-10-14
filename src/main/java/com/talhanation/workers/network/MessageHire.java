package com.talhanation.workers.network;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageHire implements Message<MessageHire> {

    private UUID player;
    private UUID worker;

    public MessageHire() {
    }

    public MessageHire(UUID player, UUID recruit) {
        this.player = player;
        this.worker = recruit;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.getCommandSenderWorld().getEntitiesOfClass(AbstractWorkerEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(AbstractWorkerEntity::isAlive)
                .findAny()
                .ifPresent(abstractRecruitEntity -> CommandEvents.handleRecruiting(player, abstractRecruitEntity));

    }

    public MessageHire fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.worker = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.worker);
    }

}