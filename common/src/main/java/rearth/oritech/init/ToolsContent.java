package rearth.oritech.init;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
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
import rearth.oritech.util.registry.OritechItemRegistry;

public class ToolsContent {
    
    public static final OritechItemRegistry ITEMS = ItemContent.ITEMS;
    
    public static final Holder<ArmorMaterial> EXOSUIT_MATERIAL = ArmorMaterials.IRON;
    public static final Holder<ArmorMaterial> JETPACK_MATERIAL = ArmorMaterials.LEATHER;
    public static final Tier ELECTRIC_MATERIAL = new ElectricToolMaterial();
    public static final Tier PROMETHIUM_MATERIAL = new PromethiumToolMaterial();
    
    public static final RegistrySupplier<Item> EXO_HELMET = registerTool("exo_helmet", new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.HELMET, unbreakingSettings("exo_helmet")));
    public static final RegistrySupplier<Item> EXO_CHESTPLATE = registerTool("exo_chestplate", new BackstorageExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.CHESTPLATE, electricSettings("exo_chestplate")));
    public static final RegistrySupplier<Item> EXO_LEGGINGS = registerTool("exo_leggings", new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.LEGGINGS, unbreakingSettings("exo_leggings")));
    public static final RegistrySupplier<Item> EXO_BOOTS = registerTool("exo_boots", new ExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.BOOTS, unbreakingSettings("exo_boots")));
    
    public static final RegistrySupplier<Item> JETPACK = registerTool("jetpack", new JetpackItem(JETPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, jetpackSettings("jetpack")));
    public static final RegistrySupplier<Item> EXO_JETPACK = registerTool("exo_jetpack", new JetpackExoArmorItem(EXOSUIT_MATERIAL, ArmorItem.Type.CHESTPLATE, jetpackSettings("exo_jetpack")));
    public static final RegistrySupplier<Item> JETPACK_ELYTRA = registerTool("jetpack_elytra", new JetpackElytraItem(JETPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, jetpackSettings("jetpack_elytra")));
    public static final RegistrySupplier<Item> JETPACK_EXO_ELYTRA = registerTool("jetpack_exo_elytra", new JetpackExoElytraItem(EXOSUIT_MATERIAL, ArmorItem.Type.CHESTPLATE, jetpackSettings("jetpack_exo_elytra")));
    
    public static final RegistrySupplier<Item> PORTABLE_LASER = registerTool("portable_laser", new PortableLaserItem(unbreakingSettings("portable_laser").rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> ELECTRIC_MACE = registerTool("electric_mace", new ElectricMaceItem(unbreakingSettings("electric_mace").attributes(ElectricMaceItem.createAttributes()).rarity(Rarity.EPIC)));
    
    public static final RegistrySupplier<Item> CHAINSAW = registerTool("chainsaw", new ChainsawItem(ELECTRIC_MATERIAL, electricSettings("chainsaw").attributes(AxeItem.createAttributes(ELECTRIC_MATERIAL, 5f, -2.4f))));
    public static final RegistrySupplier<Item> HAND_DRILL = registerTool("hand_drill", new DrillItem(ELECTRIC_MATERIAL, TagContent.DRILL_MINEABLE, electricSettings("hand_drill").attributes(PickaxeItem.createAttributes(ELECTRIC_MATERIAL, 1f, -2.4f))));
    
    public static final RegistrySupplier<Item> PROMETHIUM_AXE = registerTool("promethium_axe", new PromethiumAxeItem(PROMETHIUM_MATERIAL, unbreakingSettings("promethium_axe").rarity(Rarity.EPIC).attributes(PromethiumPickaxeItem.createPromethiumAttributes(PROMETHIUM_MATERIAL, 12f, -2.1f, 2))));
    public static final RegistrySupplier<Item> PROMETHIUM_PICKAXE = registerTool("promethium_pickaxe", new PromethiumPickaxeItem(PROMETHIUM_MATERIAL, TagContent.DRILL_MINEABLE, unbreakingSettings("promethium_pickaxe").rarity(Rarity.EPIC).attributes(PromethiumPickaxeItem.createPromethiumAttributes(PROMETHIUM_MATERIAL, 3f, -2.4f, 2))));
    
    private static RegistrySupplier<Item> registerTool(String path, Item value) {
        var supplier = ItemContent.registerItem(path, value);
        postProcessField(value, supplier);
        return supplier;
    }

    private static Item.Properties unbreakingSettings(String path) {
        return ITEMS.properties(path)
          .stacksTo(1)
          .durability(0)
          .component(DataComponents.UNBREAKABLE, new Unbreakable(true));
    }

    private static Item.Properties electricSettings(String path) {
        return unbreakingSettings(path);
    }

    private static Item.Properties jetpackSettings(String path) {
        return unbreakingSettings(path);
    }

    private static void postProcessField(Item value, RegistrySupplier<Item> supplier) {
        
        var targetGroup = ItemContent.Groups.equipment;
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
                
                player.level().playSound(null, player.blockPosition(), SoundContent.SHORT_SERVO.get(), SoundSource.PLAYERS, 0.2f, 1.0f);
                
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        });
        
    }

    public static void register() {
    }
    
}
