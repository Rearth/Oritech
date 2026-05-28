package rearth.oritech.item.tools.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.client.renderers.ExosuitArmorRenderer;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.init.ComponentContent;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.PlayState;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

// this item can store both energy and fluids
// applicable fluids will be consumed first, and then energy
// the fluid bar is rendered in a different color if a fluid is available
public class JetpackItem extends ArmorItem implements GeoItem, BaseJetpackItem {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // these are shared between all jetpacks
    // set to the level time where last ground contact was recorded
    public static long LAST_GROUND_CONTACT = Long.MAX_VALUE;
    // set to true if space has been pressed at least once AFTER loosing ground contact (to avoid flying forwards when dropping of a cliff
    public static boolean PRESSED_SPACE = false;
    
    public JetpackItem(Holder<ArmorMaterial> material, Type type, Item.Properties settings) {
        super(material, type, settings);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        
        if (level.isClientSide())
            tickJetpack(stack, entity, level);
    }
    
    @Override
    public int getDefaultMaxStackSize() {
        return 1;
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
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return OritechStartupConfig.basicJetpack.energyCapacity.get();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return OritechStartupConfig.basicJetpack.chargeSpeed.get();
    }
    
    public static void receiveUsagePacket(JetpackUsageUpdatePacket packet, IPayloadContext context) {
        var player = context.player();
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(stack.getItem() instanceof BaseJetpackItem)) return;
        
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        // to prevent dedicated servers from kicking the player for flying
        serverPlayer.connection.aboveGroundTickCount = 0;
        
        stack.set(EnergyApi.ITEM.getEnergyComponent(), packet.energyStored);
        if (packet.fluidAmount > 0)
            stack.set(ComponentContent.STORED_FLUID.get(), FluidStack.create(BuiltInRegistries.FLUID.get(Identifier.parse(packet.fluidType)), packet.fluidAmount));
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
