package net.splatcraft.forge.util;

import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.blocks.IColoredBlock;
import net.splatcraft.forge.blocks.InkedBlock;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftStats;
import net.splatcraft.forge.tileentities.InkColorTileEntity;
import net.splatcraft.forge.tileentities.InkedBlockTileEntity;

public class InkBlockUtils
{
    public static boolean playerInkBlock(PlayerEntity player, World level, BlockPos pos, int color, float damage, InkType inkType)
    {
        boolean inked = inkBlock(level, pos, color, damage, inkType);

        if (inked)
        {
            player.awardStat(SplatcraftStats.BLOCKS_INKED);
        }

        return inked;
    }

    public static boolean inkBlock(World level, BlockPos pos, int color, float damage, InkType inkType)
    {
        BlockState state = level.getBlockState(pos);

        if (InkedBlock.isTouchingLiquid(level, pos))
            return false;

        if (isUninkable(level, pos))
            return false;

        if (state.getBlock() instanceof IColoredBlock)
            return ((IColoredBlock) state.getBlock()).inkBlock(level, pos, color, damage, inkType);

        BlockState inkState = getInkState(inkType, level, pos);

        InkedBlockTileEntity inkte = (InkedBlockTileEntity) SplatcraftBlocks.inkedBlock.createTileEntity(inkState, level);
        if (inkte == null) {
            return false;
        }
        inkte.setColor(color);
        inkte.setSavedState(state);

        level.setBlock(pos, inkState, 0);
        level.setBlockEntity(pos, inkte);
        level.markAndNotifyBlock(pos, level.getChunkAt(pos), inkState, inkState, 3, 512);

        for(Direction facing : Direction.values())
        {
            if(level.getBlockEntity(pos.relative(facing)) instanceof InkedBlockTileEntity)
            {
                InkedBlockTileEntity otherTe = (InkedBlockTileEntity) level.getBlockEntity(pos.relative(facing));
                otherTe.setSavedState(otherTe.getSavedState().getBlock().updateShape(otherTe.getSavedState(), facing.getOpposite(), inkState, level, pos.relative(facing), pos));
            }
        }

        return true;
    }


    public static BlockState getInkState(InkType inkType, World level, BlockPos pos)
    {
        return inkType.block.defaultBlockState();

    }


    public static boolean canInkFromFace(World level, BlockPos pos, Direction face)
    {
        if (!(level.getBlockState(pos).getBlock() instanceof IColoredBlock) && isUninkable(level, pos))
            return false;

        return canInkPassthrough(level, pos.relative(face)) || !level.getBlockState(pos.relative(face)).is(SplatcraftTags.Blocks.BLOCKS_INK);
    }

    public static boolean isUninkable(World level, BlockPos pos) {

        if (InkedBlock.isTouchingLiquid(level, pos))
            return true;

        Block block = level.getBlockState(pos).getBlock();

        if (SplatcraftTags.Blocks.UNINKABLE_BLOCKS.contains(block))
            return true;

        if (!(level.getBlockEntity(pos) instanceof InkColorTileEntity) && level.getBlockEntity(pos) != null)
            return true;

        return canInkPassthrough(level, pos);
    }

    public static boolean canInkPassthrough(World level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);

