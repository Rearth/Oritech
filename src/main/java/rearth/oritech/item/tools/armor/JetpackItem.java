package rearth.oritech.item.tools.armor;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.client.renderers.ExosuitArmorRenderer;
import rearth.oritech.client.renderers.LaserArmRenderer;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.FluidStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;
import java.util.function.Consumer;

import static rearth.oritech.item.tools.harvesting.ChainsawItem.BAR_STEP_COUNT;

// this item can store both energy and fluids
// applicable fluids will be consumed first, and then energy
// the fluid bar is rendered in a different color if a fluid is available
public class JetpackItem extends ArmorItem implements GeoItem, OritechEnergyItem {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private final long RF_USAGE = 64;
    private final long FUEL_USAGE = 10 * (FluidConstants.BUCKET / 1000);
    private final long FUEL_CAPACITY = 4 * FluidConstants.BUCKET;
    
    private final boolean requireUpward = true;
    
    public JetpackItem(RegistryEntry<ArmorMaterial> material, Type type, Item.Settings settings) {
        super(material, type, settings);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        
        if (!world.isClient) return;
        
        if (!(entity instanceof PlayerEntity player)) return;
        
        var isEquipped = player.getEquippedStack(EquipmentSlot.CHEST).equals(stack);
        if (!isEquipped) return;
        
        var client = MinecraftClient.getInstance();
        
        var up = client.options.jumpKey.isPressed();
        var forward = client.options.forwardKey.isPressed();
        var backward = client.options.backKey.isPressed();
        var left = client.options.leftKey.isPressed();
        var right = client.options.rightKey.isPressed();
        
        var horizontal = forward || backward || left || right;
        var upOnly = up && !horizontal;
        
        var isActive = up;
        if (!requireUpward) isActive = up || horizontal;
        
        if (!isActive || player.isOnGround() || player.isSubmergedInWater()) return;
        
        var powerMultiplier = 1f;
        
        // try using energy/fuel
        if (tryUseFluid(stack)) {
            powerMultiplier *= 2.5f;
        } else if (!tryUseEnergy(stack, RF_USAGE)) {
            return;
        }
        
        if (up) {
            processUpwardsMotion(player, powerMultiplier, upOnly);
        } else {
            powerMultiplier *= 0.7f;    // slower forward while not going up
        }
        
        if (forward || backward)
            processForwardMotion(player, forward, powerMultiplier);
        
        if (left || right)
            processSideMotion(player, right, powerMultiplier);
        
        var fluidStack = getStoredFluid(stack);
        var fluid = Registries.FLUID.getId(fluidStack.variant().getFluid());
        // this will currently only for instances of this class
        NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.JetpackUsageUpdatePacket(getStoredEnergy(stack), fluid.toString(), fluidStack.amount()));
    }
    
    private static void processSideMotion(PlayerEntity player, boolean right, float powerMultiplier) {
        var modifier = right ? 1 : -1;  // either go full speed ahead, or slowly backwards
        var power = 0.07f * powerMultiplier;
        
        // get existing movement
        var movement = player.getMovement();
        var horizontalMovement = new Vec3d(movement.x, 0, movement.z);
        
        // get player facing
        var playerForward = player.getRotationVecClient();
        playerForward = new Vec3d(playerForward.x, 0, playerForward.z).normalize();
        var playerRight = playerForward.rotateY(-90);
        
        // apply forward / back
        horizontalMovement = horizontalMovement.add(playerRight.multiply(modifier * power));
        
        player.setVelocity(horizontalMovement.x, movement.y, horizontalMovement.z);
    }
    
    private static void processForwardMotion(PlayerEntity player, boolean forward, float powerMultiplier) {
        var modifier = forward ? 1f : -0.4;  // either go full speed ahead, or slowly backwards
        var power = 0.1f * powerMultiplier;
        
        // get existing movement
        var movement = player.getMovement();
        var horizontalMovement = new Vec3d(movement.x, 0, movement.z);
        
        // get player facing
        var playerForward = player.getRotationVecClient();
        playerForward = new Vec3d(playerForward.x, 0, playerForward.z).normalize();
        
        // apply forward / back
        horizontalMovement = horizontalMovement.add(playerForward.multiply(modifier * power));
        
        player.setVelocity(horizontalMovement.x, movement.y, horizontalMovement.z);
    }
    
    private static void processUpwardsMotion(PlayerEntity player, float powerMultiplier, boolean upOnly) {
        var velocity = player.getMovement();
        
        var verticalMultiplier = LaserArmRenderer.lerp(powerMultiplier, 1, 0.6f);
        var power = 0.17f * verticalMultiplier;
        var dampeningFactor = 1.7f;
        
        if (!upOnly) power *= 0.7f;
        
        var speed = Math.max(velocity.y, 0.8);
        var addedVelocity = power / Math.pow(speed, dampeningFactor);
        
        player.setVelocity(velocity.add(0, addedVelocity, 0));
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
        
        var fluidStack = getStoredFluid(stack);
        if (fluidStack.amount() > FUEL_USAGE && fluidStack.variant() == FluidVariant.of(FluidContent.STILL_FUEL)) {
            return 0xff1f8f;
        }
        
        return 0xff7007;
    }
    
    @Override
    public int getItemBarStep(ItemStack stack) {
        
        var fluidStack = getStoredFluid(stack);
        if (fluidStack.amount() > FUEL_USAGE && fluidStack.variant() == FluidVariant.of(FluidContent.STILL_FUEL)) {
            var fillPercent = fluidStack.amount() * 100 / FUEL_CAPACITY;
            return Math.round(fillPercent * BAR_STEP_COUNT) / 100;
        }
        
        var energyItem = (SimpleEnergyItem) stack.getItem();
        return Math.round((energyItem.getStoredEnergy(stack) * 100f / energyItem.getEnergyCapacity(stack)) * BAR_STEP_COUNT) / 100;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        var text = Text.translatable("tooltip.oritech.energy_indicator", this.getStoredEnergy(stack), this.getEnergyCapacity(stack));
        tooltip.add(text.formatted(Formatting.GOLD));
        
        var container = getStoredFluid(stack);
        var fluidText = Text.translatable("tooltip.oritech.jetpack_fuel", container.amount() * 1000 / FluidConstants.BUCKET, FUEL_CAPACITY * 1000 / FluidConstants.BUCKET, FluidVariantAttributes.getName(container.variant()).getString());
        tooltip.add(fluidText);
    }
    
    public boolean tryUseFluid(ItemStack stack) {
        var fluidStack = getStoredFluid(stack);
        if (fluidStack.amount() < FUEL_USAGE || fluidStack.variant() != FluidVariant.of(FluidContent.STILL_FUEL))
            return false;
        var res = new FluidStack(fluidStack.variant(), fluidStack.amount() - FUEL_USAGE);
        stack.set(ComponentContent.STORED_FLUID, res);
        return true;
    }
    
    public FluidStack getStoredFluid(ItemStack stack) {
        return stack.getOrDefault(ComponentContent.STORED_FLUID, new FluidStack(FluidVariant.blank(), 0));
    }
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?> renderer;
            
            @Override
            public @Nullable <T extends LivingEntity> BipedEntityModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable BipedEntityModel<T> original) {
                
                if (this.renderer == null)
                    this.renderer = new ExosuitArmorRenderer();
                
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
}
