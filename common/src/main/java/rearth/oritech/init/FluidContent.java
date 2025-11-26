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
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import rearth.oritech.Oritech;
import rearth.oritech.block.fluid.SheolFireFluidBlock;

import java.util.List;

public class FluidContent {
    
    // fluid usage:
    /*  (crude oil = oil)
    crude oil -> heavy oil, light naphtha, sulfuric acid
    crude oil + clay catalyst beads -> light naphtha, diesel, sulfuric acid
    heavy oil + sand -> diesel, light naphtha, sulfuric acid
    
    lava -> steam, sulfuric acid, sheol fire
    lava + enderic compound -> sulfuric acid, sheol fire, strange matter
    
    biofuel + clay catalyst beads -> diesel, light naphtha
    
    new fluids:
    - crude oil (existing oil):
      - burns very shortly in fuel generator
      - used in refinery
    - heavy oil: same as above
    - diesel:
      - fuel generator fuel
      - can be augmented to turbofuel
    - light naphtha:
      - fuel generator fuel
      - used with clay catalyst in centrifuge for polymer resin (or as inefficient alt in crafting table)
      - used in centrifuge with raw silicon for silicon wash fluid
    - sulfuric acid:
      - used in centrifuge for high yield ore washing. Results in Mineral Slurry fluid
      - used in centrifuge to fill batteries
    - silicon wash:
      - used as alt to create processing units
      - used in refinery to produce silicon (combined with sand)
    - mineral slurry:
      - used in centrifuge to create quartz
      - used as fertilizer
    - sheol fire:
      - burns in lava generator for a very long time
      - used in refinery with raw ores to process into very high yielded clumps
      - used in item creation? todo
    - strange matter:
      - used to "fill" dubious containers in centrifuge
      
    new items:
    - reinforced carbon sheeting: new machine plating type? Dark.
    - hyper-tensile filaments: used for advanced chips / ai chips
    - ion thruster: used in particle accelerator motor, jetpacks, augments?
    - clay catalyst beads: used to augment refinery recipes in some cases
    
    new crafts:
    - clay catalyst beads: crafted/assembled. Made from sand and clay, high result counts. done
    - battery / adv battery in centrifuge with sulfuric acid. done
    - reinforced carbon plating: made in refinery from light naphtha. Used as netherite replacement in some stuff? Used for carbon plating block. done
    - ion thruster from reinforced carbon sheeting, advanced battery and flux gate. done.
    
    // open concepts:
    - processing involving uranium
    - something with yeast / potatoes?
     
     */
    
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
                                                                         .lighterThanAir(true)
                                                                         .color(Color.WHITE.argb());
    
