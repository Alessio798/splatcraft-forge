package net.splatcraft.forge.items.weapons;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.splatcraft.forge.blocks.ColoredBarrierBlock;
import net.splatcraft.forge.client.audio.RollerRollTickableSound;
import net.splatcraft.forge.client.particles.InkSplashParticleData;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.handlers.PlayerPosingHandler;
import net.splatcraft.forge.handlers.WeaponHandler;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.InkDamageUtils;
import net.splatcraft.forge.util.PlayerCooldown;
import net.splatcraft.forge.util.WeaponStat;

import java.util.ArrayList;

public class RollerItem extends WeaponBaseItem
{
    public static final ArrayList<RollerItem> rollers = Lists.newArrayList();

    public float rollConsumptionMin;
    public float rollConsumptionMax;
    public int dashTime = 1;
    public float rollDamage;
    public int rollSize;

    public double mobility;
    public double dashMobility;
    public double swingMobility;

    public float swingConsumption;
    public float swingDamage;
    public float swingProjectileSpeed;
    public int swingTime;

    public float flingConsumption;
    public float flingDamage;
    public float flingProjectileSpeed;
    public int flingTime;

    public boolean isBrush;

    public RollerItem(String name, int rollSize, float rollConsumption, float rollDamage, double mobility, boolean isBrush)
    {
        super();

        addStat(new WeaponStat("range", (stack, level) -> (int) ((flingProjectileSpeed + swingProjectileSpeed) * 50)));
        addStat(new WeaponStat("ink_speed", (stack, level) -> (int) (dashMobility / 2f * 100)));
        addStat(new WeaponStat("handling", (stack, level) -> (int) ((20 - (flingTime + swingTime) / 2f) * 5)));

        setRegistryName(name);
        rollers.add(this);

        this.rollSize = rollSize;
        this.rollConsumptionMin = rollConsumption;
        this.rollConsumptionMax = rollConsumption;
        this.rollDamage = rollDamage;
        this.mobility = mobility;
        this.dashMobility = mobility;
        this.swingMobility = mobility;
        this.isBrush = isBrush;
    }

    public RollerItem(String name, RollerItem parent)
    {
        this(name, parent.rollSize, parent.rollConsumptionMin, parent.rollDamage, parent.mobility, parent.isBrush);
        setDashStats(parent.dashMobility, parent.rollConsumptionMax, parent.dashTime);
        setSwingStats(parent.swingMobility, parent.swingConsumption, parent.swingDamage, parent.swingProjectileSpeed, parent.swingTime, parent.flingConsumption, parent.flingDamage, parent.flingProjectileSpeed, parent.flingTime);
    }

    public static void applyRecoilKnockback(LivingEntity entity, double pow)
    {
        entity.setDeltaMovement(new Vector3d(Math.cos(Math.toRadians(entity.yRot + 90)) * -pow, entity.getDeltaMovement().y(), Math.sin(Math.toRadians(entity.yRot + 90)) * -pow));
        entity.hurtMarked = true;
    }

    public RollerItem setDashStats(double dashMobility, float rollConsumptionDash, int dashTime)
    {
        this.dashMobility = dashMobility;
        this.rollConsumptionMax = rollConsumptionDash;
        this.dashTime = dashTime;
        return this;
    }

    public RollerItem setSwingStats(double swingMobility, float swingConsumption, float swingDamage, float swingProjectileSpeed, int swingTime, float flingConsumption, float flingDamage, float flingProjectileSpeed, int flingTime)
    {
        this.swingMobility = swingMobility;
        this.swingConsumption = swingConsumption;
        this.swingDamage = swingDamage;
        this.swingProjectileSpeed = swingProjectileSpeed;
        this.swingTime = swingTime;
        this.flingConsumption = flingConsumption;
        this.flingDamage = flingDamage;
        this.flingProjectileSpeed = flingProjectileSpeed;
        this.flingTime = flingTime;
        return this;
    }

