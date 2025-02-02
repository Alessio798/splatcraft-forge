package net.splatcraft.forge.items;

import net.splatcraft.forge.registries.SplatcraftItemGroups;
import net.minecraft.block.Block;

public class BlockItem extends net.minecraft.item.BlockItem
{
    public BlockItem(Block block)
    {
        super(block, new Properties().tab(SplatcraftItemGroups.GROUP_GENERAL));
    }

    public BlockItem(Block block, Properties properties)
    {
        super(block, properties);
    }
}
