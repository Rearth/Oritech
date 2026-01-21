package rearth.oritech.item.tools.armor;

import dev.architectury.fluid.FluidStack;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.client.renderers.ExosuitArmorRenderer;
import rearth.oritech.init.ComponentContent;
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
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

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
    
    public JetpackItem(Holder<ArmorMaterial> material, Type type, Item.Properties settings) {
        super(material, type, settings);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        
        if (world.isClientSide)
            tickJetpack(stack, entity, world);
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
    
    public static void receiveUsagePacket(JetpackUsageUpdatePacket packet, Player player, RegistryAccess dynamicRegistryManager) {
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(stack.getItem() instanceof BaseJetpackItem)) return;
        
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        // to prevent dedicated servers from kicking the player for flying
        serverPlayer.connection.aboveGroundTickCount = 0;
        
        stack.set(EnergyApi.ITEM.getEnergyComponent(), packet.energyStored);
        if (packet.fluidAmount > 0)
            stack.set(ComponentContent.STORED_FLUID.get(), FluidStack.create(BuiltInRegistries.FLUID.get(ResourceLocation.parse(packet.fluidType)), packet.fluidAmount));
    }
    
    public record JetpackUsageUpdatePacket(long energyStored, String fluidType, long fluidAmount) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<JetpackUsageUpdatePacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("jetpack_use"));
        
        @Override
        public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
