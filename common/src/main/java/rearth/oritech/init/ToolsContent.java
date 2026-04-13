package rearth.oritech.init;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Unbreakable;
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
    
    protected static final Item.Properties UNBREAKING_SETTINGS = new Item.Properties()
                                                               .stacksTo(1)
                                                               .durability(0)
                                                               .component(DataComponents.UNBREAKABLE, new Unbreakable(true));
    
    protected static final Item.Properties ELECTRIC_SETTINGS = UNBREAKING_SETTINGS; //.component(Oritech.ENERGY_CONTENT.componentType(), 0L);
    protected static final Item.Properties JETPACK_SETTINGS = UNBREAKING_SETTINGS; // .component(ComponentContent.STORED_FLUID.get(), FluidStack.create(FluidContent.STILL_FUEL.get().getStill(), 0)); //.component(Oritech.ENERGY_CONTENT.componentType(), 0L);
    
    public static final Holder<ArmorMaterial> EXOSUIT_MATERIAL = ArmorMaterials.IRON;
    public static final Holder<ArmorMaterial> JETPACK_MATERIAL = ArmorMaterials.LEATHER;
    public static final Tier ELECTRIC_MATERIAL = new ElectricToolMaterial();
    public static final Tier PROMETHIUM_MATERIAL = new PromethiumToolMaterial();
    
    public static final Item EXO_HELMET = new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.HELMET, UNBREAKING_SETTINGS);
    public static final Item EXO_CHESTPLATE = new BackstorageExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.CHESTPLATE, ELECTRIC_SETTINGS);
    public static final Item EXO_LEGGINGS = new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.LEGGINGS, UNBREAKING_SETTINGS);
    public static final Item EXO_BOOTS = new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.BOOTS, UNBREAKING_SETTINGS);
    
    public static final Item JETPACK = new JetpackItem(JETPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, JETPACK_SETTINGS);
    public static final Item EXO_JETPACK = new JetpackExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.CHESTPLATE, JETPACK_SETTINGS);
    public static final Item JETPACK_ELYTRA = new JetpackElytraItem(JETPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, JETPACK_SETTINGS);
    public static final Item JETPACK_EXO_ELYTRA = new JetpackExoElytraItem(EXOSUIT_MATERIAL, ArmorItem.Type.CHESTPLATE, JETPACK_SETTINGS);
    
    public static final Item PORTABLE_LASER = new PortableLaserItem(UNBREAKING_SETTINGS);
    public static final Item ELECTRIC_MACE = new ElectricMaceItem(UNBREAKING_SETTINGS.attributes(ElectricMaceItem.createAttributes()).rarity(Rarity.EPIC));
    
    public static final Item CHAINSAW = new ChainsawItem(ELECTRIC_MATERIAL, ELECTRIC_SETTINGS.attributes(AxeItem.createAttributes(ELECTRIC_MATERIAL, 5f, -2.4f)));
    public static final Item HAND_DRILL = new DrillItem(ELECTRIC_MATERIAL, TagContent.DRILL_MINEABLE, ELECTRIC_SETTINGS.attributes(PickaxeItem.createAttributes(ELECTRIC_MATERIAL, 1f, -2.4f)));
    
    public static final Item PROMETHIUM_AXE = new PromethiumAxeItem(PROMETHIUM_MATERIAL, UNBREAKING_SETTINGS.attributes(PromethiumPickaxeItem.createPromethiumAttributes(PROMETHIUM_MATERIAL, 12f, -2.1f, 2)));
    public static final Item PROMETHIUM_PICKAXE = new PromethiumPickaxeItem(PROMETHIUM_MATERIAL, TagContent.DRILL_MINEABLE, UNBREAKING_SETTINGS.attributes(PromethiumPickaxeItem.createPromethiumAttributes(PROMETHIUM_MATERIAL, 3f, -2.4f, 2)));
    
    @Override
    public ResourceKey<Registry<Item>> getRegistryType() {
        return Registries.ITEM;
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
            
            if (source.typeHolder().is(DamageTypes.FALL) && entity instanceof Player player) {
                var boots = player.getItemBySlot(EquipmentSlot.FEET);
                
                if (boots == null) return EventResult.pass();
                if (!(boots.getItem() instanceof ExoArmorItem)) return EventResult.pass();
                
                player.level().playSound(null, player.blockPosition(), SoundContent.SHORT_SERVO, SoundSource.PLAYERS, 0.2f, 1.0f);
                
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
