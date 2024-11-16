package rearth.oritech.init;

import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
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
import rearth.oritech.Oritech;
import rearth.oritech.item.tools.armor.*;
import rearth.oritech.item.tools.harvesting.*;
import rearth.oritech.item.tools.util.ArmorEventHandler;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.util.ArchitecturyRegistryContainer;

import java.lang.reflect.Field;

public class ToolsContent implements ArchitecturyRegistryContainer<Item> {
    
    protected static final Item.Settings UNBREAKING_SETTINGS = new Item.Settings()
                                                               .maxCount(1)
                                                               .maxDamage(0)
                                                               .component(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true));
    
    protected static final Item.Settings ELECTRIC_SETTINGS = UNBREAKING_SETTINGS; //.component(Oritech.ENERGY_CONTENT.componentType(), 0L);
    protected static final Item.Settings JETPACK_SETTINGS = UNBREAKING_SETTINGS; // .component(ComponentContent.STORED_FLUID.get(), FluidStack.create(FluidContent.STILL_FUEL.get().getStill(), 0)); //.component(Oritech.ENERGY_CONTENT.componentType(), 0L);
    
    public static final RegistryEntry<ArmorMaterial> EXOSUIT_MATERIAL = ArmorMaterials.DIAMOND;
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
    
    
    public static final Item CHAINSAW = new ChainsawItem(ELECTRIC_MATERIAL, ELECTRIC_SETTINGS.attributeModifiers(AxeItem.createAttributeModifiers(ELECTRIC_MATERIAL, 5f, -2.4f)));
    public static final Item HAND_DRILL = new DrillItem(ELECTRIC_MATERIAL, TagContent.DRILL_MINEABLE, ELECTRIC_SETTINGS.attributeModifiers(PickaxeItem.createAttributeModifiers(ELECTRIC_MATERIAL, 1f, -2.4f)));
    
    public static final Item PROMETHIUM_AXE = new PromethiumAxeItem(PROMETHIUM_MATERIAL, UNBREAKING_SETTINGS.attributeModifiers(AxeItem.createAttributeModifiers(PROMETHIUM_MATERIAL, 8f, -2.1f)));
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
        
        if (value instanceof OritechEnergyItem energyItem) {
            var variantStack = new ItemStack(value);
            variantStack.set(Oritech.ENERGY_CONTENT.componentType(), energyItem.getEnergyCapacity(variantStack));
            ItemGroups.add(targetGroup, variantStack);
        }
        
    }
    
    public static void registerEventHandlers() {
        
        PlayerBlockBreakEvents.BEFORE.register(PromethiumPickaxeItem::preMine);
        
        ServerTickEvents.START_WORLD_TICK.register(PromethiumAxeItem::onTick);
        
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
                
                if (boots == null) return true;
                if (!(boots.getItem() instanceof ExoArmorItem)) return true;
                
                player.getWorld().playSound(null, player.getBlockPos(), SoundContent.SHORT_SERVO, SoundCategory.PLAYERS, 0.2f, 1.0f);
                
                return false;
            }
            return true;
        });
        
    }
    
    @Override
    public Class<Item> getTargetFieldType() {
        return Item.class;
    }
}
