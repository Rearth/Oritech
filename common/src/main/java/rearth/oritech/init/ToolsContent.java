package rearth.oritech.init;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Rarity;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.item.tools.ElectricMaceItem;
import rearth.oritech.item.tools.PortableLaserItem;
import rearth.oritech.item.tools.armor.*;
import rearth.oritech.item.tools.harvesting.*;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

import java.lang.reflect.Field;

public class ToolsContent implements ArchitecturyRegistryContainer<Item> {
    
    protected static final Item.Settings UNBREAKING_SETTINGS = new Item.Settings()
                                                               .maxCount(1)
                                                               .maxDamage(0)
                                                               .component(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true));
    
    protected static final Item.Settings ELECTRIC_SETTINGS = UNBREAKING_SETTINGS; //.component(Oritech.ENERGY_CONTENT.componentType(), 0L);
    protected static final Item.Settings JETPACK_SETTINGS = UNBREAKING_SETTINGS; // .component(ComponentContent.STORED_FLUID.get(), FluidStack.create(FluidContent.STILL_FUEL.get().getStill(), 0)); //.component(Oritech.ENERGY_CONTENT.componentType(), 0L);
    
    public static final RegistryEntry<ArmorMaterial> EXOSUIT_MATERIAL = ArmorMaterials.IRON;
    public static final RegistryEntry<ArmorMaterial> JETPACK_MATERIAL = ArmorMaterials.LEATHER;
    public static final ToolMaterial ELECTRIC_MATERIAL = new ElectricToolMaterial();
    public static final ToolMaterial PROMETHIUM_MATERIAL = new PromethiumToolMaterial();
    
    public static final Item EXO_HELMET = new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.HELMET, UNBREAKING_SETTINGS);
    public static final Item EXO_CHESTPLATE = new BackstorageExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.CHESTPLATE, ELECTRIC_SETTINGS);
    public static final Item EXO_LEGGINGS = new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.LEGGINGS, UNBREAKING_SETTINGS);
    public static final Item EXO_BOOTS = new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.BOOTS, UNBREAKING_SETTINGS);
    
    public static final Item JETPACK = new JetpackItem(JETPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, JETPACK_SETTINGS);
    public static final Item EXO_JETPACK = new JetpackExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.CHESTPLATE, JETPACK_SETTINGS);
    public static final Item JETPACK_ELYTRA = new JetpackElytraItem(JETPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, JETPACK_SETTINGS);
    public static final Item JETPACK_EXO_ELYTRA = new JetpackExoElytraItem(EXOSUIT_MATERIAL, ArmorItem.Type.CHESTPLATE, JETPACK_SETTINGS);
    
    public static final Item PORTABLE_LASER = new PortableLaserItem(UNBREAKING_SETTINGS);
    public static final Item ELECTRIC_MACE = new ElectricMaceItem(UNBREAKING_SETTINGS.attributeModifiers(ElectricMaceItem.createAttributeModifiers()).rarity(Rarity.EPIC));
    
    public static final Item CHAINSAW = new ChainsawItem(ELECTRIC_MATERIAL, ELECTRIC_SETTINGS.attributeModifiers(AxeItem.createAttributeModifiers(ELECTRIC_MATERIAL, 5f, -2.4f)));
    public static final Item HAND_DRILL = new DrillItem(ELECTRIC_MATERIAL, TagContent.DRILL_MINEABLE, ELECTRIC_SETTINGS.attributeModifiers(PickaxeItem.createAttributeModifiers(ELECTRIC_MATERIAL, 1f, -2.4f)));
    
    public static final Item PROMETHIUM_AXE = new PromethiumAxeItem(PROMETHIUM_MATERIAL, UNBREAKING_SETTINGS.attributeModifiers(AxeItem.createAttributeModifiers(PROMETHIUM_MATERIAL, 12f, -2.1f)));
    public static final Item PROMETHIUM_PICKAXE = new PromethiumPickaxeItem(PROMETHIUM_MATERIAL, TagContent.DRILL_MINEABLE, UNBREAKING_SETTINGS.attributeModifiers(AxeItem.createAttributeModifiers(PROMETHIUM_MATERIAL, 3f, -2.4f)));
    
    @Override
    public RegistryKey<Registry<Item>> getRegistryType() {
        return RegistryKeys.ITEM;
    }
    
    @Override
    public void postProcessField(String namespace, Item value, String identifier, Field field, RegistrySupplier<Item> supplier) {
        
        var targetGroup = ItemContent.Groups.equipment;
        if (field.isAnnotationPresent(ItemContent.ItemGroupTarget.class)) {
            targetGroup = field.getAnnotation(ItemContent.ItemGroupTarget.class).value();
        }
        
        ItemGroups.add(targetGroup, value);
        
        if (EnergyApi.ITEM != null && value instanceof OritechEnergyItem energyItem) {
            var variantStack = new ItemStack(value);
            variantStack.set(EnergyApi.ITEM.getEnergyComponent(), energyItem.getEnergyCapacity(variantStack));
            ItemGroups.add(targetGroup, variantStack);
            
            EnergyApi.ITEM.registerForItem(() -> value);
        }
        
        if (FluidApi.ITEM != null && value instanceof FluidApi.ItemProvider) {
            FluidApi.ITEM.registerForItem(() -> value);
        }
        
    }
    
    public static void registerEventHandlers() {
        
        BlockEvent.BREAK.register(PromethiumPickaxeItem::preMine);
        // PlayerBlockBreakEvents.BEFORE.register(PromethiumPickaxeItem::preMine);
        
        TickEvent.SERVER_LEVEL_PRE.register(PromethiumAxeItem::onTick);
        
        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            
            if (source.getTypeRegistryEntry().matchesKey(DamageTypes.FALL) && entity instanceof PlayerEntity player) {
                var boots = player.getEquippedStack(EquipmentSlot.FEET);
                
                if (boots == null) return EventResult.pass();
                if (!(boots.getItem() instanceof ExoArmorItem)) return EventResult.pass();
                
                player.getWorld().playSound(null, player.getBlockPos(), SoundContent.SHORT_SERVO, SoundCategory.PLAYERS, 0.2f, 1.0f);
                
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        });
        
    }
    
    @Override
    public Class<Item> getTargetFieldType() {
        return Item.class;
    }
}
