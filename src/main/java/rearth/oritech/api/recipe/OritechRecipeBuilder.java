package rearth.oritech.api.recipe;

import com.google.common.base.Optional;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.util.FluidIngredient;

import java.util.ArrayList;
import java.util.List;

public abstract class OritechRecipeBuilder {

    protected final RegistrySupplier<RecipeType<OritechRecipe>> type;
    protected List<Ingredient> inputs;
    protected List<ItemStack> results;
    protected FluidIngredient fluidInput;
    protected List<FluidStack> fluidOutputs;
    protected int time = 200;
    protected float timeMultiplier = 1f;
    protected boolean addToGrinder;
    private final String resourcePath;

    protected OritechRecipeBuilder(RegistrySupplier<RecipeType<OritechRecipe>> type, String resourcePath) {
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
        return input(Ingredient.of(in));
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
    
    public OritechRecipeBuilder specificFluidInput(TagKey<Fluid> in, long amountMillis) {
        return fluidInput(new FluidIngredient().withSpecificAmount(amountMillis).withContent(in));
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

    public void export(RecipeOutput exporter, String suffix, String namespace) {
        
        var id = Identifier.fromNamespaceAndPath(namespace, resourcePath + "/" + suffix);
        validate(id);
        
        exporter.accept(
          id,
          new OritechRecipe(
            inputs != null ? inputs : List.of(),
            results != null ? results : List.of(),
            fluidInput != null ? fluidInput : FluidIngredient.EMPTY,
                        fluidOutputs != null ? fluidOutputs : List.of(),
                        (int)(time * timeMultiplier),
                        type.get()),
          null);
    }
    
    
    public void export(RecipeOutput exporter, String suffix) {
        export(exporter, suffix, Oritech.MOD_ID);
    }
}