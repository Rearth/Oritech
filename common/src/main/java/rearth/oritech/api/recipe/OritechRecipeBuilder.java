package rearth.oritech.api.recipe;

import dev.architectury.hooks.fluid.FluidStackHooks;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.util.FluidIngredient;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

import dev.architectury.fluid.FluidStack;

public abstract class OritechRecipeBuilder {

    protected final OritechRecipeType type;
    protected List<Ingredient> inputs;
    protected List<ItemStack> results;
    protected FluidIngredient fluidInput;
    protected List<FluidStack> fluidOutputs;
    protected int time = 200;
    protected float timeMultiplier = 1f;
    protected boolean addToGrinder;
    private final String resourcePath;

    protected OritechRecipeBuilder(OritechRecipeType type, String resourcePath) {
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

    public OritechRecipeBuilder input(ItemConvertible in) {
        return input(Ingredient.ofItems(in));
    }

    public OritechRecipeBuilder input(TagKey<Item> in) {
        return input(Ingredient.fromTag(in));
    }

    public OritechRecipeBuilder fluidInput(FluidIngredient in) {
        fluidInput = in;
        return this;
    }
    
    public OritechRecipeBuilder fluidInput(Fluid in, float bucketAmount) {
        return fluidInput(new FluidIngredient().withContent(in).withAmount(bucketAmount));
    }
    
    public OritechRecipeBuilder specificFluidInput(Fluid in, long amountMillis) {
        return fluidInput(new FluidIngredient().withContent(in).withSpecificAmount(amountMillis));
    }

    public OritechRecipeBuilder fluidInput(Fluid in) {
        return fluidInput(in, 1.0f);
    }

    public OritechRecipeBuilder fluidInput(TagKey<Fluid> in) {
        return fluidInput(in, 1.0f);
    }

    public OritechRecipeBuilder fluidInput(TagKey<Fluid> in, float bucketAmount)  {
        return fluidInput(new FluidIngredient().withContent(in).withAmount(bucketAmount));
    }

    public OritechRecipeBuilder fluidOutput(FluidStack out) {
        fluidOutputs.add(out);
        return this;
    }

    public OritechRecipeBuilder fluidOutput(Fluid out, float bucketAmount) {
        return fluidOutput(FluidStack.create(out, (long)(bucketAmount * FluidStackHooks.bucketAmount())));
    }

    public OritechRecipeBuilder fluidOutput(Fluid out) {
        return fluidOutput(FluidStack.create(out, FluidStackHooks.bucketAmount()));
    }

    public OritechRecipeBuilder result(ItemStack out) {
        if (results == null)
            results = new ArrayList<>();
        results.add(out);
        return this;
    }

    public OritechRecipeBuilder result(List<ItemStack> out) {
        if (results == null)
            results = new ArrayList<>();
        results.addAll(out);
        return this;
    }

    public OritechRecipeBuilder result(Item out, int count) {

        return result(new ItemStack(out, count));
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

    public void export(RecipeExporter exporter, String suffix) {
        var id = Oritech.id(resourcePath + "/" + suffix);
        validate(id);
        
        exporter.accept(
            id,
            new OritechRecipe(
                (int)(time * timeMultiplier),
                inputs != null ? inputs : List.of(),
                results != null ? results : List.of(),
                type,
                fluidInput != null ? fluidInput : FluidIngredient.EMPTY,
                fluidOutputs != null ? fluidOutputs : List.of()),
            null);
    }
}