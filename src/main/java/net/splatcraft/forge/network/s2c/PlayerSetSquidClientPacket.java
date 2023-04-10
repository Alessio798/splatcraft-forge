package net.splatcraft.forge.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;

import java.util.UUID;

public class PlayerSetSquidClientPacket extends PlayToClientPacket
{
    UUID target;
    private final boolean squid;

    public PlayerSetSquidClientPacket(UUID player, boolean squid) {
        this.squid = squid;
        this.target = player;
    }

    public static PlayerSetSquidClientPacket decode(FriendlyByteBuf buffer)
    {
        return new PlayerSetSquidClientPacket(buffer.readUUID(), buffer.readBoolean());
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeUUID(target);
        buffer.writeBoolean(squid);
    }

    @Override
    public void execute() {
        if (Minecraft.getInstance().level.getPlayerByUUID(this.target) == null) {
            return;
        }
        PlayerInfo target = PlayerInfoCapability.get(Minecraft.getInstance().level.getPlayerByUUID(this.target));
        target.setIsSquid(squid);
    }
}
