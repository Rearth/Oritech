package rearth.oritech.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import rearth.oritech.Oritech;
import rearth.oritech.block.fluid.SheolFireFluidBlock;

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
      - used in item creation?
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

    public static final DeferredRegister.Blocks FLUID_BLOCKS = DeferredRegister.createBlocks(Oritech.MOD_ID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Oritech.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Oritech.MOD_ID);
    public static final DeferredRegister.Items BUCKET_ITEMS = DeferredRegister.createItems(Oritech.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> OIL_TYPE = FLUID_TYPES.register("oil_fluid_type", () -> new FluidType(FluidType.Properties.create().density(1050).temperature(315).viscosity(1800)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_OIL = FLUIDS.register("flowing_oil", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(OIL_TYPE, FluidContent.STILL_OIL, FluidContent.FLOWING_OIL).block(FluidContent.STILL_OIL_BLOCK).bucket(FluidContent.STILL_OIL_BUCKET)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STILL_OIL = FLUIDS.register("still_oil", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(OIL_TYPE, FluidContent.STILL_OIL, FluidContent.FLOWING_OIL).block(FluidContent.STILL_OIL_BLOCK).bucket(FluidContent.STILL_OIL_BUCKET)));
    public static final DeferredHolder<Block, LiquidBlock> STILL_OIL_BLOCK = FLUID_BLOCKS.registerBlock("still_oil_block", props -> new LiquidBlock(STILL_OIL.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    public static final DeferredHolder<Item, BucketItem> STILL_OIL_BUCKET = BUCKET_ITEMS.registerItem("still_oil_bucket", props -> new BucketItem(STILL_OIL.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    public static final DeferredHolder<FluidType, FluidType> FUEL_TYPE = FLUID_TYPES.register("fuel_fluid_type", () -> new FluidType(FluidType.Properties.create().density(780).temperature(300).viscosity(850)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_FUEL = FLUIDS.register("flowing_fuel", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(FUEL_TYPE, FluidContent.STILL_FUEL, FluidContent.FLOWING_FUEL).block(FluidContent.STILL_FUEL_BLOCK).bucket(FluidContent.STILL_FUEL_BUCKET)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STILL_FUEL = FLUIDS.register("still_fuel", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(FUEL_TYPE, FluidContent.STILL_FUEL, FluidContent.FLOWING_FUEL).block(FluidContent.STILL_FUEL_BLOCK).bucket(FluidContent.STILL_FUEL_BUCKET)));
    public static final DeferredHolder<Block, LiquidBlock> STILL_FUEL_BLOCK = FLUID_BLOCKS.registerBlock("still_fuel_block", props -> new LiquidBlock(STILL_FUEL.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    public static final DeferredHolder<Item, BucketItem> STILL_FUEL_BUCKET = BUCKET_ITEMS.registerItem("still_fuel_bucket", props -> new BucketItem(STILL_FUEL.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    public static final DeferredHolder<FluidType, FluidType> BIOFUEL_TYPE = FLUID_TYPES.register("biofuel_fluid_type", () -> new FluidType(FluidType.Properties.create().density(830).temperature(300).viscosity(950)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_BIOFUEL = FLUIDS.register("flowing_biofuel", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(BIOFUEL_TYPE, FluidContent.STILL_BIOFUEL, FluidContent.FLOWING_BIOFUEL).block(FluidContent.STILL_BIOFUEL_BLOCK).bucket(FluidContent.STILL_BIOFUEL_BUCKET)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STILL_BIOFUEL = FLUIDS.register("still_biofuel", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(BIOFUEL_TYPE, FluidContent.STILL_BIOFUEL, FluidContent.FLOWING_BIOFUEL).block(FluidContent.STILL_BIOFUEL_BLOCK).bucket(FluidContent.STILL_BIOFUEL_BUCKET)));
    public static final DeferredHolder<Block, LiquidBlock> STILL_BIOFUEL_BLOCK = FLUID_BLOCKS.registerBlock("still_biofuel_block", props -> new LiquidBlock(STILL_BIOFUEL.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    public static final DeferredHolder<Item, BucketItem> STILL_BIOFUEL_BUCKET = BUCKET_ITEMS.registerItem("still_biofuel_bucket", props -> new BucketItem(STILL_BIOFUEL.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = FLUID_TYPES.register("steam_fluid_type", () -> new FluidType(FluidType.Properties.create().motionScale(0.01D).canPushEntity(false).canSwim(false).canDrown(false).fallDistanceModifier(0.0F).density(-500).temperature(450).viscosity(100)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_STEAM = FLUIDS.register("flowing_steam", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(STEAM_TYPE, FluidContent.STILL_STEAM, FluidContent.FLOWING_STEAM).block(FluidContent.STILL_STEAM_BLOCK).bucket(FluidContent.STILL_STEAM_BUCKET)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STILL_STEAM = FLUIDS.register("still_steam", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(STEAM_TYPE, FluidContent.STILL_STEAM, FluidContent.FLOWING_STEAM).block(FluidContent.STILL_STEAM_BLOCK).bucket(FluidContent.STILL_STEAM_BUCKET)));
    public static final DeferredHolder<Block, LiquidBlock> STILL_STEAM_BLOCK = FLUID_BLOCKS.registerBlock("still_steam_block", props -> new LiquidBlock(STILL_STEAM.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    public static final DeferredHolder<Item, BucketItem> STILL_STEAM_BUCKET = BUCKET_ITEMS.registerItem("still_steam_bucket", props -> new BucketItem(STILL_STEAM.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    public static final DeferredHolder<FluidType, FluidType> HEAVY_OIL_TYPE = FLUID_TYPES.register("heavy_oil_fluid_type", () -> new FluidType(FluidType.Properties.create().density(1250).temperature(330).viscosity(2400)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_HEAVY_OIL = FLUIDS.register("flowing_heavy_oil", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(HEAVY_OIL_TYPE, FluidContent.STILL_HEAVY_OIL, FluidContent.FLOWING_HEAVY_OIL).block(FluidContent.STILL_HEAVY_OIL_BLOCK).bucket(FluidContent.STILL_HEAVY_OIL_BUCKET)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STILL_HEAVY_OIL = FLUIDS.register("still_heavy_oil", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(HEAVY_OIL_TYPE, FluidContent.STILL_HEAVY_OIL, FluidContent.FLOWING_HEAVY_OIL).block(FluidContent.STILL_HEAVY_OIL_BLOCK).bucket(FluidContent.STILL_HEAVY_OIL_BUCKET)));
    public static final DeferredHolder<Block, LiquidBlock> STILL_HEAVY_OIL_BLOCK = FLUID_BLOCKS.registerBlock("still_heavy_oil_block", props -> new LiquidBlock(STILL_HEAVY_OIL.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    public static final DeferredHolder<Item, BucketItem> STILL_HEAVY_OIL_BUCKET = BUCKET_ITEMS.registerItem("still_heavy_oil_bucket", props -> new BucketItem(STILL_HEAVY_OIL.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    public static final DeferredHolder<FluidType, FluidType> DIESEL_TYPE = FLUID_TYPES.register("diesel_fluid_type", () -> new FluidType(FluidType.Properties.create().density(830).temperature(340).viscosity(1000)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_DIESEL = FLUIDS.register("flowing_diesel", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(DIESEL_TYPE, FluidContent.STILL_DIESEL, FluidContent.FLOWING_DIESEL).block(FluidContent.STILL_DIESEL_BLOCK).bucket(FluidContent.STILL_DIESEL_BUCKET)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STILL_DIESEL = FLUIDS.register("still_diesel", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(DIESEL_TYPE, FluidContent.STILL_DIESEL, FluidContent.FLOWING_DIESEL).block(FluidContent.STILL_DIESEL_BLOCK).bucket(FluidContent.STILL_DIESEL_BUCKET)));
    public static final DeferredHolder<Block, LiquidBlock> STILL_DIESEL_BLOCK = FLUID_BLOCKS.registerBlock("still_diesel_block", props -> new LiquidBlock(STILL_DIESEL.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    public static final DeferredHolder<Item, BucketItem> STILL_DIESEL_BUCKET = BUCKET_ITEMS.registerItem("still_diesel_bucket", props -> new BucketItem(STILL_DIESEL.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    public static final DeferredHolder<FluidType, FluidType> NAPHTHA_TYPE = FLUID_TYPES.register("naphtha_fluid_type", () -> new FluidType(FluidType.Properties.create().density(720).temperature(335).viscosity(550)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_NAPHTHA = FLUIDS.register("flowing_naphtha", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(NAPHTHA_TYPE, FluidContent.STILL_NAPHTHA, FluidContent.FLOWING_NAPHTHA).block(FluidContent.STILL_NAPHTHA_BLOCK).bucket(FluidContent.STILL_NAPHTHA_BUCKET)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STILL_NAPHTHA = FLUIDS.register("still_naphtha", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(NAPHTHA_TYPE, FluidContent.STILL_NAPHTHA, FluidContent.FLOWING_NAPHTHA).block(FluidContent.STILL_NAPHTHA_BLOCK).bucket(FluidContent.STILL_NAPHTHA_BUCKET)));
    public static final DeferredHolder<Block, LiquidBlock> STILL_NAPHTHA_BLOCK = FLUID_BLOCKS.registerBlock("still_naphtha_block", props -> new LiquidBlock(STILL_NAPHTHA.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    public static final DeferredHolder<Item, BucketItem> STILL_NAPHTHA_BUCKET = BUCKET_ITEMS.registerItem("still_naphtha_bucket", props -> new BucketItem(STILL_NAPHTHA.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    public static final DeferredHolder<FluidType, FluidType> SULFURIC_ACID_TYPE = FLUID_TYPES.register("sulfuric_acid_fluid_type", () -> new FluidType(FluidType.Properties.create().density(1800).temperature(320).viscosity(1400).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SULFURIC_ACID = FLUIDS.register("flowing_sulfuric_acid", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(SULFURIC_ACID_TYPE, FluidContent.STILL_SULFURIC_ACID, FluidContent.FLOWING_SULFURIC_ACID).block(FluidContent.STILL_SULFURIC_ACID_BLOCK).bucket(FluidContent.STILL_SULFURIC_ACID_BUCKET)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STILL_SULFURIC_ACID = FLUIDS.register("still_sulfuric_acid", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(SULFURIC_ACID_TYPE, FluidContent.STILL_SULFURIC_ACID, FluidContent.FLOWING_SULFURIC_ACID).block(FluidContent.STILL_SULFURIC_ACID_BLOCK).bucket(FluidContent.STILL_SULFURIC_ACID_BUCKET)));
    public static final DeferredHolder<Block, LiquidBlock> STILL_SULFURIC_ACID_BLOCK = FLUID_BLOCKS.registerBlock("still_sulfuric_acid_block", props -> new LiquidBlock(STILL_SULFURIC_ACID.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    public static final DeferredHolder<Item, BucketItem> STILL_SULFURIC_ACID_BUCKET = BUCKET_ITEMS.registerItem("still_sulfuric_acid_bucket", props -> new BucketItem(STILL_SULFURIC_ACID.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    public static final DeferredHolder<FluidType, FluidType> SILICON_WASH_TYPE = FLUID_TYPES.register("silicon_wash_fluid_type", () -> new FluidType(FluidType.Properties.create().density(1080).temperature(300).viscosity(1100)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SILICON_WASH = FLUIDS.register("flowing_silicon_wash", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(SILICON_WASH_TYPE, FluidContent.STILL_SILICON_WASH, FluidContent.FLOWING_SILICON_WASH).block(FluidContent.STILL_SILICON_WASH_BLOCK).bucket(FluidContent.STILL_SILICON_WASH_BUCKET)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STILL_SILICON_WASH = FLUIDS.register("still_silicon_wash", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(SILICON_WASH_TYPE, FluidContent.STILL_SILICON_WASH, FluidContent.FLOWING_SILICON_WASH).block(FluidContent.STILL_SILICON_WASH_BLOCK).bucket(FluidContent.STILL_SILICON_WASH_BUCKET)));
    public static final DeferredHolder<Block, LiquidBlock> STILL_SILICON_WASH_BLOCK = FLUID_BLOCKS.registerBlock("still_silicon_wash_block", props -> new LiquidBlock(STILL_SILICON_WASH.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    public static final DeferredHolder<Item, BucketItem> STILL_SILICON_WASH_BUCKET = BUCKET_ITEMS.registerItem("still_silicon_wash_bucket", props -> new BucketItem(STILL_SILICON_WASH.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    public static final DeferredHolder<FluidType, FluidType> MINERAL_SLURRY_TYPE = FLUID_TYPES.register("mineral_slurry_fluid_type", () -> new FluidType(FluidType.Properties.create().density(1600).temperature(295).viscosity(2600)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_MINERAL_SLURRY = FLUIDS.register("flowing_mineral_slurry", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(MINERAL_SLURRY_TYPE, FluidContent.STILL_MINERAL_SLURRY, FluidContent.FLOWING_MINERAL_SLURRY).block(FluidContent.STILL_MINERAL_SLURRY_BLOCK).bucket(FluidContent.STILL_MINERAL_SLURRY_BUCKET)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STILL_MINERAL_SLURRY = FLUIDS.register("still_mineral_slurry", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(MINERAL_SLURRY_TYPE, FluidContent.STILL_MINERAL_SLURRY, FluidContent.FLOWING_MINERAL_SLURRY).block(FluidContent.STILL_MINERAL_SLURRY_BLOCK).bucket(FluidContent.STILL_MINERAL_SLURRY_BUCKET)));
    public static final DeferredHolder<Block, LiquidBlock> STILL_MINERAL_SLURRY_BLOCK = FLUID_BLOCKS.registerBlock("still_mineral_slurry_block", props -> new LiquidBlock(STILL_MINERAL_SLURRY.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    public static final DeferredHolder<Item, BucketItem> STILL_MINERAL_SLURRY_BUCKET = BUCKET_ITEMS.registerItem("still_mineral_slurry_bucket", props -> new BucketItem(STILL_MINERAL_SLURRY.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    public static final DeferredHolder<FluidType, FluidType> SHEOL_FIRE_TYPE = FLUID_TYPES.register("sheol_fire_fluid_type", () -> new FluidType(FluidType.Properties.create().canSwim(false).canDrown(false).canPushEntity(false).fallDistanceModifier(0.1F).lightLevel(15).density(850).temperature(1400).viscosity(900).rarity(Rarity.RARE)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SHEOL_FIRE = FLUIDS.register("flowing_sheol_fire", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(SHEOL_FIRE_TYPE, FluidContent.STILL_SHEOL_FIRE, FluidContent.FLOWING_SHEOL_FIRE).block(FluidContent.STILL_SHEOL_FIRE_BLOCK).bucket(FluidContent.STILL_SHEOL_FIRE_BUCKET)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STILL_SHEOL_FIRE = FLUIDS.register("still_sheol_fire", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(SHEOL_FIRE_TYPE, FluidContent.STILL_SHEOL_FIRE, FluidContent.FLOWING_SHEOL_FIRE).block(FluidContent.STILL_SHEOL_FIRE_BLOCK).bucket(FluidContent.STILL_SHEOL_FIRE_BUCKET)));
    public static final DeferredHolder<Block, SheolFireFluidBlock> STILL_SHEOL_FIRE_BLOCK = FLUID_BLOCKS.registerBlock("still_sheol_fire_block", props -> new SheolFireFluidBlock(STILL_SHEOL_FIRE.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA));
    public static final DeferredHolder<Item, BucketItem> STILL_SHEOL_FIRE_BUCKET = BUCKET_ITEMS.registerItem("still_sheol_fire_bucket", props -> new BucketItem(STILL_SHEOL_FIRE.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    public static final DeferredHolder<FluidType, FluidType> STRANGE_MATTER_TYPE = FLUID_TYPES.register("strange_matter_fluid_type", () -> new FluidType(FluidType.Properties.create().canSwim(false).canDrown(false).canPushEntity(false).lightLevel(7).density(1800).temperature(500).viscosity(1800).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_STRANGE_MATTER = FLUIDS.register("flowing_strange_matter", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(STRANGE_MATTER_TYPE, FluidContent.STILL_STRANGE_MATTER, FluidContent.FLOWING_STRANGE_MATTER).block(FluidContent.STILL_STRANGE_MATTER_BLOCK).bucket(FluidContent.STILL_STRANGE_MATTER_BUCKET)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STILL_STRANGE_MATTER = FLUIDS.register("still_strange_matter", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(STRANGE_MATTER_TYPE, FluidContent.STILL_STRANGE_MATTER, FluidContent.FLOWING_STRANGE_MATTER).block(FluidContent.STILL_STRANGE_MATTER_BLOCK).bucket(FluidContent.STILL_STRANGE_MATTER_BUCKET)));
    public static final DeferredHolder<Block, LiquidBlock> STILL_STRANGE_MATTER_BLOCK = FLUID_BLOCKS.registerBlock("still_strange_matter_block", props -> new LiquidBlock(STILL_STRANGE_MATTER.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    public static final DeferredHolder<Item, BucketItem> STILL_STRANGE_MATTER_BUCKET = BUCKET_ITEMS.registerItem("still_strange_matter_bucket", props -> new BucketItem(STILL_STRANGE_MATTER.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

}


