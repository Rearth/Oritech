package rearth.oritech.item.tools.armor;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
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

// this item can store both energy and fluids
// applicable fluids will be consumed first, and then energy
// the fluid bar is rendered in a different color if a fluid is available
public class JetpackItem extends ArmorItem implements GeoItem, BaseJetpackItem {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    
    // these are shared between all jetpacks
    // set to the world time where last ground contact was recorded
    public static long LAST_GROUND_CONTACT = Long.MAX_VALUE;
    // set to true if space has been pressed at least once AFTER loosing ground contact (to avoid flying forwards when dropping of a cliff
    public static boolean PRESSED_SPACE = false;
    
    public JetpackItem(RegistryEntry<ArmorMaterial> material, Type type, Item.Settings settings) {
        super(material, type, settings);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        
        if (world.isClient)
            tickJetpack(stack, entity, world);
    }
    
    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getItemBarColor(ItemStack stack) {
        return getJetpackBarColor(stack);
    }
    
    @Override
    public int getItemBarStep(ItemStack stack) {
        return getJetpackBarStep(stack);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        var hint = Text.translatable("tooltip.oritech.jetpack_usage").formatted(Formatting.GRAY, Formatting.ITALIC);
        tooltip.add(hint);
        hint = Text.translatable("tooltip.oritech.jetpack_usage2").formatted(Formatting.GRAY, Formatting.ITALIC);
        tooltip.add(hint);
        
        addJetpackTooltip(stack, tooltip, true);
    }
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?> renderer;
            
            @Override
            public @Nullable <T extends LivingEntity> BipedEntityModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable BipedEntityModel<T> original) {
                
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
        return Oritech.CONFIG.basicJetpack.speed();
    }
    
    @Override
    public int getRfUsage() {
        return Oritech.CONFIG.basicJetpack.energyUsage();
    }
    
    @Override
    public int getFuelUsage() {
        return Oritech.CONFIG.basicJetpack.fuelUsage();
    }
    
    @Override
    public long getFuelCapacity() {
        return Oritech.CONFIG.basicJetpack.fuelCapacity();
    }
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return Oritech.CONFIG.basicJetpack.energyCapacity();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return Oritech.CONFIG.basicJetpack.chargeSpeed();
    }
}
