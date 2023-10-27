package net.splatcraft.forge.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.items.weapons.IChargeableWeapon;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.c2s.UpdateChargeStatePacket;

public class PlayerCharge {
    private static final Map<ServerPlayer, Boolean> hasChargeServerPlayerMap = new HashMap<>();

    public ItemStack chargedWeapon;
    public float charge;
    public float prevCharge;
    public int dischargedTicks;
    public int prevDischargedTicks;

    public PlayerCharge(ItemStack stack, float charge) {
        this.chargedWeapon = stack;
        this.charge = charge;
    }

    public static PlayerCharge getCharge(Player player) {
        return PlayerInfoCapability.get(player).getPlayerCharge();
    }

    public static void setCharge(Player player, PlayerCharge charge) {
        PlayerInfoCapability.get(player).setPlayerCharge(charge);
    }

    public static boolean hasCharge(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Attempted to retrieve charge for a null player");
        }

        if (player instanceof ServerPlayer serverPlayer) {
            return hasChargeServerPlayerMap.getOrDefault(serverPlayer, false);
        }

        if (!PlayerInfoCapability.hasCapability(player)) {
            return false;
        }

        PlayerInfo capability = PlayerInfoCapability.get(player);
        return capability.getPlayerCharge() != null && capability.getPlayerCharge().charge > 0;
    }

    public static boolean shouldCreateCharge(Player player) {
        if (player == null) {
            return false;
        }
        PlayerInfo capability = PlayerInfoCapability.get(player);
        return capability.getPlayerCharge() == null;
    }

    public static boolean chargeMatches(Player player, ItemStack stack) {
        return hasCharge(player) && getCharge(player).chargedWeapon.sameItem(stack);
    }

    public static void addChargeValue(Player player, ItemStack stack, float value) {
        if (value < 0.0f) {
            throw new IllegalArgumentException("Attempted to add negative charge: " + value);
        }
        if (shouldCreateCharge(player)) {
            setCharge(player, new PlayerCharge(stack, 0));
        }

        PlayerCharge charge = getCharge(player);
        if (charge.charge <= 0.0f && value > 0.0f) {
            SplatcraftPacketHandler.sendToServer(new UpdateChargeStatePacket(true));
        }

        if (chargeMatches(player, stack)) {
            charge.prevCharge = charge.charge;
            charge.charge = Math.max(0.0f, Math.min(1.0f, charge.charge + value));
            charge.dischargedTicks = 0;
            charge.prevDischargedTicks = 0;
        } else {
            setCharge(player, new PlayerCharge(stack, value));
        }
    }

    public static float getChargeValue(Player player, ItemStack stack) {
        return chargeMatches(player, stack) ? getCharge(player).charge : 0;
    }

    public float getDischargeValue(float partialTicks)
    {
        if(chargedWeapon.getItem() instanceof IChargeableWeapon chargeable)
        {
            float maxDischargeTicks = chargeable.getDischargeTicks(chargedWeapon);
            return maxDischargeTicks <= 0 ? 1 : 1 - Mth.lerp(partialTicks, prevDischargedTicks / maxDischargeTicks, dischargedTicks / maxDischargeTicks);
        }

        return 1;
    }

    public static void dischargeWeapon(Player player) {
        if (!player.level.isClientSide || !hasCharge(player)) {
            return;
        }
        PlayerCharge charge = getCharge(player);
        Item dischargeItem = charge.chargedWeapon.getItem();

        charge.prevDischargedTicks = charge.dischargedTicks;

        if (!(dischargeItem instanceof IChargeableWeapon chargeable)
                || charge.charge < 1.0f
                || charge.dischargedTicks >= chargeable.getDischargeTicks(charge.chargedWeapon)) {
            charge.charge = 0f;
            charge.prevCharge = 0;
            charge.dischargedTicks = 0;
            SplatcraftPacketHandler.sendToServer(new UpdateChargeStatePacket(false));
        } else {
            charge.dischargedTicks++;
        }
    }

    public static void updateServerMap(Player player, boolean hasCharge) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            throw new IllegalStateException("Client attempted to modify server charge map");
        }

        if (hasChargeServerPlayerMap.containsKey(serverPlayer) && hasChargeServerPlayerMap.get(serverPlayer) == hasCharge) {
            throw new IllegalStateException("Charge state did not change: " + hasCharge);
        }

        hasChargeServerPlayerMap.put(serverPlayer, hasCharge);
    }

    public void reset() {
        chargedWeapon = ItemStack.EMPTY;
        charge = 0;
        prevCharge = 0;
        dischargedTicks = 0;
        prevDischargedTicks = 0;
    }

    @Override
    public String toString() {
        return "PlayerCharge: [" + chargedWeapon + " x " + charge + "] (" + super.toString() + ")";
    }
}
