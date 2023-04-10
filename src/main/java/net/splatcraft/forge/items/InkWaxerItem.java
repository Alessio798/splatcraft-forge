package net.splatcraft.forge.items;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.forge.blocks.IColoredBlock;
import net.splatcraft.forge.registries.SplatcraftItemGroups;
import net.splatcraft.forge.tileentities.InkedBlockTileEntity;
import net.splatcraft.forge.util.InkBlockUtils;

public class InkWaxerItem extends Item
{
    public InkWaxerItem()
    {
        super(new Properties().durability(256).tab(SplatcraftItemGroups.GROUP_GENERAL));
    }

    public void onBlockStartBreak(ItemStack itemstack, BlockPos pos, Level level)
    {
        if(level.getBlockEntity(pos) instanceof InkedBlockTileEntity)
        {
            InkedBlockTileEntity te = (InkedBlockTileEntity) level.getBlockEntity(pos);
            te.setPermanentColor(-1);

            level.globalLevelEvent(2001, pos, Block.getId(level.getBlockState(pos)));

            if(level.getBlockState(pos).getBlock() instanceof IColoredBlock)
                ((IColoredBlock) level.getBlockState(pos).getBlock()).remoteInkClear(level, pos);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        if(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof InkedBlockTileEntity)
        {
            InkedBlockTileEntity te = (InkedBlockTileEntity) context.getLevel().getBlockEntity(context.getClickedPos());

            if(te.getPermanentColor() != te.getColor())
            {
                te.setPermanentColor(te.getColor());
                te.setPermanentInkType(InkBlockUtils.getInkType(context.getLevel().getBlockState(context.getClickedPos())));

                context.getLevel().globalLevelEvent(2005, context.getClickedPos(), 0);
                if(context.getPlayer() instanceof ServerPlayer && !context.getPlayer().isCreative())
                    context.getItemInHand().hurtAndBreak(1, context.getPlayer(), player -> player.broadcastBreakEvent(context.getHand()));
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level levelIn, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 0;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.getItem().equals(Items.HONEYCOMB) || super.isValidRepairItem(toRepair, repair);
    }
}
