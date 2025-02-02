package net.splatcraft.forge.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.splatcraft.forge.network.SplatcraftPacket;

import java.util.function.Supplier;

public abstract class PlayToServerPacket extends SplatcraftPacket
{
    @Override
    public void consume(Supplier<NetworkEvent.Context> ctx)
    {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER)
        {
            ctx.get().enqueueWork(() -> this.execute(ctx.get().getSender()));
        }
        ctx.get().setPacketHandled(true);
    }

    public abstract void execute(PlayerEntity player);
}
