package rearth.oritech.init;

import io.wispforest.lavender.book.LavenderBookItem;
import io.wispforest.owo.registration.annotations.IterationIgnored;
import io.wispforest.owo.registration.reflect.ItemRegistryContainer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import rearth.oritech.Oritech;
import rearth.oritech.item.tools.armor.BackstorageExoArmorItem;
import rearth.oritech.item.tools.armor.ExoArmorItem;
import rearth.oritech.item.tools.armor.ExoArmorMaterial;
import rearth.oritech.item.tools.harvesting.*;
import rearth.oritech.item.tools.util.ArmorEventHandler;

import java.lang.reflect.Field;

public class ToolsContent implements ItemRegistryContainer {
    
    public static final ArmorMaterial EXOSUIT_MATERIAL = new ExoArmorMaterial();
    public static final ToolMaterial ELECTRIC_MATERIAL = new ElectricToolMaterial();
    public static final ToolMaterial PROMETHIUM_MATERIAL = new PromethiumToolMaterial();
    
    public static final Item EXO_HELMET = new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.HELMET, new Item.Settings());
    public static final Item EXO_CHESTPLATE = new BackstorageExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Settings());
    public static final Item EXO_LEGGINGS = new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Settings());
    public static final Item EXO_BOOTS = new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.BOOTS, new Item.Settings());
    
    public static final Item CHAINSAW = new ChainsawItem(ELECTRIC_MATERIAL, 5, -2.5f);
    public static final Item HAND_DRILL = new DrillItem(2.5f, -2.8f, ELECTRIC_MATERIAL);
    
    public static final Item PROMETHIUM_AXE = new PromethiumAxeItem(PROMETHIUM_MATERIAL, 8, -2.2f);
    public static final Item PROMETHIUM_PICKAXE = new PromethiumPickaxeItem(4, -2.4f, PROMETHIUM_MATERIAL);
    
    @IterationIgnored
    public static final Item ORITECH_GUIDE = LavenderBookItem.registerForBook(Oritech.id("oritech_guide"), new Item.Settings().maxCount(1));
    
    @Override
    public void postProcessField(String namespace, Item value, String identifier, Field field) {
        ItemRegistryContainer.super.postProcessField(namespace, value, identifier, field);
        
        var targetGroup = ItemContent.Groups.equipment;
        if (field.isAnnotationPresent(ItemContent.ItemGroupTarget.class)) {
            targetGroup = field.getAnnotation(ItemContent.ItemGroupTarget.class).value();
        }
        
        ItemGroups.add(targetGroup, value);
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
