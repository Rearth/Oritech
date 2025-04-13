package rearth.oritech.init;

import dev.architectury.core.block.ArchitecturyLiquidBlock;
import dev.architectury.core.fluid.ArchitecturyFlowingFluid;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import dev.architectury.core.item.ArchitecturyBucketItem;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.wispforest.owo.ui.core.Color;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import rearth.oritech.Oritech;

import java.util.List;

public class FluidContent {
    
    public static final ArchitecturyFluidAttributes OIL_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_OIL, () -> FluidContent.STILL_OIL)
                                                                       .blockSupplier(() -> FluidContent.STILL_OIL_BLOCK)
                                                                       .bucketItemSupplier(() -> FluidContent.STILL_OIL_BUCKET)
                                                                       .sourceTexture(Oritech.id("block/fluid/fluid_gas_dark"))
                                                                       .flowingTexture(Oritech.id("block/fluid/fluid_gas_dark"))
                                                                       .color(new Color(0.478f, 0.478f, 0.478f).argb());
    
    public static final ArchitecturyFluidAttributes FUEL_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_FUEL, () -> FluidContent.STILL_FUEL)
                                                                        .blockSupplier(() -> FluidContent.STILL_FUEL_BLOCK)
                                                                        .bucketItemSupplier(() -> FluidContent.STILL_FUEL_BUCKET)
                                                                        .sourceTexture(Oritech.id("block/fluid/fluid_strange_pale_2"))
                                                                        .flowingTexture(Oritech.id("block/fluid/fluid_strange_pale_2"))
                                                                        .color(new Color(0.176f, 0.239f, 0.282f).argb());

    public static final ArchitecturyFluidAttributes BIOFUEL_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_BIOFUEL, () -> FluidContent.STILL_BIOFUEL)
                                                                        .blockSupplier(() -> FluidContent.STILL_BIOFUEL_BLOCK)
                                                                        .bucketItemSupplier(() -> FluidContent.STILL_BIOFUEL_BUCKET)
                                                                        .sourceTexture(Oritech.id("block/fluid/fluid_strange_pale_2"))
                                                                        .flowingTexture(Oritech.id("block/fluid/fluid_strange_pale_2"))
                                                                        .color(new Color(0.25f, 0.316f, 0.086f).argb());
    
    public static final ArchitecturyFluidAttributes STEAM_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_STEAM, () -> FluidContent.STILL_STEAM)
                                                                         .blockSupplier(() -> FluidContent.STILL_STEAM_BLOCK)
                                                                         .bucketItemSupplier(() -> FluidContent.STILL_STEAM_BUCKET)
                                                                         .sourceTexture(Oritech.id("block/fluid/fluid_steam"))
                                                                         .flowingTexture(Oritech.id("block/fluid/fluid_steam"))
                                                                         .color(Color.WHITE.argb());
    
    public static final ArchitecturyFluidAttributes MOLTEN_ADAMANT_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_MOLTEN_ADAMANT, () -> FluidContent.STILL_MOLTEN_ADAMANT)
                                                                                  .blockSupplier(() -> FluidContent.STILL_MOLTEN_ADAMANT_BLOCK)
                                                                                  .bucketItemSupplier(() -> FluidContent.STILL_MOLTEN_ADAMANT_BUCKET)
                                                                                  .sourceTexture(Oritech.id("block/fluid/molten_metal"))
                                                                                  .flowingTexture(Oritech.id("block/fluid/molten_metal_flow"))
                                                                                  .color(new Color(0.398f, 0.629f, 0.797f).argb());
                                                                                  
    public static final ArchitecturyFluidAttributes MOLTEN_BIOSTEEL_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_MOLTEN_BIOSTEEL, () -> FluidContent.STILL_MOLTEN_BIOSTEEL)
                                                                                  .blockSupplier(() -> FluidContent.STILL_MOLTEN_BIOSTEEL_BLOCK)
                                                                                  .bucketItemSupplier(() -> FluidContent.STILL_MOLTEN_BIOSTEEL_BUCKET)
                                                                                  .sourceTexture(Oritech.id("block/fluid/molten_metal"))
                                                                                  .flowingTexture(Oritech.id("block/fluid/molten_metal_flow"))
                                                                                  .color(new Color(0.145f, 0.344f, 0.176f).argb());

    public static final ArchitecturyFluidAttributes MOLTEN_DURATIUM_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_MOLTEN_DURATIUM, () -> FluidContent.STILL_MOLTEN_DURATIUM)
                                                                                   .blockSupplier(() -> FluidContent.STILL_MOLTEN_DURATIUM_BLOCK)
                                                                                   .bucketItemSupplier(() -> FluidContent.STILL_MOLTEN_DURATIUM_BUCKET)
                                                                                   .sourceTexture(Oritech.id("block/fluid/molten_metal"))
                                                                                   .flowingTexture(Oritech.id("block/fluid/molten_metal_flow"))
                                                                                   .color(new Color(0.254f, 0.176f, 0.360f).argb());

    public static final ArchitecturyFluidAttributes MOLTEN_ENERGITE_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_MOLTEN_ENERGITE, () -> FluidContent.STILL_MOLTEN_ENERGITE)
                                                                                  .blockSupplier(() -> FluidContent.STILL_MOLTEN_ENERGITE_BLOCK)
                                                                                  .bucketItemSupplier(() -> FluidContent.STILL_MOLTEN_ENERGITE_BUCKET)
                                                                                  .sourceTexture(Oritech.id("block/fluid/molten_metal"))
                                                                                  .flowingTexture(Oritech.id("block/fluid/molten_metal_flow"))
                                                                                  .color(new Color(0.879f, 0.300f, 1.0f).argb());

    public static final ArchitecturyFluidAttributes MOLTEN_FLUXITE_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_MOLTEN_FLUXITE, () -> FluidContent.STILL_MOLTEN_FLUXITE)
                                                                                  .blockSupplier(() -> FluidContent.STILL_MOLTEN_FLUXITE_BLOCK)
                                                                                  .bucketItemSupplier(() -> FluidContent.STILL_MOLTEN_FLUXITE_BUCKET)
                                                                                  .sourceTexture(Oritech.id("block/fluid/fluid_strange_pale_2"))
                                                                                  .flowingTexture(Oritech.id("block/fluid/fluid_strange_pale_2"))
                                                                                  .color(new Color(0.453f, 0.195f, 0.648f).argb());

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Oritech.MOD_ID, RegistryKeys.FLUID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Oritech.MOD_ID, RegistryKeys.BLOCK);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Oritech.MOD_ID, RegistryKeys.ITEM);
    public static final List<ArchitecturyFluidAttributes> FLUID_ATTRIBUTES = Platform.isModLoaded("productivemetalworks")
        ? List.of(OIL_ATTRIBUTES, FUEL_ATTRIBUTES, BIOFUEL_ATTRIBUTES, STEAM_ATTRIBUTES, MOLTEN_ADAMANT_ATTRIBUTES, MOLTEN_BIOSTEEL_ATTRIBUTES, MOLTEN_DURATIUM_ATTRIBUTES, MOLTEN_ENERGITE_ATTRIBUTES, MOLTEN_FLUXITE_ATTRIBUTES)
        : List.of(OIL_ATTRIBUTES, FUEL_ATTRIBUTES, BIOFUEL_ATTRIBUTES, STEAM_ATTRIBUTES);
    
    // oil
    public static final RegistrySupplier<FlowableFluid> STILL_OIL = FLUIDS.register("still_oil", () -> new ArchitecturyFlowingFluid.Source(OIL_ATTRIBUTES));
    public static final RegistrySupplier<FlowableFluid> FLOWING_OIL = FLUIDS.register("flowing_oil", () -> new ArchitecturyFlowingFluid.Flowing(OIL_ATTRIBUTES));
    public static final RegistrySupplier<FluidBlock> STILL_OIL_BLOCK = BLOCKS.register("still_oil_block", () -> new ArchitecturyLiquidBlock(STILL_OIL, AbstractBlock.Settings.copy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_OIL_BUCKET = ITEMS.register("still_oil_bucket", () -> new ArchitecturyBucketItem(STILL_OIL, new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET)));
    
    // fuel
    public static final RegistrySupplier<FlowableFluid> STILL_FUEL = FLUIDS.register("still_fuel", () -> new ArchitecturyFlowingFluid.Source(FUEL_ATTRIBUTES));
    public static final RegistrySupplier<FlowableFluid> FLOWING_FUEL = FLUIDS.register("flowing_fuel", () -> new ArchitecturyFlowingFluid.Flowing(FUEL_ATTRIBUTES));
    public static final RegistrySupplier<FluidBlock> STILL_FUEL_BLOCK = BLOCKS.register("still_fuel_block", () -> new ArchitecturyLiquidBlock(STILL_FUEL, AbstractBlock.Settings.copy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_FUEL_BUCKET = ITEMS.register("still_fuel_bucket", () -> new ArchitecturyBucketItem(STILL_FUEL, new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET)));

    // biofuel
    public static final RegistrySupplier<FlowableFluid> STILL_BIOFUEL = FLUIDS.register("still_biofuel", () -> new ArchitecturyFlowingFluid.Source(BIOFUEL_ATTRIBUTES));
    public static final RegistrySupplier<FlowableFluid> FLOWING_BIOFUEL = FLUIDS.register("flowing_biofuel", () -> new ArchitecturyFlowingFluid.Flowing(BIOFUEL_ATTRIBUTES));
    public static final RegistrySupplier<FluidBlock> STILL_BIOFUEL_BLOCK = BLOCKS.register("still_biofuel_block", () -> new ArchitecturyLiquidBlock(STILL_BIOFUEL, AbstractBlock.Settings.copy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_BIOFUEL_BUCKET = ITEMS.register("still_biofuel_bucket", () -> new ArchitecturyBucketItem(STILL_BIOFUEL, new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET)));
    
    // steam
    public static final RegistrySupplier<FlowableFluid> STILL_STEAM = FLUIDS.register("still_steam", () -> new ArchitecturyFlowingFluid.Source(STEAM_ATTRIBUTES));
    public static final RegistrySupplier<FlowableFluid> FLOWING_STEAM = FLUIDS.register("flowing_steam", () -> new ArchitecturyFlowingFluid.Flowing(STEAM_ATTRIBUTES));
    public static final RegistrySupplier<FluidBlock> STILL_STEAM_BLOCK = BLOCKS.register("still_steam_block", () -> new ArchitecturyLiquidBlock(STILL_STEAM, AbstractBlock.Settings.copy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_STEAM_BUCKET = ITEMS.register("still_steam_bucket", () -> new ArchitecturyBucketItem(STILL_STEAM, new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET)));

    // adamant
    public static final RegistrySupplier<FlowableFluid> STILL_MOLTEN_ADAMANT = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("still_molten_adamant", () -> new ArchitecturyFlowingFluid.Source(MOLTEN_ADAMANT_ATTRIBUTES)) : null;
    public static final RegistrySupplier<FlowableFluid> FLOWING_MOLTEN_ADAMANT = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("flowing_molten_adamant", () -> new ArchitecturyFlowingFluid.Flowing(MOLTEN_ADAMANT_ATTRIBUTES)) : null;
    public static final RegistrySupplier<FluidBlock> STILL_MOLTEN_ADAMANT_BLOCK = Platform.isModLoaded("productivemetalworks") ? BLOCKS.register("still_molten_adamant_block", () -> new ArchitecturyLiquidBlock(STILL_MOLTEN_ADAMANT, AbstractBlock.Settings.copy(Blocks.WATER))) : null;
    public static final RegistrySupplier<Item> STILL_MOLTEN_ADAMANT_BUCKET = Platform.isModLoaded("productivemetalworks") ? ITEMS.register("still_molten_adamant_bucket", () -> new ArchitecturyBucketItem(STILL_MOLTEN_ADAMANT, new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET))) : null;

    // biosteel
    public static final RegistrySupplier<FlowableFluid> STILL_MOLTEN_BIOSTEEL = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("still_molten_biosteel", () -> new ArchitecturyFlowingFluid.Source(MOLTEN_BIOSTEEL_ATTRIBUTES)) : null;
    public static final RegistrySupplier<FlowableFluid> FLOWING_MOLTEN_BIOSTEEL = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("flowing_molten_biosteel", () -> new ArchitecturyFlowingFluid.Flowing(MOLTEN_BIOSTEEL_ATTRIBUTES)) : null;
    public static final RegistrySupplier<FluidBlock> STILL_MOLTEN_BIOSTEEL_BLOCK = Platform.isModLoaded("productivemetalworks") ? BLOCKS.register("still_molten_biosteel_block", () -> new ArchitecturyLiquidBlock(STILL_MOLTEN_BIOSTEEL, AbstractBlock.Settings.copy(Blocks.WATER))) : null;
    public static final RegistrySupplier<Item> STILL_MOLTEN_BIOSTEEL_BUCKET = Platform.isModLoaded("productivemetalworks") ? ITEMS.register("still_molten_biosteel_bucket", () -> new ArchitecturyBucketItem(STILL_MOLTEN_BIOSTEEL, new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET))) : null;

    // duratium
    public static final RegistrySupplier<FlowableFluid> STILL_MOLTEN_DURATIUM = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("still_molten_duratium", () -> new ArchitecturyFlowingFluid.Source(MOLTEN_DURATIUM_ATTRIBUTES)) : null;
    public static final RegistrySupplier<FlowableFluid> FLOWING_MOLTEN_DURATIUM = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("flowing_molten_duratium", () -> new ArchitecturyFlowingFluid.Flowing(MOLTEN_DURATIUM_ATTRIBUTES)) : null;
    public static final RegistrySupplier<FluidBlock> STILL_MOLTEN_DURATIUM_BLOCK = Platform.isModLoaded("productivemetalworks") ? BLOCKS.register("still_molten_duratium_block", () -> new ArchitecturyLiquidBlock(STILL_MOLTEN_DURATIUM, AbstractBlock.Settings.copy(Blocks.WATER))) : null;
    public static final RegistrySupplier<Item> STILL_MOLTEN_DURATIUM_BUCKET = Platform.isModLoaded("productivemetalworks") ? ITEMS.register("still_molten_duratium_bucket", () -> new ArchitecturyBucketItem(STILL_MOLTEN_DURATIUM, new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET))) : null;

    // energite
    public static final RegistrySupplier<FlowableFluid> STILL_MOLTEN_ENERGITE = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("still_molten_energite", () -> new ArchitecturyFlowingFluid.Source(MOLTEN_ENERGITE_ATTRIBUTES)) : null;
    public static final RegistrySupplier<FlowableFluid> FLOWING_MOLTEN_ENERGITE = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("flowing_molten_energite", () -> new ArchitecturyFlowingFluid.Flowing(MOLTEN_ENERGITE_ATTRIBUTES)) : null;
    public static final RegistrySupplier<FluidBlock> STILL_MOLTEN_ENERGITE_BLOCK = Platform.isModLoaded("productivemetalworks") ? BLOCKS.register("still_molten_energite_block", () -> new ArchitecturyLiquidBlock(STILL_MOLTEN_ENERGITE, AbstractBlock.Settings.copy(Blocks.WATER))) : null;
    public static final RegistrySupplier<Item> STILL_MOLTEN_ENERGITE_BUCKET = Platform.isModLoaded("productivemetalworks") ? ITEMS.register("still_molten_energite_bucket", () -> new ArchitecturyBucketItem(STILL_MOLTEN_ENERGITE, new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET))) : null;

    // fluxite
    public static final RegistrySupplier<FlowableFluid> STILL_MOLTEN_FLUXITE = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("still_molten_fluxite", () -> new ArchitecturyFlowingFluid.Source(MOLTEN_FLUXITE_ATTRIBUTES)) : null;
    public static final RegistrySupplier<FlowableFluid> FLOWING_MOLTEN_FLUXITE = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("flowing_molten_fluxite", () -> new ArchitecturyFlowingFluid.Flowing(MOLTEN_FLUXITE_ATTRIBUTES)) : null;
    public static final RegistrySupplier<FluidBlock> STILL_MOLTEN_FLUXITE_BLOCK = Platform.isModLoaded("productivemetalworks") ? BLOCKS.register("still_molten_fluxite_block", () -> new ArchitecturyLiquidBlock(STILL_MOLTEN_FLUXITE, AbstractBlock.Settings.copy(Blocks.WATER))) : null;
    public static final RegistrySupplier<Item> STILL_MOLTEN_FLUXITE_BUCKET = Platform.isModLoaded("productivemetalworks") ? ITEMS.register("still_molten_fluxite_bucket", () -> new ArchitecturyBucketItem(STILL_MOLTEN_FLUXITE, new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET))) : null;
    
    public static void registerFluids() {
        FLUIDS.register();
    }
    
    public static void registerBlocks() {
        BLOCKS.register();
    }
    
    public static void registerItems() {
        ITEMS.register();
    }
    
    public static void registerItemsToGroups() {
        ItemGroups.add(ItemContent.Groups.components, STILL_OIL_BUCKET.get());
        ItemGroups.add(ItemContent.Groups.components, STILL_FUEL_BUCKET.get());
        ItemGroups.add(ItemContent.Groups.components, STILL_BIOFUEL_BUCKET.get());
        ItemGroups.add(ItemContent.Groups.components, STILL_STEAM_BUCKET.get());
        if (Platform.isModLoaded("productivemetalworks")) {
            ItemGroups.add(ItemContent.Groups.components, STILL_MOLTEN_ADAMANT_BUCKET.get());
            ItemGroups.add(ItemContent.Groups.components, STILL_MOLTEN_BIOSTEEL_BUCKET.get());
            ItemGroups.add(ItemContent.Groups.components, STILL_MOLTEN_DURATIUM_BUCKET.get());
            ItemGroups.add(ItemContent.Groups.components, STILL_MOLTEN_ENERGITE_BUCKET.get());
            ItemGroups.add(ItemContent.Groups.components, STILL_MOLTEN_FLUXITE_BUCKET.get());
        }
    }
    
}
