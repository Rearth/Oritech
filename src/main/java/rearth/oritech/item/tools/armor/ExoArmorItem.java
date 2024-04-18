package rearth.oritech.item.tools.armor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import rearth.oritech.client.renderers.ExosuitArmorRenderer;
import rearth.oritech.item.tools.util.ArmorEventHandler;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExoArmorItem extends ArmorItem implements GeoItem, ArmorEventHandler {
    
    // Thanks for being private, and TR for including it
    public static final UUID[] MODIFIERS = new UUID[]{
      UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"),
      UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"),
      UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"),
      UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")
    };
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    
    public ExoArmorItem(ArmorMaterial material, Type type, Settings settings) {
        super(material, type, settings.maxDamage(-1));
    }
    
    @Override
    public boolean isDamageable() {
        return false;
    }
    
    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return false;
    }
    
    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        
        var modifiers = ArrayListMultimap.create(super.getAttributeModifiers(slot));
        
        var speed = 0.2f;
        
        if (slot == EquipmentSlot.LEGS && this.getSlotType() == EquipmentSlot.LEGS) {
            modifiers.removeAll(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            modifiers.removeAll(EntityAttributes.GENERIC_FLYING_SPEED);
            modifiers.put(EntityAttributes.GENERIC_MOVEMENT_SPEED, new EntityAttributeModifier(MODIFIERS[slot.getEntitySlotId()], "Movement Speed", speed, EntityAttributeModifier.Operation.MULTIPLY_BASE));
            modifiers.put(EntityAttributes.GENERIC_FLYING_SPEED, new EntityAttributeModifier(MODIFIERS[slot.getEntitySlotId()], "Flying Speed", speed, EntityAttributeModifier.Operation.MULTIPLY_BASE));
        }
        
        return ImmutableMultimap.copyOf(modifiers);
        
    }
    
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
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private GeoArmorRenderer<?> renderer;
            
            @Override
            public BipedEntityModel<LivingEntity> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, BipedEntityModel<LivingEntity> original) {
                
                if (this.renderer == null)
                    this.renderer = new ExosuitArmorRenderer();
                // This prepares our GeoArmorRenderer for the current render frame.
                // These parameters may be null however, so we don't do anything further with them
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                
                return this.renderer;
                
            }
        });
    }
    
    @Override
    public Supplier<Object> getRenderProvider() {
        return this.renderProvider;
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
}