    public static final ArchitecturyFluidAttributes HEAVY_OIL_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_HEAVY_OIL, () -> FluidContent.STILL_HEAVY_OIL)
                                                                             .blockSupplier(() -> FluidContent.STILL_HEAVY_OIL_BLOCK)
                                                                             .bucketItemSupplier(() -> FluidContent.STILL_HEAVY_OIL_BUCKET)
                                                                             .sourceTexture(Oritech.id("block/fluid/fluid_molten"))
                                                                             .flowingTexture(Oritech.id("block/fluid/fluid_molten"))
                                                                             .color(new Color(0.135f, 0.135f, 0.135f).argb());
    
    public static final ArchitecturyFluidAttributes DIESEL_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_DIESEL, () -> FluidContent.STILL_DIESEL)
                                                                          .blockSupplier(() -> FluidContent.STILL_DIESEL_BLOCK)
                                                                          .bucketItemSupplier(() -> FluidContent.STILL_DIESEL_BUCKET)
                                                                          .sourceTexture(Oritech.id("block/fluid/fluid_steam"))
                                                                          .flowingTexture(Oritech.id("block/fluid/fluid_steam"))
                                                                          .color(new Color(0.735f, 0.735f, 0.235f).argb());
    
    public static final ArchitecturyFluidAttributes NAPHTHA_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_NAPHTHA, () -> FluidContent.STILL_NAPHTHA)
                                                                           .blockSupplier(() -> FluidContent.STILL_NAPHTHA_BLOCK)
                                                                           .bucketItemSupplier(() -> FluidContent.STILL_NAPHTHA_BUCKET)
                                                                           .sourceTexture(Oritech.id("block/fluid/fluid_molten"))
                                                                           .flowingTexture(Oritech.id("block/fluid/fluid_molten"))
                                                                           .color(new Color(0.949f, 0.929f, 0.745f).argb());
    
    public static final ArchitecturyFluidAttributes SULFURIC_ACID_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_SULFURIC_ACID, () -> FluidContent.STILL_SULFURIC_ACID)
                                                                                 .blockSupplier(() -> FluidContent.STILL_SULFURIC_ACID_BLOCK)
                                                                                 .bucketItemSupplier(() -> FluidContent.STILL_SULFURIC_ACID_BUCKET)
                                                                                 .sourceTexture(Oritech.id("block/fluid/fluid_steam"))
                                                                                 .flowingTexture(Oritech.id("block/fluid/fluid_steam"))
                                                                                 .color(new Color(0.398f, 1, 0.3f).argb());
    
    public static final ArchitecturyFluidAttributes SILICON_WASH_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_SILICON_WASH, () -> FluidContent.STILL_SILICON_WASH)
                                                                                .blockSupplier(() -> FluidContent.STILL_SILICON_WASH_BLOCK)
                                                                                .bucketItemSupplier(() -> FluidContent.STILL_SILICON_WASH_BUCKET)
                                                                                .sourceTexture(Oritech.id("block/fluid/fluid_steam"))
                                                                                .flowingTexture(Oritech.id("block/fluid/fluid_steam"))
                                                                                .color(new Color(0.7f, 1f, 0.7f).argb());
    
    public static final ArchitecturyFluidAttributes MINERAL_SLURRY_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_MINERAL_SLURRY, () -> FluidContent.STILL_MINERAL_SLURRY)
                                                                                  .blockSupplier(() -> FluidContent.STILL_MINERAL_SLURRY_BLOCK)
                                                                                  .bucketItemSupplier(() -> FluidContent.STILL_MINERAL_SLURRY_BUCKET)
                                                                                  .sourceTexture(Oritech.id("block/fluid/molten_metal"))
                                                                                  .flowingTexture(Oritech.id("block/fluid/molten_metal"))
                                                                                  .color(new Color(0.627f, 0.849f, 1f).argb());
    
    public static final ArchitecturyFluidAttributes SHEOL_FIRE_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_SHEOL_FIRE, () -> FluidContent.STILL_SHEOL_FIRE)
                                                                              .blockSupplier(() -> FluidContent.STILL_SHEOL_FIRE_BLOCK)
                                                                              .bucketItemSupplier(() -> FluidContent.STILL_SHEOL_FIRE_BUCKET)
                                                                              .sourceTexture(Oritech.id("block/fluid/fluid_roiling_plasma"))
                                                                              .flowingTexture(Oritech.id("block/fluid/fluid_roiling_plasma"))
                                                                              .color(new Color(1, 0.7f, 0.7f).argb());
    
    public static final ArchitecturyFluidAttributes STRANGE_MATTER_ATTRIBUTES = SimpleArchitecturyFluidAttributes.ofSupplier(() -> FluidContent.FLOWING_STRANGE_MATTER, () -> FluidContent.STILL_STRANGE_MATTER)
                                                                                  .blockSupplier(() -> FluidContent.STILL_STRANGE_MATTER_BLOCK)
                                                                                  .bucketItemSupplier(() -> FluidContent.STILL_STRANGE_MATTER_BUCKET)
                                                                                  .sourceTexture(Oritech.id("block/fluid/fluid_strange_mixture"))
                                                                                  .flowingTexture(Oritech.id("block/fluid/fluid_strange_mixture"))
                                                                                  .color(new Color(1, 1, 1).argb());
    
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
    
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Oritech.MOD_ID, Registries.FLUID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Oritech.MOD_ID, Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Oritech.MOD_ID, Registries.ITEM);
    
    public static final List<ArchitecturyFluidAttributes> FLUID_ATTRIBUTES = Platform.isModLoaded("productivemetalworks")
                                                                               ? List.of(OIL_ATTRIBUTES, FUEL_ATTRIBUTES, BIOFUEL_ATTRIBUTES, STEAM_ATTRIBUTES, DIESEL_ATTRIBUTES, HEAVY_OIL_ATTRIBUTES, NAPHTHA_ATTRIBUTES, SULFURIC_ACID_ATTRIBUTES, SILICON_WASH_ATTRIBUTES, MINERAL_SLURRY_ATTRIBUTES, SHEOL_FIRE_ATTRIBUTES, STRANGE_MATTER_ATTRIBUTES, MOLTEN_ADAMANT_ATTRIBUTES, MOLTEN_BIOSTEEL_ATTRIBUTES, MOLTEN_DURATIUM_ATTRIBUTES, MOLTEN_ENERGITE_ATTRIBUTES, MOLTEN_FLUXITE_ATTRIBUTES)
                                                                               : List.of(OIL_ATTRIBUTES, FUEL_ATTRIBUTES, BIOFUEL_ATTRIBUTES, STEAM_ATTRIBUTES, DIESEL_ATTRIBUTES, HEAVY_OIL_ATTRIBUTES, NAPHTHA_ATTRIBUTES, SULFURIC_ACID_ATTRIBUTES, SILICON_WASH_ATTRIBUTES, MINERAL_SLURRY_ATTRIBUTES, SHEOL_FIRE_ATTRIBUTES, STRANGE_MATTER_ATTRIBUTES);
    
    // oil
    public static final RegistrySupplier<FlowingFluid> STILL_OIL = FLUIDS.register("still_oil", () -> cast(new ArchitecturyFlowingFluid.Source(OIL_ATTRIBUTES)));
    public static final RegistrySupplier<FlowingFluid> FLOWING_OIL = FLUIDS.register("flowing_oil", () -> cast(new ArchitecturyFlowingFluid.Flowing(OIL_ATTRIBUTES)));
    public static final RegistrySupplier<LiquidBlock> STILL_OIL_BLOCK = BLOCKS.register("still_oil_block", () -> new ArchitecturyLiquidBlock(STILL_OIL, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_OIL_BUCKET = ITEMS.register("still_oil_bucket", () -> new ArchitecturyBucketItem(STILL_OIL, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    
    // fuel
    public static final RegistrySupplier<FlowingFluid> STILL_FUEL = FLUIDS.register("still_fuel", () -> cast(new ArchitecturyFlowingFluid.Source(FUEL_ATTRIBUTES)));
    public static final RegistrySupplier<FlowingFluid> FLOWING_FUEL = FLUIDS.register("flowing_fuel", () -> cast(new ArchitecturyFlowingFluid.Flowing(FUEL_ATTRIBUTES)));
    public static final RegistrySupplier<LiquidBlock> STILL_FUEL_BLOCK = BLOCKS.register("still_fuel_block", () -> new ArchitecturyLiquidBlock(STILL_FUEL, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_FUEL_BUCKET = ITEMS.register("still_fuel_bucket", () -> new ArchitecturyBucketItem(STILL_FUEL, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    
    // biofuel
    public static final RegistrySupplier<FlowingFluid> STILL_BIOFUEL = FLUIDS.register("still_biofuel", () -> cast(new ArchitecturyFlowingFluid.Source(BIOFUEL_ATTRIBUTES)));
    public static final RegistrySupplier<FlowingFluid> FLOWING_BIOFUEL = FLUIDS.register("flowing_biofuel", () -> cast(new ArchitecturyFlowingFluid.Flowing(BIOFUEL_ATTRIBUTES)));
    public static final RegistrySupplier<LiquidBlock> STILL_BIOFUEL_BLOCK = BLOCKS.register("still_biofuel_block", () -> new ArchitecturyLiquidBlock(STILL_BIOFUEL, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_BIOFUEL_BUCKET = ITEMS.register("still_biofuel_bucket", () -> new ArchitecturyBucketItem(STILL_BIOFUEL, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    
    // steam
    public static final RegistrySupplier<FlowingFluid> STILL_STEAM = FLUIDS.register("still_steam", () -> cast(new ArchitecturyFlowingFluid.Source(STEAM_ATTRIBUTES)));
    public static final RegistrySupplier<FlowingFluid> FLOWING_STEAM = FLUIDS.register("flowing_steam", () -> cast(new ArchitecturyFlowingFluid.Flowing(STEAM_ATTRIBUTES)));
    public static final RegistrySupplier<LiquidBlock> STILL_STEAM_BLOCK = BLOCKS.register("still_steam_block", () -> new ArchitecturyLiquidBlock(STILL_STEAM, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_STEAM_BUCKET = ITEMS.register("still_steam_bucket", () -> new ArchitecturyBucketItem(STILL_STEAM, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    
    // heavy oil
    public static final RegistrySupplier<FlowingFluid> STILL_HEAVY_OIL = FLUIDS.register("still_heavy_oil", () -> cast(new ArchitecturyFlowingFluid.Source(HEAVY_OIL_ATTRIBUTES)));
    public static final RegistrySupplier<FlowingFluid> FLOWING_HEAVY_OIL = FLUIDS.register("flowing_heavy_oil", () -> cast(new ArchitecturyFlowingFluid.Flowing(HEAVY_OIL_ATTRIBUTES)));
    public static final RegistrySupplier<LiquidBlock> STILL_HEAVY_OIL_BLOCK = BLOCKS.register("still_heavy_oil_block", () -> new ArchitecturyLiquidBlock(STILL_HEAVY_OIL, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_HEAVY_OIL_BUCKET = ITEMS.register("still_heavy_oil_bucket", () -> new ArchitecturyBucketItem(STILL_HEAVY_OIL, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    
    // diesel
    public static final RegistrySupplier<FlowingFluid> STILL_DIESEL = FLUIDS.register("still_diesel", () -> cast(new ArchitecturyFlowingFluid.Source(DIESEL_ATTRIBUTES)));
    public static final RegistrySupplier<FlowingFluid> FLOWING_DIESEL = FLUIDS.register("flowing_diesel", () -> cast(new ArchitecturyFlowingFluid.Flowing(DIESEL_ATTRIBUTES)));
    public static final RegistrySupplier<LiquidBlock> STILL_DIESEL_BLOCK = BLOCKS.register("still_diesel_block", () -> new ArchitecturyLiquidBlock(STILL_DIESEL, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_DIESEL_BUCKET = ITEMS.register("still_diesel_bucket", () -> new ArchitecturyBucketItem(STILL_DIESEL, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    
    // naphtha
    public static final RegistrySupplier<FlowingFluid> STILL_NAPHTHA = FLUIDS.register("still_naphtha", () -> cast(new ArchitecturyFlowingFluid.Source(NAPHTHA_ATTRIBUTES)));
    public static final RegistrySupplier<FlowingFluid> FLOWING_NAPHTHA = FLUIDS.register("flowing_naphtha", () -> cast(new ArchitecturyFlowingFluid.Flowing(NAPHTHA_ATTRIBUTES)));
    public static final RegistrySupplier<LiquidBlock> STILL_NAPHTHA_BLOCK = BLOCKS.register("still_naphtha_block", () -> new ArchitecturyLiquidBlock(STILL_NAPHTHA, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_NAPHTHA_BUCKET = ITEMS.register("still_naphtha_bucket", () -> new ArchitecturyBucketItem(STILL_NAPHTHA, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    
    // sulfuric acid
    public static final RegistrySupplier<FlowingFluid> STILL_SULFURIC_ACID = FLUIDS.register("still_sulfuric_acid", () -> cast(new ArchitecturyFlowingFluid.Source(SULFURIC_ACID_ATTRIBUTES)));
    public static final RegistrySupplier<FlowingFluid> FLOWING_SULFURIC_ACID = FLUIDS.register("flowing_sulfuric_acid", () -> cast(new ArchitecturyFlowingFluid.Flowing(SULFURIC_ACID_ATTRIBUTES)));
    public static final RegistrySupplier<LiquidBlock> STILL_SULFURIC_ACID_BLOCK = BLOCKS.register("still_sulfuric_acid_block", () -> new ArchitecturyLiquidBlock(STILL_SULFURIC_ACID, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_SULFURIC_ACID_BUCKET = ITEMS.register("still_sulfuric_acid_bucket", () -> new ArchitecturyBucketItem(STILL_SULFURIC_ACID, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    
    // silicon wash
    public static final RegistrySupplier<FlowingFluid> STILL_SILICON_WASH = FLUIDS.register("still_silicon_wash", () -> cast(new ArchitecturyFlowingFluid.Source(SILICON_WASH_ATTRIBUTES)));
    public static final RegistrySupplier<FlowingFluid> FLOWING_SILICON_WASH = FLUIDS.register("flowing_silicon_wash", () -> cast(new ArchitecturyFlowingFluid.Flowing(SILICON_WASH_ATTRIBUTES)));
    public static final RegistrySupplier<LiquidBlock> STILL_SILICON_WASH_BLOCK = BLOCKS.register("still_silicon_wash_block", () -> new ArchitecturyLiquidBlock(STILL_SILICON_WASH, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_SILICON_WASH_BUCKET = ITEMS.register("still_silicon_wash_bucket", () -> new ArchitecturyBucketItem(STILL_SILICON_WASH, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    
    // mineral slurry
    public static final RegistrySupplier<FlowingFluid> STILL_MINERAL_SLURRY = FLUIDS.register("still_mineral_slurry", () -> cast(new ArchitecturyFlowingFluid.Source(MINERAL_SLURRY_ATTRIBUTES)));
    public static final RegistrySupplier<FlowingFluid> FLOWING_MINERAL_SLURRY = FLUIDS.register("flowing_mineral_slurry", () -> cast(new ArchitecturyFlowingFluid.Flowing(MINERAL_SLURRY_ATTRIBUTES)));
    public static final RegistrySupplier<LiquidBlock> STILL_MINERAL_SLURRY_BLOCK = BLOCKS.register("still_mineral_slurry_block", () -> new ArchitecturyLiquidBlock(STILL_MINERAL_SLURRY, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_MINERAL_SLURRY_BUCKET = ITEMS.register("still_mineral_slurry_bucket", () -> new ArchitecturyBucketItem(STILL_MINERAL_SLURRY, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    
    // sheol fire
    public static final RegistrySupplier<FlowingFluid> STILL_SHEOL_FIRE = FLUIDS.register("still_sheol_fire", () -> cast(new ArchitecturyFlowingFluid.Source(SHEOL_FIRE_ATTRIBUTES)));
    public static final RegistrySupplier<FlowingFluid> FLOWING_SHEOL_FIRE = FLUIDS.register("flowing_sheol_fire", () -> cast(new ArchitecturyFlowingFluid.Flowing(SHEOL_FIRE_ATTRIBUTES)));
    public static final RegistrySupplier<LiquidBlock> STILL_SHEOL_FIRE_BLOCK = BLOCKS.register("still_sheol_fire_block", () -> new SheolFireFluidBlock(STILL_SHEOL_FIRE, BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA)));
    public static final RegistrySupplier<Item> STILL_SHEOL_FIRE_BUCKET = ITEMS.register("still_sheol_fire_bucket", () -> new ArchitecturyBucketItem(STILL_SHEOL_FIRE, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    
    // strange matter
    public static final RegistrySupplier<FlowingFluid> STILL_STRANGE_MATTER = FLUIDS.register("still_strange_matter", () -> cast(new ArchitecturyFlowingFluid.Source(STRANGE_MATTER_ATTRIBUTES)));
    public static final RegistrySupplier<FlowingFluid> FLOWING_STRANGE_MATTER = FLUIDS.register("flowing_strange_matter", () -> cast(new ArchitecturyFlowingFluid.Flowing(STRANGE_MATTER_ATTRIBUTES)));
    public static final RegistrySupplier<LiquidBlock> STILL_STRANGE_MATTER_BLOCK = BLOCKS.register("still_strange_matter_block", () -> new ArchitecturyLiquidBlock(STILL_STRANGE_MATTER, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final RegistrySupplier<Item> STILL_STRANGE_MATTER_BUCKET = ITEMS.register("still_strange_matter_bucket", () -> new ArchitecturyBucketItem(STILL_STRANGE_MATTER, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    
    // adamant
    public static final RegistrySupplier<FlowingFluid> STILL_MOLTEN_ADAMANT = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("still_molten_adamant", () -> cast(new ArchitecturyFlowingFluid.Source(MOLTEN_ADAMANT_ATTRIBUTES))) : null;
    public static final RegistrySupplier<FlowingFluid> FLOWING_MOLTEN_ADAMANT = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("flowing_molten_adamant", () -> cast(new ArchitecturyFlowingFluid.Flowing(MOLTEN_ADAMANT_ATTRIBUTES))) : null;
    public static final RegistrySupplier<LiquidBlock> STILL_MOLTEN_ADAMANT_BLOCK = Platform.isModLoaded("productivemetalworks") ? BLOCKS.register("still_molten_adamant_block", () -> new ArchitecturyLiquidBlock(STILL_MOLTEN_ADAMANT, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER))) : null;
    public static final RegistrySupplier<Item> STILL_MOLTEN_ADAMANT_BUCKET = Platform.isModLoaded("productivemetalworks") ? ITEMS.register("still_molten_adamant_bucket", () -> new ArchitecturyBucketItem(STILL_MOLTEN_ADAMANT, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET))) : null;
    
    // biosteel
    public static final RegistrySupplier<FlowingFluid> STILL_MOLTEN_BIOSTEEL = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("still_molten_biosteel", () -> cast(new ArchitecturyFlowingFluid.Source(MOLTEN_BIOSTEEL_ATTRIBUTES))) : null;
    public static final RegistrySupplier<FlowingFluid> FLOWING_MOLTEN_BIOSTEEL = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("flowing_molten_biosteel", () -> cast(new ArchitecturyFlowingFluid.Flowing(MOLTEN_BIOSTEEL_ATTRIBUTES))) : null;
    public static final RegistrySupplier<LiquidBlock> STILL_MOLTEN_BIOSTEEL_BLOCK = Platform.isModLoaded("productivemetalworks") ? BLOCKS.register("still_molten_biosteel_block", () -> new ArchitecturyLiquidBlock(STILL_MOLTEN_BIOSTEEL, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER))) : null;
    public static final RegistrySupplier<Item> STILL_MOLTEN_BIOSTEEL_BUCKET = Platform.isModLoaded("productivemetalworks") ? ITEMS.register("still_molten_biosteel_bucket", () -> new ArchitecturyBucketItem(STILL_MOLTEN_BIOSTEEL, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET))) : null;
    
    // duratium
    public static final RegistrySupplier<FlowingFluid> STILL_MOLTEN_DURATIUM = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("still_molten_duratium", () -> cast(new ArchitecturyFlowingFluid.Source(MOLTEN_DURATIUM_ATTRIBUTES))) : null;
    public static final RegistrySupplier<FlowingFluid> FLOWING_MOLTEN_DURATIUM = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("flowing_molten_duratium", () -> cast(new ArchitecturyFlowingFluid.Flowing(MOLTEN_DURATIUM_ATTRIBUTES))) : null;
    public static final RegistrySupplier<LiquidBlock> STILL_MOLTEN_DURATIUM_BLOCK = Platform.isModLoaded("productivemetalworks") ? BLOCKS.register("still_molten_duratium_block", () -> new ArchitecturyLiquidBlock(STILL_MOLTEN_DURATIUM, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER))) : null;
    public static final RegistrySupplier<Item> STILL_MOLTEN_DURATIUM_BUCKET = Platform.isModLoaded("productivemetalworks") ? ITEMS.register("still_molten_duratium_bucket", () -> new ArchitecturyBucketItem(STILL_MOLTEN_DURATIUM, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET))) : null;
    
    // energite
    public static final RegistrySupplier<FlowingFluid> STILL_MOLTEN_ENERGITE = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("still_molten_energite", () -> cast(new ArchitecturyFlowingFluid.Source(MOLTEN_ENERGITE_ATTRIBUTES))) : null;
    public static final RegistrySupplier<FlowingFluid> FLOWING_MOLTEN_ENERGITE = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("flowing_molten_energite", () -> cast(new ArchitecturyFlowingFluid.Flowing(MOLTEN_ENERGITE_ATTRIBUTES))) : null;
    public static final RegistrySupplier<LiquidBlock> STILL_MOLTEN_ENERGITE_BLOCK = Platform.isModLoaded("productivemetalworks") ? BLOCKS.register("still_molten_energite_block", () -> new ArchitecturyLiquidBlock(STILL_MOLTEN_ENERGITE, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER))) : null;
    public static final RegistrySupplier<Item> STILL_MOLTEN_ENERGITE_BUCKET = Platform.isModLoaded("productivemetalworks") ? ITEMS.register("still_molten_energite_bucket", () -> new ArchitecturyBucketItem(STILL_MOLTEN_ENERGITE, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET))) : null;
    
    // fluxite
    public static final RegistrySupplier<FlowingFluid> STILL_MOLTEN_FLUXITE = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("still_molten_fluxite", () -> cast(new ArchitecturyFlowingFluid.Source(MOLTEN_FLUXITE_ATTRIBUTES))) : null;
    public static final RegistrySupplier<FlowingFluid> FLOWING_MOLTEN_FLUXITE = Platform.isModLoaded("productivemetalworks") ? FLUIDS.register("flowing_molten_fluxite", () -> cast(new ArchitecturyFlowingFluid.Flowing(MOLTEN_FLUXITE_ATTRIBUTES))) : null;
    public static final RegistrySupplier<LiquidBlock> STILL_MOLTEN_FLUXITE_BLOCK = Platform.isModLoaded("productivemetalworks") ? BLOCKS.register("still_molten_fluxite_block", () -> new ArchitecturyLiquidBlock(STILL_MOLTEN_FLUXITE, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER))) : null;
    public static final RegistrySupplier<Item> STILL_MOLTEN_FLUXITE_BUCKET = Platform.isModLoaded("productivemetalworks") ? ITEMS.register("still_molten_fluxite_bucket", () -> new ArchitecturyBucketItem(STILL_MOLTEN_FLUXITE, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET))) : null;
    
    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }
    
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
        ItemGroups.add(ItemContent.Groups.components, STILL_HEAVY_OIL_BUCKET.get());
        ItemGroups.add(ItemContent.Groups.components, STILL_DIESEL_BUCKET.get());
        ItemGroups.add(ItemContent.Groups.components, STILL_NAPHTHA_BUCKET.get());
        ItemGroups.add(ItemContent.Groups.components, STILL_SULFURIC_ACID_BUCKET.get());
        ItemGroups.add(ItemContent.Groups.components, STILL_SILICON_WASH_BUCKET.get());
        ItemGroups.add(ItemContent.Groups.components, STILL_MINERAL_SLURRY_BUCKET.get());
        ItemGroups.add(ItemContent.Groups.components, STILL_SHEOL_FIRE_BUCKET.get());
        ItemGroups.add(ItemContent.Groups.components, STILL_STRANGE_MATTER_BUCKET.get());
        
        if (Platform.isModLoaded("productivemetalworks")) {
            ItemGroups.add(ItemContent.Groups.components, STILL_MOLTEN_ADAMANT_BUCKET.get());
            ItemGroups.add(ItemContent.Groups.components, STILL_MOLTEN_BIOSTEEL_BUCKET.get());
            ItemGroups.add(ItemContent.Groups.components, STILL_MOLTEN_DURATIUM_BUCKET.get());
            ItemGroups.add(ItemContent.Groups.components, STILL_MOLTEN_ENERGITE_BUCKET.get());
            ItemGroups.add(ItemContent.Groups.components, STILL_MOLTEN_FLUXITE_BUCKET.get());
        }
    }
    
}
