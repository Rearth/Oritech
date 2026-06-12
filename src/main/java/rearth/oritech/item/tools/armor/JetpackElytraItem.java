package rearth.oritech.item.tools.armor;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.NonNull;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.ExosuitArmorRenderer;
import rearth.oritech.config.OritechStartupConfig;

import java.util.function.Consumer;

// this item can store both energy and fluids
// applicable fluids will be consumed first, and then energy
// the fluid bar is rendered in a different color if a fluid is available
public class JetpackElytraItem extends Item implements GeoItem, BaseJetpackItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final ArmorType type;

    public JetpackElytraItem(ArmorMaterial material, ArmorType type, Item.Properties settings) {
        super(settings);
        this.type = type;
    }

    @Override
    public int getDefaultMaxStackSize() {
        return 1;
    }

    @Override
    public boolean requireTakeoff() {
        return false;
    }

    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() && Minecraft.getInstance().player.isFallFlying()) {
            tickJetpack(stack, entity, level);
        }
    }

    public boolean useCustomElytra(LivingEntity entity, ItemStack chestStack, boolean tickElytra) {
        if (!tickElytra) return true;

        int nextRoll = entity.getFallFlyingTicks() + 1;
        if (!entity.level().isClientSide() && nextRoll % 10 == 0) {
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag type) {
        super.appendHoverText(stack, context, display, builder, type);
        var hint = Component.translatable("tooltip.oritech.jetpack_usage").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        builder.accept(hint);
        hint = Component.translatable("tooltip.oritech.jetpack_usage2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        builder.accept(hint);
        addJetpackTooltip(stack, builder, true);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ExosuitArmorRenderer renderer;

            @Override
            public @NonNull GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack itemStack, EquipmentSlot equipmentSlot) {
                if (this.renderer == null)
                    this.renderer = new ExosuitArmorRenderer(Oritech.id("armor/basic_jetpack"), Oritech.id("armor/exo_armor"));

                return this.renderer;
            }
        });
    }

    // Let's add our animation controller
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("base_controller", 20, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public int getEnergyCapacity() {
        return OritechStartupConfig.elytraJetpack.energyCapacity.get();
    }

    @Override
    public int getMaxRFInputRate() {
        return OritechStartupConfig.elytraJetpack.chargeSpeed.get();
    }

    @Override
    public int getMaxRFOutputRate() {
        return OritechStartupConfig.elytraJetpack.energyUsage.get();
    }

    @Override
    public boolean requireUpward() {
        return true;
    }

    @Override
    public float getSpeed() {
        return OritechStartupConfig.elytraJetpack.speed.get().floatValue();
    }

    @Override
    public int getRfUsage() {
        return OritechStartupConfig.elytraJetpack.energyUsage.get();
    }

    @Override
    public int getFuelUsage() {
        return OritechStartupConfig.elytraJetpack.fuelUsage.get();
    }

    @Override
    public long getFuelCapacity() {
        return OritechStartupConfig.elytraJetpack.fuelCapacity.get();
    }
}
