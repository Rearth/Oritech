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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.ExosuitArmorRenderer;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.init.ComponentContent;

import java.util.function.Consumer;

// this item can store both energy and fluids
// applicable fluids will be consumed first, and then energy
// the fluid bar is rendered in a different color if a fluid is available
public class JetpackItem extends Item implements GeoItem, BaseJetpackItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // these are shared between all jetpacks
    // set to the level time where last ground contact was recorded
    public static long LAST_GROUND_CONTACT = Long.MAX_VALUE;
    // set to true if space has been pressed at least once AFTER loosing ground contact (to avoid flying forwards when dropping of a cliff
    public static boolean PRESSED_SPACE = false;

    private final ArmorType type;

    public JetpackItem(ArmorMaterial material, ArmorType type, Item.Properties settings) {
        super(settings);
        this.type = type;
    }

    @Override
    public int getDefaultMaxStackSize() {
        return 1;
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
        return OritechStartupConfig.basicJetpack.energyCapacity.get();
    }

    @Override
    public int getMaxRFInputRate() {
        return OritechStartupConfig.basicJetpack.chargeSpeed.get();
    }

    @Override
    public int getMaxRFOutputRate() {
        return OritechStartupConfig.basicJetpack.energyUsage.get();
    }

    @Override
    public boolean requireUpward() {
        return true;
    }

    @Override
    public float getSpeed() {
        return OritechStartupConfig.basicJetpack.speed.get().floatValue();
    }

    @Override
    public int getRfUsage() {
        return OritechStartupConfig.basicJetpack.energyUsage.get();
    }

    @Override
    public int getFuelUsage() {
        return OritechStartupConfig.basicJetpack.fuelUsage.get();
    }

    @Override
    public long getFuelCapacity() {
        return OritechStartupConfig.basicJetpack.fuelCapacity.get();
    }

    public static void receiveUsagePacket(JetpackUsageUpdatePacket packet, IPayloadContext context) {
        var player = context.player();
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(stack.getItem() instanceof BaseJetpackItem)) return;

        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // to prevent dedicated servers from kicking the player for flying
        serverPlayer.connection.aboveGroundTickCount = 0;

        stack.set(ComponentContent.ENERGY, (int) packet.energyStored);
        if (packet.fluidAmount > 0) {
            var fluidHolder = BuiltInRegistries.FLUID.get(Identifier.parse(packet.fluidType));
            fluidHolder.ifPresent(fluidReference -> stack.set(ComponentContent.STORED_FLUID, SimpleFluidContent.copyOf(new FluidStack(fluidReference, (int) packet.fluidAmount))));
        }
    }

    public record JetpackUsageUpdatePacket(long energyStored, String fluidType,
                                           long fluidAmount) implements CustomPacketPayload {

        public static final Type<JetpackUsageUpdatePacket> PACKET_ID = new Type<>(Oritech.id("jetpack_use"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
