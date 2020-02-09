package com.cibernet.splatcraft.items;

import com.cibernet.splatcraft.SplatCraft;
import com.cibernet.splatcraft.entities.classes.EntityInkProjectile;
import com.cibernet.splatcraft.utils.InkColors;
import com.cibernet.splatcraft.utils.SplatCraftPlayerData;
import com.cibernet.splatcraft.utils.SplatCraftUtils;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ItemWeaponBase extends Item
{
	public static List<ItemWeaponBase> weapons = new ArrayList<>();
	
	public static int colIndex = 0;

	public ItemWeaponBase(String unlocName, String registryName)
	{
		setUnlocalizedName(unlocName);
		setRegistryName(registryName);
		setCreativeTab(CreativeTabs.COMBAT);
		setMaxStackSize(1);
		weapons.add(this);
	}
	
	public static int getInkColor(ItemStack stack)
	{
		if(!stack.hasTagCompound() || !stack.getTagCompound().hasKey("color"))
			return InkColors.ORANGE.getColor();
		return stack.getTagCompound().getInteger("color");
	}
	public static boolean isInkLocked(ItemStack stack)
	{
		if(!stack.hasTagCompound() || !stack.getTagCompound().hasKey("colorLocked"))
			return false;
		return stack.getTagCompound().getBoolean("colorLocked");
	}
	
	private static NBTTagCompound checkTagCompound(ItemStack stack) {
		NBTTagCompound tagCompound = stack.getTagCompound();
		if (tagCompound == null) {
			tagCompound = new NBTTagCompound();
			stack.setTagCompound(tagCompound);
		}
		
		return tagCompound;
	}
	
	public static ItemStack setInkColor(ItemStack stack, int color)
	{
		checkTagCompound(stack).setInteger("color", color);
		return stack;
	}
	public static ItemStack setColorLocked(ItemStack stack, boolean colorLocked)
	{
		checkTagCompound(stack).setBoolean("colorLocked", colorLocked);
		return stack;
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
	{
		if(playerIn.isSneaking())
			return EnumActionResult.FAIL;
		
		ItemStack stack = playerIn.getHeldItem(hand);

		SplatCraftUtils.inkBlock(worldIn, pos, getInkColor(stack));
		
		return EnumActionResult.SUCCESS;
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
	{
		
		if(worldIn.isRemote || !playerIn.isSneaking()) return super.onItemRightClick(worldIn, playerIn, handIn);
		
		colIndex++;
		if(colIndex >= InkColors.values().length)
			colIndex = 0;
		
		ItemStack stack = playerIn.getHeldItem(handIn);
		
		setInkColor(stack, InkColors.values()[colIndex].getColor());
		
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}
	
	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected)
	{
		if(isInkLocked(stack) || !(entityIn instanceof EntityPlayer))
			return;
		
		setInkColor(stack, SplatCraftPlayerData.getInkColor((EntityPlayer) entityIn));
		
		super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);
	}
	
	@Override
	public boolean onEntityItemUpdate(EntityItem entityItem)
	{
		
		if(entityItem.world.getBlockState(new BlockPos(entityItem.posX, entityItem.posY-1, entityItem.posZ)).getBlock().equals(Blocks.ANVIL))
		{
			ItemStack stack = entityItem.getItem();
			setInkColor(stack, InkColors.BLUE.getColor());
			setColorLocked(stack, true);
		}
		
		return super.onEntityItemUpdate(entityItem);
	}
	
	public int getMaxItemUseDuration(ItemStack stack)
	{
		return 72000;
	}

	public void onItemTickUse(World worldIn, EntityPlayer playerIn, ItemStack stack, int useTime)
	{
	}

	public float getUseWalkSpeed()
	{
		return 0.5f;
	}

	public void onItemLeftClick(World world, EntityPlayer player, ItemStack stack)
	{
	}
}
