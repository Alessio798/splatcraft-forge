package net.splatcraft.forge.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WeaponWorkbenchRecipe implements IRecipe<IInventory>, Comparable<WeaponWorkbenchRecipe>
{
    protected final ResourceLocation id;
    protected final ResourceLocation tab;
    protected final List<WeaponWorkbenchSubtypeRecipe> subRecipes;
    protected final int pos;

    public WeaponWorkbenchRecipe(ResourceLocation id, ResourceLocation tab, int pos, List<WeaponWorkbenchSubtypeRecipe> subRecipes)
    {
        this.id = id;
        this.pos = pos;
        this.tab = tab;
        this.subRecipes = subRecipes;
    }

    @Override
    public boolean matches(IInventory inv, World levelIn)
    {
        return true;
    }

    @Override
    public ItemStack assemble(IInventory inv)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return false;
    }

    @Override
    public ItemStack getResultItem()
    {
        return subRecipes.isEmpty() ? ItemStack.EMPTY : subRecipes.get(0).getOutput().copy();
    }

    @Override
    public ResourceLocation getId()
    {
        return id;
    }

    @Override
    public IRecipeSerializer<?> getSerializer()
    {
        return SplatcraftRecipeTypes.WEAPON_STATION;
    }

    @Override
    public IRecipeType<?> getType()
    {
        return SplatcraftRecipeTypes.WEAPON_STATION_TYPE;
    }

    @Override
    public int compareTo(WeaponWorkbenchRecipe o)
    {
        return pos - o.pos;
    }

    public WeaponWorkbenchTab getTab(World level)
    {
        return (WeaponWorkbenchTab) level.getRecipeManager().byKey(tab).get();
    }

    public WeaponWorkbenchSubtypeRecipe getRecipeFromIndex(int subTypePos)
    {
        return subRecipes.get(subTypePos);
    }

    public int getTotalRecipes()
    {
        return subRecipes.size();
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<WeaponWorkbenchRecipe>
    {

        public Serializer(String name)
        {
            super();
            setRegistryName(name);
        }

        @Override
        public WeaponWorkbenchRecipe fromJson(ResourceLocation recipeId, JsonObject json)
        {
            List<WeaponWorkbenchSubtypeRecipe> recipes = new ArrayList<>();
            JsonArray arr = json.getAsJsonArray("recipes");

            for (int i = 0; i < arr.size(); i++)
            {
                ResourceLocation id = new ResourceLocation(recipeId.getNamespace(), recipeId.getPath() + "subtype" + i);
                recipes.add(WeaponWorkbenchSubtypeRecipe.fromJson(id, arr.get(i).getAsJsonObject()));
            }

            return new WeaponWorkbenchRecipe(recipeId, new ResourceLocation(JSONUtils.getAsString(json, "tab")), json.has("pos") ? JSONUtils.getAsInt(json, "pos") : Integer.MAX_VALUE, recipes);
        }

        @Nullable
        @Override
        public WeaponWorkbenchRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer)
        {
            List<WeaponWorkbenchSubtypeRecipe> s = new ArrayList<>();
            int count = buffer.readInt();
            for (int i = 0; i < count; i++)
            {
                ResourceLocation loc = buffer.readResourceLocation();
                s.add(WeaponWorkbenchSubtypeRecipe.fromBuffer(loc, buffer));
            }

            ResourceLocation loc = buffer.readResourceLocation();

            return new WeaponWorkbenchRecipe(recipeId, loc, buffer.readInt(), s);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, WeaponWorkbenchRecipe recipe)
        {
            buffer.writeInt(recipe.subRecipes.size());

            for (WeaponWorkbenchSubtypeRecipe s : recipe.subRecipes)
            {
                buffer.writeResourceLocation(s.id);
                s.toBuffer(buffer);
            }

            buffer.writeResourceLocation(recipe.tab);
            buffer.writeInt(recipe.pos);
        }
    }
}
