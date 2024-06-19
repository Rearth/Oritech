package rearth.oritech.item.tools.armor;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.client.renderers.ExosuitArmorRenderer;
import rearth.oritech.item.tools.util.ArmorEventHandler;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ExoArmorItem extends ArmorItem implements GeoItem, ArmorEventHandler {
    
    // Thanks for being private, and TR for including it
    public static final UUID[] MODIFIERS = new UUID[]{
      UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"),
      UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"),
      UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"),
      UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")
    };
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public ExoArmorItem(RegistryEntry<ArmorMaterial> material, Type type, Settings settings) {
        super(material, type, settings.maxDamage(-1));
    }
    
    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return false;
    }
    
//    @Override
//    public AttributeModifiersComponent getAttributeModifiers() {
//
//        // TODO
//
//        var modifiers = super.getAttributeModifiers().modifiers();
//        var identifier = Identifier.ofVanilla("armor." + type.getName());
//        modifiers.add(new AttributeModifiersComponent.Entry(EntityAttributes.GENERIC_MOVEMENT_SPEED, new EntityAttributeModifier(identifier, 1, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.LEGS));
//
//        var speed = 0.2f;
//
//        if (slot == EquipmentSlot.LEGS && this.getSlotType() == EquipmentSlot.LEGS) {
//            modifiers.removeAll(EntityAttributes.GENERIC_MOVEMENT_SPEED);
//            modifiers.removeAll(EntityAttributes.GENERIC_FLYING_SPEED);
//            modifiers.put(EntityAttributes.GENERIC_MOVEMENT_SPEED, new EntityAttributeModifier(MODIFIERS[slot.getEntitySlotId()], "Movement Speed", speed, EntityAttributeModifier.Operation.MULTIPLY_BASE));
//            modifiers.put(EntityAttributes.GENERIC_FLYING_SPEED, new EntityAttributeModifier(MODIFIERS[slot.getEntitySlotId()], "Flying Speed", speed, EntityAttributeModifier.Operation.MULTIPLY_BASE));
//        }
//
//        return ImmutableMultimap.copyOf(modifiers);
//
//    }
    
    @Override
    public void onEquipped(PlayerEntity playerEntity, ItemStack stack) {
        
        if (this.getSlotType() == EquipmentSlot.HEAD)
            playerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
    }
    
    @Override
    public void onUnequipped(PlayerEntity playerEntity, ItemStack stack) {
        
        if (this.getSlotType() == EquipmentSlot.HEAD)
            playerEntity.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
    
    // Create our armor model/renderer for Fabric and return it
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?> renderer;
            
            @Override
            public @Nullable <T extends LivingEntity> BipedEntityModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable BipedEntityModel<T> original) {
                
                if (this.renderer == null)
                    this.renderer = new ExosuitArmorRenderer();
                // This prepares our GeoArmorRenderer for the current render frame.
                // These parameters may be null however, so we don't do anything further with them
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                
                return this.renderer;
            }
        });
    }
    
    // Let's add our animation controller
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 20, state -> PlayState.STOP));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("tooltip.oritech." + Registries.ITEM.getId(stack.getItem()).getPath()).formatted(Formatting.GRAY));
    }
}
