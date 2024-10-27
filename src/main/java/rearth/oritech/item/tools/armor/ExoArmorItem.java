package rearth.oritech.item.tools.armor;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
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
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
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
import java.util.function.Consumer;

public class ExoArmorItem extends ArmorItem implements GeoItem, ArmorEventHandler {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public ExoArmorItem(RegistryEntry<ArmorMaterial> material, Type type, Settings settings) {
        super(material, type, settings);
    }
    
    @Override
    public ComponentMap getComponents() {
        return super.getComponents();
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
    public AttributeModifiersComponent getAttributeModifiers() {
        var slotType = this.getSlotType();
        if (slotType != EquipmentSlot.LEGS) return super.getAttributeModifiers();
        
        return super.getAttributeModifiers()
                 .with(EntityAttributes.GENERIC_MOVEMENT_SPEED, new EntityAttributeModifier(Oritech.id("exo_move_speed"), 0.2, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE), AttributeModifierSlot.LEGS)
                 .with(EntityAttributes.GENERIC_FLYING_SPEED, new EntityAttributeModifier(Oritech.id("exo_fly_speed"), 0.2, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE), AttributeModifierSlot.LEGS);
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
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?> renderer;
            
            @Override
            public @Nullable <T extends LivingEntity> BipedEntityModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable BipedEntityModel<T> original) {
                
                if (this.renderer == null)
                    this.renderer = new ExosuitArmorRenderer(getModel(), Oritech.id("armor/exo_armor"));
                
                return this.renderer;
            }
        });
    }
    
    public Identifier getModel() {
        return Oritech.id("armor/exo_armor");
    }
    
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
