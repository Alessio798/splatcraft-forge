package net.splatcraft.forge.network.s2c;

import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.splatcraft.forge.network.SplatcraftPacket;

import java.util.function.Supplier;

public abstract class PlayToClientPacket extends SplatcraftPacket
{
    @Override
    public void consume(Supplier<NetworkEvent.Context> ctx)
    {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT)
        {
            ctx.get().enqueueWork(this::execute);
        }
        ctx.get().setPacketHandled(true);
    }

    public abstract void execute();
}