    public RollerItem setSwingStats(double swingMobility, float swingConsumption, float swingDamage, float swingProjectileSpeed, int swingTime)
    {
        return setSwingStats(swingMobility, swingConsumption, swingDamage, swingProjectileSpeed, swingTime, swingConsumption, swingDamage, swingProjectileSpeed * (isBrush ? 1 : 1.3f), swingTime);
    }

    public IItemPropertyGetter getUnfolded()
    {
        return (stack, level, entity) ->
        {
            if (entity instanceof PlayerEntity && PlayerCooldown.hasOverloadedPlayerCooldown((PlayerEntity) entity))
            {

                PlayerCooldown cooldown = PlayerCooldown.getPlayerCooldown((PlayerEntity) entity);
                if(cooldown.getTime() > (cooldown.isGrounded() ? -10 : 0))
                {
                    ItemStack cooldownStack = cooldown.getHand().equals(Hand.MAIN_HAND) ? ((PlayerEntity) entity).inventory.items.get(cooldown.getSlotIndex())
                            : entity.getOffhandItem();
                    return stack.equals(cooldownStack) && (isBrush || cooldown.isGrounded()) ? 1 : 0;
                }
            }
            return entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0;
        };
    }

    @Override
    public void weaponUseTick(World level, LivingEntity entity, ItemStack stack, int timeLeft) {
        if (!(entity instanceof PlayerEntity))
            return;

        int startupTicks = entity.isOnGround() ? swingTime : flingTime;
        if (getUseDuration(stack) - timeLeft < startupTicks) {
            //if (getInkAmount(entity, stack) > inkConsumption){
            PlayerCooldown cooldown = new PlayerCooldown(stack, startupTicks, ((PlayerEntity) entity).inventory.selected, entity.getUsedItemHand(), true, false, true, entity.isOnGround());
            PlayerCooldown.setPlayerCooldown((PlayerEntity) entity, cooldown);
            //} else
            if (reduceInk(entity, entity.isOnGround() ? swingConsumption : flingConsumption, timeLeft % 4 == 0) && isBrush) {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.brushFling, SoundCategory.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
                int total = rollSize * 2 + 1;
                for (int i = 0; i < total; i++) {
                    InkProjectileEntity proj = new InkProjectileEntity(level, entity, stack, InkBlockUtils.getInkType(entity), 1.6f,
                            entity.isOnGround() ? swingDamage : flingDamage);
                    proj.setProjectileType(InkProjectileEntity.Types.ROLLER);
                    proj.shootFromRotation(entity, entity.xRot, entity.yRot + (i - total / 2f) * 20, 0, entity.isOnGround() ? swingProjectileSpeed : flingProjectileSpeed, 0.05f);
                    proj.moveTo(proj.getX(), proj.getY() - entity.getEyeHeight() / 2f, proj.getZ());
                    level.addFreshEntity(proj);
                }
            }
        } else
        {
            float toConsume = Math.min(1, (float) (getUseDuration(stack) - timeLeft) / (float) dashTime) * (rollConsumptionMax - rollConsumptionMin) + rollConsumptionMin;
            boolean isMoving = Math.abs(entity.yHeadRotO - entity.yHeadRot) > 0 || (level.isClientSide ? Math.abs(entity.getDeltaMovement().x()) > 0 || Math.abs(entity.getDeltaMovement().z()) > 0
                    : new Vector3d(entity.blockPosition().getX(), entity.blockPosition().getY(), entity.blockPosition().getZ())
                    .multiply(1, 0, 1).distanceTo(WeaponHandler.getPlayerPrevPos((PlayerEntity) entity).multiply(1, 0, 1)) > 0);

            double dxOff = 0;
            double dzOff = 0;
            for(int i = 1; i <= 2; i++)
            {
                dxOff = Math.cos(Math.toRadians(entity.yRot + 90)) * i;
                dzOff = Math.sin(Math.toRadians(entity.yRot + 90)) * i;

                    BlockPos pos = new BlockPos(entity.getX() + dxOff, entity.getY(), entity.getZ() + dzOff);
                    if (!InkBlockUtils.canInkPassthrough(level, pos))
                        break;
            }

            boolean doPush = false;
            if (isMoving)
            {
                for (int i = 0; i < rollSize; i++)
                {
                    double off = (double) i - (rollSize - 1) / 2d;
                    double xOff = Math.cos(Math.toRadians(entity.yRot)) * off;
                    double zOff = Math.sin(Math.toRadians(entity.yRot)) * off;

                    if (enoughInk(entity, toConsume, timeLeft % 4 == 0)) {
                        boolean consumeInk = false;
                        for (float yOff = 0; yOff >= -3; yOff--) {
                            if (yOff == -3) {
                                dxOff = Math.cos(Math.toRadians(entity.yRot + 90));
                                dzOff = Math.sin(Math.toRadians(entity.yRot + 90));
                            }

                            BlockPos pos = new BlockPos(entity.getX() + xOff + dxOff, entity.getY() + yOff, entity.getZ() + zOff + dzOff);

                            if(level.getBlockState(pos).getBlock() instanceof ColoredBarrierBlock && ((ColoredBarrierBlock)level.getBlockState(pos).getBlock()).canAllowThrough(pos, entity))
                                continue;

                            if (!InkBlockUtils.canInkPassthrough(level, pos))
                            {
                                VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);

                                consumeInk = InkBlockUtils.playerInkBlock((PlayerEntity) entity, level, pos, ColorUtils.getInkColor(stack), rollDamage, InkBlockUtils.getInkType(entity));
                                double blockHeight = shape.isEmpty() ? 0 : shape.bounds().maxY;

                                level.addParticle(new InkSplashParticleData(ColorUtils.getInkColor(stack), 1), entity.getX() + xOff + dxOff, pos.getY() + blockHeight + 0.1, entity.getZ() + zOff + dzOff, 0, 0, 0);

                                if(yOff != -3 && !(shape.bounds().minX <= 0 && shape.bounds().minZ <= 0 && shape.bounds().maxX >= 1 && shape.bounds().maxZ >= 1))
                                    consumeInk |= InkBlockUtils.playerInkBlock((PlayerEntity) entity, level, pos.below(), ColorUtils.getInkColor(stack), rollDamage, InkBlockUtils.getInkType(entity));

                                if (i > 0)
                                {
                                    double xhOff = dxOff + Math.cos(Math.toRadians(entity.yRot)) * (off - 0.5);
                                    double zhOff = dzOff + Math.sin(Math.toRadians(entity.yRot)) * (off - 0.5);
                                    level.addParticle(new InkSplashParticleData(ColorUtils.getInkColor(stack), 1), entity.getX() + xhOff, pos.getY() + blockHeight + 0.1, entity.getZ() + zhOff, 0, 0, 0);
                                }
                                break;
                            }
                        }
                        if (consumeInk)
                            reduceInk(entity, Math.min(1, (float) (getUseDuration(stack) - timeLeft) / (float) dashTime) * (rollConsumptionMax - rollConsumptionMin) + rollConsumptionMin, false);
                    }

                    BlockPos attackPos = new BlockPos(entity.getX() + xOff + dxOff, entity.getY() - 1, entity.getZ() + zOff + dzOff);
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(attackPos, attackPos.offset(1, 2, 1)), EntityPredicates.NO_SPECTATORS.and((e) ->
                    {
                        if (e instanceof LivingEntity && ColorUtils.getEntityColor(e) != -1)
                            return InkDamageUtils.canDamageColor(level, ColorUtils.getEntityColor(e), ColorUtils.getInkColor(stack));
                        return true;
                    }))) {
                        if (!target.equals(entity) && !InkDamageUtils.isSplatted(level, target) && InkDamageUtils.canDamage(target, entity) && (!enoughInk(entity, toConsume, false) ||
                                !InkDamageUtils.doRollDamage(level, target, rollDamage, ColorUtils.getInkColor(stack), entity, stack, false, InkBlockUtils.getInkType(entity)))
                            /*&& target.invulnerableTime >= 10*/)
                            doPush = true;
                    }
                }
            }
            if (doPush)
                applyRecoilKnockback(entity, 0.8);
        }
    }

    @Override
    public void onPlayerCooldownEnd(World level, PlayerEntity player, ItemStack stack, PlayerCooldown cooldown)
    {
        boolean airborne = !cooldown.isGrounded();

        if (level.isClientSide)
            playRollSound(isBrush);

        if (reduceInk(player, airborne ? flingConsumption : swingConsumption, false) && !isBrush) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.rollerFling, SoundCategory.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
            for (int i = 0; i < rollSize; i++) {

                InkProjectileEntity proj = new InkProjectileEntity(level, player, stack, InkBlockUtils.getInkType(player), 1.6f, airborne ? flingDamage : swingDamage);
                proj.shootFromRotation(player, player.xRot, player.yRot, airborne ? 0.0f : -67.5f, airborne ? flingProjectileSpeed : swingProjectileSpeed, 0.05f);
                proj.setRollerSwingStats(airborne);
                if (airborne) {
                    double off = (double) i - (rollSize - 1) / 2d;
                    double yOff = Math.sin(Math.toRadians(player.xRot+90));
                    double y2Off = Math.cos(Math.toRadians(player.xRot+90));
                    double xOff = Math.cos(Math.toRadians(player.yRot+90)) * off * y2Off;
                    double zOff = Math.sin(Math.toRadians(player.yRot+90)) * off * y2Off;
                    proj.moveTo(proj.getX() + xOff, proj.getY() + yOff*off, proj.getZ() + zOff);
                }
                else
                {
                    double off = (double) i - (rollSize - 1) / 2d;
                    double xOff = Math.cos(Math.toRadians(player.yRot)) * off;
                    double zOff = Math.sin(Math.toRadians(player.yRot)) * off;
                    proj.moveTo(proj.getX() + xOff, proj.getY() - player.getEyeHeight() / 2f, proj.getZ() + zOff);
                }
                level.addFreshEntity(proj);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected static void playRollSound(boolean isBrush)
    {
        Minecraft.getInstance().getSoundManager().queueTickingSound(new RollerRollTickableSound(Minecraft.getInstance().player, isBrush));
    }

    @Override
    public boolean hasSpeedModifier(LivingEntity entity, ItemStack stack)
    {
        if (entity instanceof PlayerEntity && PlayerCooldown.hasPlayerCooldown((PlayerEntity) entity) || !entity.getUseItem().equals(stack))
            return false;
        return super.hasSpeedModifier(entity, stack);
    }

    @Override
    public AttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
    {
        double appliedMobility;
        int useTime = entity.getUseItemRemainingTicks() - entity.getUseItemRemainingTicks();

        if (enoughInk(entity, Math.min(rollConsumptionMax, rollConsumptionMin), false)) {
            if (entity instanceof PlayerEntity && (PlayerCooldown.hasPlayerCooldown((PlayerEntity) entity)))
                appliedMobility = swingMobility;
            else appliedMobility = Math.min(1, (float) useTime / (float) dashTime) * (dashMobility - mobility) + mobility;
        } else {
            appliedMobility = 0.7;
        }

        return new AttributeModifier(SplatcraftItems.SPEED_MOD_UUID, "Roller Mobility", appliedMobility - 1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public PlayerPosingHandler.WeaponPose getPose()
    {
        return isBrush ? PlayerPosingHandler.WeaponPose.BRUSH : PlayerPosingHandler.WeaponPose.ROLL;
    }
}
