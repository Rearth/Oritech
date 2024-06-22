package rearth.oritech.init;

import io.wispforest.lavender.book.LavenderBookItem;
import io.wispforest.owo.registration.annotations.IterationIgnored;
import io.wispforest.owo.registration.reflect.ItemRegistryContainer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import rearth.oritech.Oritech;
import rearth.oritech.init.datagen.data.TagContent;
import rearth.oritech.item.tools.armor.BackstorageExoArmorItem;
import rearth.oritech.item.tools.armor.ExoArmorItem;
import rearth.oritech.item.tools.harvesting.*;
import rearth.oritech.item.tools.util.ArmorEventHandler;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.lang.reflect.Field;

public class ToolsContent implements ItemRegistryContainer {
    
    private static final Item.Settings UNBREAKING_SETTINGS = new Item.Settings().maxCount(1).maxDamage(0).component(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true));
    
    public static final RegistryEntry<ArmorMaterial> EXOSUIT_MATERIAL = ArmorMaterials.DIAMOND;
    public static final ToolMaterial ELECTRIC_MATERIAL = new ElectricToolMaterial();
    public static final ToolMaterial PROMETHIUM_MATERIAL = new PromethiumToolMaterial();
    
    public static final Item EXO_HELMET = new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.HELMET, UNBREAKING_SETTINGS);
    public static final Item EXO_CHESTPLATE = new BackstorageExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.CHESTPLATE, UNBREAKING_SETTINGS);
    public static final Item EXO_LEGGINGS = new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.LEGGINGS, UNBREAKING_SETTINGS);
    public static final Item EXO_BOOTS = new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.BOOTS, UNBREAKING_SETTINGS);
    
    public static final Item CHAINSAW = new ChainsawItem(ELECTRIC_MATERIAL, UNBREAKING_SETTINGS.attributeModifiers(AxeItem.createAttributeModifiers(ELECTRIC_MATERIAL, 5f, -2.4f)));
    public static final Item HAND_DRILL = new DrillItem(ELECTRIC_MATERIAL, TagContent.DRILL_MINEABLE, UNBREAKING_SETTINGS.attributeModifiers(PickaxeItem.createAttributeModifiers(ELECTRIC_MATERIAL, 1f, -2.4f)));
    
    public static final Item PROMETHIUM_AXE = new PromethiumAxeItem(PROMETHIUM_MATERIAL, UNBREAKING_SETTINGS.attributeModifiers(AxeItem.createAttributeModifiers(PROMETHIUM_MATERIAL, 8f, -2.1f)));
    public static final Item PROMETHIUM_PICKAXE = new PromethiumPickaxeItem(PROMETHIUM_MATERIAL, TagContent.DRILL_MINEABLE, UNBREAKING_SETTINGS.attributeModifiers(AxeItem.createAttributeModifiers(PROMETHIUM_MATERIAL, 3f, -2.4f)));
    
    @IterationIgnored
    public static final Item ORITECH_GUIDE = LavenderBookItem.registerForBook(Oritech.id("oritech_guide"), UNBREAKING_SETTINGS.maxCount(1));
    
    @Override
    public void postProcessField(String namespace, Item value, String identifier, Field field) {
        ItemRegistryContainer.super.postProcessField(namespace, value, identifier, field);
        
        var targetGroup = ItemContent.Groups.equipment;
        if (field.isAnnotationPresent(ItemContent.ItemGroupTarget.class)) {
            targetGroup = field.getAnnotation(ItemContent.ItemGroupTarget.class).value();
        }
        
        ItemGroups.add(targetGroup, value);
        
        if (value instanceof SimpleEnergyItem energyItem) {
            var variantStack = new ItemStack(value);
            variantStack.set(EnergyStorage.ENERGY_COMPONENT, energyItem.getEnergyCapacity(variantStack));
            ItemGroups.add(targetGroup, variantStack);
        }
        
    }
    
    public static void registerEventHandlers() {
        
        ServerEntityEvents.EQUIPMENT_CHANGE.register((livingEntity, equipmentSlot, previousStack, currentStack) -> {
            if (livingEntity instanceof PlayerEntity playerEntity && equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                if (previousStack.getItem() instanceof ArmorEventHandler armorItem) {
                    armorItem.onUnequipped(playerEntity, previousStack);
                }
                if (currentStack.getItem() instanceof ArmorEventHandler armorItem) {
                    armorItem.onEquipped(playerEntity, currentStack);
                }
                
            }
        });
        
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            
            if (source.getTypeRegistryEntry().matchesKey(DamageTypes.FALL) && entity instanceof PlayerEntity player) {
                var boots = player.getEquippedStack(EquipmentSlot.FEET);
                return boots == null || !(boots.getItem() instanceof ExoArmorItem);
            }
            return true;
        });
        
    }
    
}
