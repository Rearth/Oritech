package rearth.oritech.init;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Util;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import rearth.oritech.Oritech;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.item.tools.ElectricMaceItem;
import rearth.oritech.item.tools.PortableLaserItem;
import rearth.oritech.item.tools.armor.*;
import rearth.oritech.item.tools.harvesting.ChainsawItem;
import rearth.oritech.item.tools.harvesting.DrillItem;
import rearth.oritech.item.tools.harvesting.PromethiumAxeItem;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;

import java.util.EnumMap;

@SuppressWarnings("NullableProblems")
public class ToolsContent {

    public static final DeferredRegister.Items EQUIPMENT = DeferredRegister.createItems(Oritech.MOD_ID);

    public static final ResourceKey<EquipmentAsset> EXOSUIT_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Oritech.id("exosuit"));

    public static final ArmorMaterial EXOSUIT_MATERIAL = new ArmorMaterial(
            100,   // doesnt really matter here
            Util.make(new EnumMap<>(ArmorType.class), map -> {    // protection values
                map.put(ArmorType.BOOTS, 3);
                map.put(ArmorType.LEGGINGS, 6);
                map.put(ArmorType.CHESTPLATE, 8);
                map.put(ArmorType.HELMET, 3);
                map.put(ArmorType.BODY, 4);
            }),
            20,
            SoundContent.SHORT_SERVO,
            0,
            0,
            TagContent.UNBREAKABLE_REPAIRS,
            EXOSUIT_ASSET
    );

    public static final ToolMaterial ELECTRIC_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            2000,
            9f,
            3,
            20,
            TagContent.UNBREAKABLE_REPAIRS
    );
    public static final ToolMaterial PROMETHIUM_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            2000,
            24f,
            5,
            28,
            TagContent.UNBREAKABLE_REPAIRS
    );

    public static final DeferredItem<Item> EXO_HELMET = EQUIPMENT.registerItem(
            "exo_helmet",
            props -> new ExoArmorItem(EXOSUIT_MATERIAL, ArmorType.HELMET,
                    props.humanoidArmor(EXOSUIT_MATERIAL, ArmorType.HELMET)
                            .component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );
    public static final DeferredItem<Item> EXO_CHESTPLATE = EQUIPMENT.registerItem(
            "exo_chestplate",
            props -> new BackstorageExoArmorItem(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE,
                    props.humanoidArmor(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE)
                            .component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );
    public static final DeferredItem<Item> EXO_LEGGINGS = EQUIPMENT.registerItem(
            "exo_leggings",
            props -> new ExoArmorItem(EXOSUIT_MATERIAL, ArmorType.LEGGINGS,
                    props.humanoidArmor(EXOSUIT_MATERIAL, ArmorType.LEGGINGS)
                            .component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );
    public static final DeferredItem<Item> EXO_BOOTS = EQUIPMENT.registerItem(
            "exo_boots",
            props -> new ExoArmorItem(EXOSUIT_MATERIAL, ArmorType.BOOTS,
                    props.humanoidArmor(EXOSUIT_MATERIAL, ArmorType.BOOTS)
                            .component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );

    public static final DeferredItem<Item> JETPACK = EQUIPMENT.registerItem(
            "jetpack",
            props -> new JetpackItem(JETPACK_MATERIAL, ArmorType.CHESTPLATE,
                    props.humanoidArmor(JETPACK_MATERIAL, ArmorType.CHESTPLATE)
                            .component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );
    public static final DeferredItem<Item> EXO_JETPACK = EQUIPMENT.registerItem(
            "exo_jetpack",
            props -> new JetpackExoArmorItem(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE,
                    props.humanoidArmor(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE)
                            .component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );
    public static final DeferredItem<Item> JETPACK_ELYTRA = EQUIPMENT.registerItem(
            "jetpack_elytra",
            props -> new JetpackElytraItem(JETPACK_MATERIAL, ArmorType.CHESTPLATE,
                    props.humanoidArmor(JETPACK_MATERIAL, ArmorType.CHESTPLATE)
                            .component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );
    public static final DeferredItem<Item> JETPACK_EXO_ELYTRA = EQUIPMENT.registerItem(
            "jetpack_exo_elytra",
            props -> new JetpackExoElytraItem(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE,
                    props.humanoidArmor(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE)
                            .component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );

    public static final DeferredItem<Item> PORTABLE_LASER = EQUIPMENT.registerItem(
            "portable_laser",
            props -> new PortableLaserItem(props.stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );
    public static final DeferredItem<Item> ELECTRIC_MACE = EQUIPMENT.registerItem(
            "electric_mace",
            props -> new ElectricMaceItem(props.stacksTo(1).attributes(ElectricMaceItem.createAttributes()).rarity(Rarity.EPIC).component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );

    public static final DeferredItem<Item> CHAINSAW = EQUIPMENT.registerItem(
            "chainsaw",
            props -> new ChainsawItem(ELECTRIC_MATERIAL, props.axe(ELECTRIC_MATERIAL, 5f, -2.4f).component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );
    public static final DeferredItem<Item> HAND_DRILL = EQUIPMENT.registerItem(
            "hand_drill",
            props -> new DrillItem(ELECTRIC_MATERIAL, TagContent.DRILL_MINEABLE,
                    props.pickaxe(ELECTRIC_MATERIAL, 1f, -2.4f).component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );

    public static final DeferredItem<Item> PROMETHIUM_AXE = EQUIPMENT.registerItem(
            "promethium_axe",
            props -> new PromethiumAxeItem(PROMETHIUM_MATERIAL,
                    props.axe(PROMETHIUM_MATERIAL, 12f, -2.1f)
                            .attributes(PromethiumPickaxeItem.createPromethiumAttributes(PROMETHIUM_MATERIAL, 12f, -2.1f, 2))
                            .rarity(Rarity.EPIC)
                            .component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );
    public static final DeferredItem<Item> PROMETHIUM_PICKAXE = EQUIPMENT.registerItem(
            "promethium_pickaxe",
            props -> new PromethiumPickaxeItem(PROMETHIUM_MATERIAL, TagContent.DRILL_MINEABLE,
                    props.pickaxe(PROMETHIUM_MATERIAL, 3f, -2.4f)
                            .attributes(PromethiumPickaxeItem.createPromethiumAttributes(PROMETHIUM_MATERIAL, 3f, -2.4f, 2))
                            .rarity(Rarity.EPIC)
                            .component(DataComponents.UNBREAKABLE, new Unbreakable(true)))
    );

    // TODO: Move equipment-tab population to registry/annotation scanning so these direct registrations show up without manual post-processing.
    // TODO: Revisit energy/fluid item hookup under NeoForge's current capability/data-component APIs instead of relying on the deleted bridge registration.

    public static void registerItemCapabilities(RegisterCapabilitiesEvent event) {

        event.registerItem(
                Capabilities.Energy.ITEM,
                (stack, itemAccess) -> {
                    var tool = (EnergyProvider.Item) stack.getItem();
                    return new ItemAccessEnergyHandler(itemAccess, ComponentContent.ENERGY.get(), tool.getCapacity(), tool.getMaxInsert(), tool.getMaxExtract());
                },
                HAND_DRILL
                );

        event.registerItem(
                Capabilities.Fluid.ITEM,
                (stack, itemAccess) -> {
                    var tool = (FluidProvider.Item) stack.getItem();
                    return new ItemAccessFluidHandler(itemAccess, ComponentContent.STORED_FLUID.get(), tool.getCapacity());
                },
                JETPACK
                );

    }

//    public static void registerEventHandlers() {
//
//        BlockEvent.BREAK.register(PromethiumPickaxeItem::preMine);
//        // PlayerBlockBreakEvents.BEFORE.register(PromethiumPickaxeItem::preMine);
//
//        TickEvent.SERVER_LEVEL_PRE.register(PromethiumAxeItem::onTick);
//
//        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
//
//            if (source.typeHolder().is(DamageTypes.FALL) && entity instanceof Player player) {
//                var boots = player.getItemBySlot(EquipmentSlot.FEET);
//
//                if (boots == null) return EventResult.pass();
//                if (!(boots.getItem() instanceof ExoArmorItem)) return EventResult.pass();
//
//                player.level().playSound(null, player.blockPosition(), SoundContent.SHORT_SERVO.get(), SoundSource.PLAYERS, 0.2f, 1.0f);
//
//                return EventResult.interruptFalse();
//            }
//            return EventResult.pass();
//        });
//
//    }
}
