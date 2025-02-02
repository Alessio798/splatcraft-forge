package net.splatcraft.forge.network.s2c;

import net.minecraft.network.PacketBuffer;
import net.minecraft.world.GameRules;
import net.splatcraft.forge.registries.SplatcraftGameRules;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class UpdateBooleanGamerulesPacket extends PlayToClientPacket
{
    public TreeMap<Integer, Boolean> booleanRules;

    public UpdateBooleanGamerulesPacket(TreeMap<Integer, Boolean> booleanRules)
    {
        this.booleanRules = booleanRules;
    }

    public UpdateBooleanGamerulesPacket(GameRules.RuleKey<GameRules.BooleanValue> rule, boolean value)
    {
        this.booleanRules = new TreeMap<Integer, Boolean>()
        {{
            put(SplatcraftGameRules.getRuleIndex(rule), value);
        }};
    }

    public static UpdateBooleanGamerulesPacket decode(PacketBuffer buffer)
    {
        TreeMap<Integer, Boolean> booleanRules = new TreeMap<>();
        int entrySize = buffer.readInt();

        for (int i = 0; i < entrySize; i++)
        {
            booleanRules.put(buffer.readInt(), buffer.readBoolean());
        }

        return new UpdateBooleanGamerulesPacket(booleanRules);
    }

    @Override
    public void encode(PacketBuffer buffer)
    {
        Set<Map.Entry<Integer, Boolean>> entrySet = booleanRules.entrySet();

        buffer.writeInt(entrySet.size());

        for (Map.Entry<Integer, Boolean> rule : entrySet)
        {
            buffer.writeInt(rule.getKey());
            buffer.writeBoolean(rule.getValue());
        }
    }

    @Override
    public void execute()
    {
        for (Map.Entry<Integer, Boolean> rule : booleanRules.entrySet())
        {
            SplatcraftGameRules.booleanRules.put(rule.getKey(), rule.getValue());
        }
    }

}
