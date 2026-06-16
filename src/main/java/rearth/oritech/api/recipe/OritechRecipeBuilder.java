package rearth.oritech.api.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.OritechRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class OritechRecipeBuilder {

    protected final Supplier<RecipeType<OritechRecipe>> type;
    protected List<Ingredient> inputs;
    protected List<ItemStackTemplate> results;
    protected SizedFluidIngredient fluidInput;
    protected List<FluidStack> fluidOutputs;
    protected int time = 200;
    protected float timeMultiplier = 1f;
    protected boolean addToGrinder;
    private final String resourcePath;

    protected OritechRecipeBuilder(Supplier<RecipeType<OritechRecipe>> type, String resourcePath) {
        this.type = type;
        this.resourcePath = resourcePath;
        this.fluidOutputs = new ArrayList<>();
    }

    public OritechRecipeBuilder input(List<Ingredient> in) {
        if (inputs == null)
            inputs = new ArrayList<>();
        inputs.addAll(in);
        return this;
    }

    public OritechRecipeBuilder input(Ingredient in) {
        if (inputs == null)
            inputs = new ArrayList<>();
        inputs.add(in);
        return this;
    }

    public OritechRecipeBuilder input(ItemLike in) {
        return input(Ingredient.of(in));
    }

    public OritechRecipeBuilder input(TagKey<Item> in) {
        return input(Ingredient.of(BuiltInRegistries.ITEM.get(in).orElseThrow()));
    }

    public OritechRecipeBuilder fluidInput(SizedFluidIngredient in) {
        fluidInput = in;
        return this;
    }

    public OritechRecipeBuilder fluidInput(Fluid in, float bucketAmount) {
        return fluidInput(SizedFluidIngredient.of(in, (int) (bucketAmount * FluidType.BUCKET_VOLUME)));
    }

    public OritechRecipeBuilder specificFluidInput(Fluid in, int amountMillis) {
        return fluidInput(SizedFluidIngredient.of(in, amountMillis));
    }

    public OritechRecipeBuilder specificFluidInput(TagKey<Fluid> in, int amountMillis) {
        return fluidInput(new SizedFluidIngredient(FluidIngredient.of(BuiltInRegistries.FLUID.get(in).orElseThrow()), amountMillis));
    }

    public OritechRecipeBuilder fluidInput(Fluid in) {
        return fluidInput(in, 1.0f);
    }

    public OritechRecipeBuilder fluidInput(TagKey<Fluid> in) {
        return fluidInput(in, 1.0f);
    }

    public OritechRecipeBuilder fluidInput(TagKey<Fluid> in, float bucketAmount) {
        return fluidInput(new SizedFluidIngredient(FluidIngredient.of(BuiltInRegistries.FLUID.get(in).orElseThrow()), (int) (bucketAmount * FluidType.BUCKET_VOLUME)));
    }

    public OritechRecipeBuilder fluidOutput(FluidStack out) {
        fluidOutputs.add(out);
        return this;
    }

    public OritechRecipeBuilder fluidOutput(Fluid out, float bucketAmount) {
        return fluidOutput(new FluidStack(out, (int) (bucketAmount * FluidType.BUCKET_VOLUME)));
    }

    public OritechRecipeBuilder fluidOutput(Fluid out) {
        return fluidOutput(new FluidStack(out, FluidType.BUCKET_VOLUME));
    }

    public OritechRecipeBuilder result(ItemStackTemplate out) {
        if (results == null)
            results = new ArrayList<>();
        results.add(out);
        return this;
    }

    public OritechRecipeBuilder result(ItemStack out) {
        return result(ItemStackTemplate.fromNonEmptyStack(out));
    }

    public OritechRecipeBuilder result(List<ItemStack> out) {
        for (var stack : out) {
            result(stack);
        }
        return this;
    }

    public OritechRecipeBuilder result(Item out, int count) {
        return result(new ItemStackTemplate(out, count));
    }

    public OritechRecipeBuilder result(Item out) {
        return result(out, 1);
    }

    public OritechRecipeBuilder result(Optional<Item> out, int count) {
        if (out.isPresent())
            return result(out.get(), count);
        return this;
    }

    public OritechRecipeBuilder result(Optional<Item> out) {
        return result(out, 1);
    }

    public OritechRecipeBuilder time(int time) {
        this.time = time;
        return this;
    }

    public OritechRecipeBuilder timeInSeconds(int time) {
        return time(time * 20);
    }

    public OritechRecipeBuilder timeMultiplier(float timeMultiplier) {
        this.timeMultiplier = timeMultiplier;
        return this;
    }

    public OritechRecipeBuilder addToGrinder() {
        this.addToGrinder = true;
        return this;
    }

    public abstract void validate(Identifier id) throws IllegalStateException;

    public void export(RecipeOutput exporter, String suffix, String namespace) {

        var id = Identifier.fromNamespaceAndPath(namespace, resourcePath + "/" + suffix);
        validate(id);

        exporter.accept(
                ResourceKey.create(Registries.RECIPE, id),
                new OritechRecipe(
                        inputs != null ? inputs : List.of(),
                        results != null ? results : List.of(),
                        fluidInput != null ? fluidInput : SizedFluidIngredient.of(Fluids.EMPTY, 0),
                        fluidOutputs != null ? fluidOutputs : List.of(),
                        (int) (time * timeMultiplier),
                        type.get()),
                null);
    }


    public void export(RecipeOutput exporter, String suffix) {
        export(exporter, suffix, Oritech.MOD_ID);
    }
}
