package rearth.oritech.item.tools.armor;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.ExosuitArmorRenderer;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

// this item can store both energy and fluids
// applicable fluids will be consumed first, and then energy
// the fluid bar is rendered in a different color if a fluid is available
public class JetpackElytraItem extends ArmorItem implements GeoItem, BaseJetpackItem {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public JetpackElytraItem(Holder<ArmorMaterial> material, Type type, Item.Properties settings) {
        super(material, type, settings);
    }
    
    @Override
    public boolean requireTakeoff() {
        return false;
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        
        if (world.isClientSide && Minecraft.getInstance().player.isFallFlying()) {
            tickJetpack(stack, entity, world);
        }
    }
    
    public boolean useCustomElytra(LivingEntity entity, ItemStack chestStack, boolean tickElytra) {
        if (!tickElytra) return true;
        
        int nextRoll = entity.getFallFlyingTicks() + 1;
        if (!entity.level().isClientSide && nextRoll % 10 == 0) {
            entity.gameEvent(GameEvent.ELYTRA_GLIDE);
        }
        
        return true;
    }
    
    // this overrides the IItemExtension methods in neoforge
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return useCustomElytra(entity, entity.getItemBySlot(EquipmentSlot.CHEST), true);
    }
    
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        return true;
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getBarColor(ItemStack stack) {
        return getJetpackBarColor(stack);
    }
    
    @Override
    public int getBarWidth(ItemStack stack) {
        return getJetpackBarStep(stack);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        var hint = Component.translatable("tooltip.oritech.jetpack_usage").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        tooltip.add(hint);
        hint = Component.translatable("tooltip.oritech.jetpack_usage2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        tooltip.add(hint);
        addJetpackTooltip(stack, tooltip, true);
    }
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?> renderer;
            
            @Override
            public @Nullable <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                
                if (this.renderer == null)
                    this.renderer = new ExosuitArmorRenderer(Oritech.id("armor/basic_jetpack"), Oritech.id("armor/basic_jetpack"));
                
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
    public boolean requireUpward() {
        return true;
    }
    
    @Override
    public float getSpeed() {
        return Oritech.CONFIG.elytraJetpack.speed();
    }
    
    @Override
    public int getRfUsage() {
        return (int) Oritech.CONFIG.elytraJetpack.energyUsage();
    }
    
    @Override
    public int getFuelUsage() {
        return Oritech.CONFIG.elytraJetpack.fuelUsage();
    }
    
    @Override
    public long getFuelCapacity() {
        return Oritech.CONFIG.elytraJetpack.fuelCapacity();
    }
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return Oritech.CONFIG.elytraJetpack.energyCapacity();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return Oritech.CONFIG.elytraJetpack.chargeSpeed();
    }
}