        return state.getCollisionShape(level, pos).isEmpty() || level.getBlockState(pos).is(SplatcraftTags.Blocks.INK_PASSTHROUGH);
    }

    public static boolean canSquidHide(LivingEntity entity)
    {
        return !entity.isSpectator() && (entity.isOnGround() || !entity.level.getBlockState(new BlockPos(entity.getX(), entity.getY() - 0.1, entity.getZ())).getBlock().equals(Blocks.AIR))
                && canSquidSwim(entity) || canSquidClimb(entity);
    }

    public static boolean canSquidSwim(LivingEntity entity)
    {
        boolean canSwim = false;

        BlockPos down = getBlockStandingOnPos(entity);

        Block standingBlock = entity.level.getBlockState(down).getBlock();
        if (standingBlock instanceof IColoredBlock)
            canSwim = ((IColoredBlock) standingBlock).canSwim();

        return canSwim && ColorUtils.colorEquals(entity, entity.level.getBlockEntity(down));
    }

    public static BlockPos getBlockStandingOnPos(Entity entity)
    {
        BlockPos result;
        for(double i = 0; i >= -0.5; i-=0.1)
        {
            result = new BlockPos(entity.getX(), entity.getY()+i, entity.getZ());

            VoxelShape shape = entity.level.getBlockState(result).getCollisionShape(entity.level, result, ISelectionContext.of(entity));

            if(!shape.isEmpty() && shape.bounds().minY <= entity.getY()-result.getY())
                return result;
        }

        return new BlockPos(entity.getX(), entity.getY()-0.6, entity.getZ());
    }

    public static boolean onEnemyInk(LivingEntity entity)
    {
        if (!entity.isOnGround())
            return false;
        boolean canDamage = false;
        BlockPos pos = getBlockStandingOnPos(entity);

        if (entity.level.getBlockState(pos).getBlock() instanceof IColoredBlock)
            canDamage = ((IColoredBlock) entity.level.getBlockState(pos).getBlock()).canDamage();

        return canDamage && ColorUtils.getInkColor(entity.level.getBlockEntity(pos)) != -1 && !canSquidSwim(entity);
    }

    public static boolean canSquidClimb(LivingEntity entity)
    {
        if (onEnemyInk(entity))
            return false;
        for (int i = 0; i < 4; i++)
        {
            float xOff = (i < 2 ? .32f : 0) * (i % 2 == 0 ? 1 : -1), zOff = (i < 2 ? 0 : .32f) * (i % 2 == 0 ? 1 : -1);
            BlockPos pos = new BlockPos(entity.getX() - xOff, entity.getY(), entity.getZ() - zOff);
            Block block = entity.level.getBlockState(pos).getBlock();
            VoxelShape shape = entity.level.getBlockState(pos).getCollisionShape(entity.level, pos, ISelectionContext.of(entity));

            if(pos.equals(getBlockStandingOnPos(entity)) || (!shape.isEmpty() && (shape.bounds().maxY < (entity.getY()-entity.blockPosition().getY()) || shape.bounds().minY > (entity.getY()-entity.blockPosition().getY()))))
                continue;

            if ((!(block instanceof IColoredBlock) || ((IColoredBlock) block).canClimb()) && entity.level.getBlockEntity(pos) instanceof InkColorTileEntity && ColorUtils.colorEquals(entity, entity.level.getBlockEntity(pos)) && !entity.isPassenger())
                return true;
        }
        return false;
    }

    public static InkBlockUtils.InkType getInkType(LivingEntity entity)
    {
        return PlayerInfoCapability.hasCapability(entity) ? PlayerInfoCapability.get(entity).getInkType() : InkType.NORMAL;
    }

    public static InkType getInkTypeFromStack(ItemStack stack)
    {
        if(!stack.isEmpty())
            for(InkType t : InkType.values.values())
                if(t.getRepItem().equals(stack.getItem()))
                    return t;

        return InkType.NORMAL;
    }

    public static boolean hasInkType(ItemStack stack)
    {
        if(!stack.isEmpty())
            for(InkType t : InkType.values.values())
                if(t.getRepItem().equals(stack.getItem()))
                    return true;
        return false;
    }

    public static InkType getInkType(BlockState state)
    {
        for(InkType type : InkType.values.values())
        {
            if(type.block.equals(state.getBlock()))
                return type;
        }
        return InkType.NORMAL;
    }

    public static class InkType implements Comparable<InkType>, IStringSerializable
    {
        public static final HashMap<ResourceLocation, InkType> values = new HashMap<>();

        public static final InkType NORMAL = new InkType(new ResourceLocation(Splatcraft.MODID, "normal"), SplatcraftBlocks.inkedBlock);
        public static final InkType GLOWING = new InkType(new ResourceLocation(Splatcraft.MODID, "glowing"), SplatcraftItems.splatfestBand, SplatcraftBlocks.glowingInkedBlock);
        public static final InkType CLEAR = new InkType(new ResourceLocation(Splatcraft.MODID, "clear"), SplatcraftItems.clearBand, SplatcraftBlocks.clearInkedBlock);

        private final ResourceLocation name;
        private final Item repItem;

        private final InkedBlock block;

        public InkType(ResourceLocation name, Item repItem, InkedBlock inkedBlock)
        {
            values.put(name, this);
            this.name = name;
            this.repItem = repItem;
            this.block = inkedBlock;
        }

        public InkType(ResourceLocation name, InkedBlock inkedBlock)
        {
            this(name, Items.AIR, inkedBlock);
        }

        @Override
        public int compareTo(InkType o)
        {
            return getName().compareTo(o.getName());
        }

        public ResourceLocation getName()
        {
            return name;
        }

        public Item getRepItem()
        {
            return repItem;
        }

        @Override
        public String toString() {
            return name.toString();
        }

        @Override
        public String getSerializedName() {
            return getName().toString();
        }
    }
}
