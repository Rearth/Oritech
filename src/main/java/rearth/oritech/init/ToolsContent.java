package rearth.oritech.init;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import rearth.oritech.Oritech;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.item.other.PortableTankFluidHandler;
import rearth.oritech.item.other.SmallFluidTankBlockItem;
import rearth.oritech.item.tools.ElectricMaceItem;
import rearth.oritech.item.tools.EndericRailgunItem;
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
                            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE))
    );
    public static final DeferredItem<Item> EXO_CHESTPLATE = EQUIPMENT.registerItem(
            "exo_chestplate",
            props -> new BackstorageExoArmorItem(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE,
                    props.humanoidArmor(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE)
                            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE))
    );
    public static final DeferredItem<Item> EXO_LEGGINGS = EQUIPMENT.registerItem(
            "exo_leggings",
            props -> new ExoArmorItem(EXOSUIT_MATERIAL, ArmorType.LEGGINGS,
                    props.humanoidArmor(EXOSUIT_MATERIAL, ArmorType.LEGGINGS)
                            .attributes(EXOSUIT_MATERIAL.createAttributes(ArmorType.LEGGINGS)
                                    .withModifierAdded(Attributes.MOVEMENT_SPEED, new AttributeModifier(Oritech.id("exo_move_speed"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.LEGS)
                                    .withModifierAdded(Attributes.FLYING_SPEED, new AttributeModifier(Oritech.id("exo_fly_speed"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.LEGS))
                            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE))
    );
    public static final DeferredItem<Item> EXO_BOOTS = EQUIPMENT.registerItem(
            "exo_boots",
            props -> new ExoArmorItem(EXOSUIT_MATERIAL, ArmorType.BOOTS,
                    props.humanoidArmor(EXOSUIT_MATERIAL, ArmorType.BOOTS)
                            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE))
    );

    public static final DeferredItem<Item> JETPACK = EQUIPMENT.registerItem(
            "jetpack",
            props -> new JetpackItem(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE,
                    props.humanoidArmor(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE)
                            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE))
    );
    public static final DeferredItem<Item> EXO_JETPACK = EQUIPMENT.registerItem(
            "exo_jetpack",
            props -> new JetpackExoArmorItem(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE,
                    props.humanoidArmor(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE)
                            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE))
    );
    public static final DeferredItem<Item> JETPACK_ELYTRA = EQUIPMENT.registerItem(
            "jetpack_elytra",
            props -> new JetpackElytraItem(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE,
                    elytraJetpackProperties(props))
    );
    public static final DeferredItem<Item> JETPACK_EXO_ELYTRA = EQUIPMENT.registerItem(
            "jetpack_exo_elytra",
            props -> new JetpackExoElytraItem(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE,
                    elytraJetpackProperties(props))
    );

    private static Item.Properties elytraJetpackProperties(Item.Properties props) {
        return props.humanoidArmor(EXOSUIT_MATERIAL, ArmorType.CHESTPLATE)
                .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                        .setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA)
                        .setAsset(EquipmentAssets.ELYTRA)
                        .setDamageOnHurt(false)
                        .build())
                .component(DataComponents.GLIDER, Unit.INSTANCE)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }

    public static final DeferredItem<Item> ENDERIC_RAILGUN = EQUIPMENT.registerItem(
            "enderic_railgun",
            props -> new EndericRailgunItem(props
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, Unit.INSTANCE))
    );

    public static final DeferredItem<Item> ELECTRIC_MACE = EQUIPMENT.registerItem(
            "electric_mace",
            props -> new ElectricMaceItem(props
                    .stacksTo(1)
                    .attributes(ElectricMaceItem.createAttributes())
                    .rarity(Rarity.EPIC)
                    .component(DataComponents.TOOL, MaceItem.createToolProperties())
                    .component(DataComponents.WEAPON, new Weapon(1))
                    .component(DataComponents.UNBREAKABLE, Unit.INSTANCE))
    );

    public static final DeferredItem<Item> CHAINSAW = EQUIPMENT.registerItem(
            "chainsaw",
            props -> new ChainsawItem(
                    ELECTRIC_MATERIAL, 5f, -2.4f, props.component(DataComponents.UNBREAKABLE, Unit.INSTANCE))
    );
    public static final DeferredItem<Item> HAND_DRILL = EQUIPMENT.registerItem(
            "hand_drill",
            props -> new DrillItem(
                    props.tool(ELECTRIC_MATERIAL, TagContent.DRILL_MINEABLE, 1f, -2.4f, 0)
                            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE))
    );

    public static final DeferredItem<Item> PROMETHIUM_AXE = EQUIPMENT.registerItem(
            "promethium_axe",
            props -> new PromethiumAxeItem(PROMETHIUM_MATERIAL,12f, -2.1f,
                    props
                            .rarity(Rarity.EPIC)
                            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                            .enchantable(20)
            )
    );

    public static final DeferredItem<Item> PROMETHIUM_PICKAXE = EQUIPMENT.registerItem(
            "promethium_pickaxe",
            props -> new PromethiumPickaxeItem(
                    props.pickaxe(PROMETHIUM_MATERIAL, 3f, -2.4f)
                            .attributes(PromethiumPickaxeItem.getToolAttributesWithRange(PROMETHIUM_MATERIAL, 3f, -2.4f, 2, "pick"))
                            .rarity(Rarity.EPIC)
                            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE))
    );

    public static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        // AxeItem applies its own attack attributes in the constructor, replacing any range attributes
        // passed in through Item.Properties. Patch the final default components after construction instead.
        event.modify(PROMETHIUM_AXE.get(), (components, context, item) -> components.set(
                DataComponents.ATTRIBUTE_MODIFIERS,
                PromethiumPickaxeItem.getToolAttributesWithRange(PROMETHIUM_MATERIAL, 12f, -2.1f, 2, "axe")
        ));
    }

    public static void registerItemCapabilities(RegisterCapabilitiesEvent event) {

        event.registerItem(
                Capabilities.Energy.ITEM,
                (stack, itemAccess) -> {
                    var tool = (EnergyProvider.Item) stack.getItem();
                    return new ItemAccessEnergyHandler(itemAccess, ComponentContent.ENERGY.get(), tool.getEnergyCapacity(), tool.getMaxRFInputRate(), tool.getMaxRFOutputRate());
                },
                HAND_DRILL, CHAINSAW, ELECTRIC_MACE, ENDERIC_RAILGUN, EXO_CHESTPLATE,
                JETPACK, EXO_JETPACK, JETPACK_ELYTRA, JETPACK_EXO_ELYTRA, ItemContent.PORTABLE_ENERGY_STORAGE_ITEM
        );

        event.registerItem(
                Capabilities.Fluid.ITEM,
                (stack, itemAccess) -> {
                    var tool = (FluidProvider.Item) stack.getItem();
                    if (stack.getItem() instanceof SmallFluidTankBlockItem) {
                        return new PortableTankFluidHandler(itemAccess, tool.getFluidCapacity());
                    }
                    return new ItemAccessFluidHandler(itemAccess, ComponentContent.STORED_FLUID.get(), tool.getFluidCapacity());
                },
                JETPACK, EXO_JETPACK, JETPACK_ELYTRA, JETPACK_EXO_ELYTRA, ItemContent.PORTABLE_TANK_ITEM, ItemContent.CREATIVE_TANK_ITEM
        );

    }
}
