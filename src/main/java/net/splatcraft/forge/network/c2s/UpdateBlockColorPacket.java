package net.splatcraft.forge.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.forge.tileentities.InkVatTileEntity;
import net.splatcraft.forge.util.ColorUtils;

public class UpdateBlockColorPacket extends PlayToServerPacket
{
    BlockPos pos;
    int color;
    int inkVatPointer = -1;

    public UpdateBlockColorPacket(BlockPos pos, int color)
    {
        this.color = color;
        this.pos = pos;
    }

    public UpdateBlockColorPacket(BlockPos pos, int color, int pointer)
    {
        this(pos, color);
        inkVatPointer = pointer;
    }

    public static UpdateBlockColorPacket decode(PacketBuffer buffer)
    {
        return new UpdateBlockColorPacket(new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt()), buffer.readInt(), buffer.readInt());
    }

    @Override
    public void execute(PlayerEntity player)
    {
        TileEntity te = player.level.getBlockEntity(pos);

        if (te instanceof InkVatTileEntity)
        {
            ((InkVatTileEntity) te).pointer = inkVatPointer;
        }

        ColorUtils.setInkColor(te, color);
    }

    @Override
    public void encode(PacketBuffer buffer)
    {
        buffer.writeInt(pos.getX());
        buffer.writeInt(pos.getY());
        buffer.writeInt(pos.getZ());
        buffer.writeInt(color);
        buffer.writeInt(inkVatPointer);
    }
}
