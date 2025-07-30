package rearth.oritech.item.tools.armor;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleItemFluidStorage;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.renderers.LaserArmRenderer;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.item.tools.util.OritechEnergyItem;

import rearth.oritech.util.TooltipHelper;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

import static rearth.oritech.item.tools.harvesting.ChainsawItem.BAR_STEP_COUNT;


public interface BaseJetpackItem extends OritechEnergyItem, FluidApi.ItemProvider {
    
    boolean requireUpward();
    int getRfUsage();
    int getFuelUsage();
    long getFuelCapacity();
    float getSpeed();
    
    default boolean requireTakeoff() {return true;}
    
    default void tickJetpack(ItemStack stack, Entity entity, Level world) {
        
        if (!(entity instanceof Player player)) return;
        
        var isEquipped = player.getItemBySlot(EquipmentSlot.CHEST).equals(stack);
        if (!isEquipped) return;
        
        var client = Minecraft.getInstance();
        
        var up = client.options.keyJump.isDown();
        var forward = client.options.keyUp.isDown();
        var backward = client.options.keyDown.isDown();
        var left = client.options.keyLeft.isDown();
        var right = client.options.keyRight.isDown();
        
        var horizontal = forward || backward || left || right;
        var upOnly = up && !horizontal;
        
        var isActive = up;
        if (!requireUpward()) isActive = up || horizontal;
        
        if (requireTakeoff() && !isJetpackStarted(player, world, up)) return;
        
        if (!isActive || player.onGround() || player.isUnderWater()) return;
        
        var powerMultiplier = getSpeed();
        
        // try using energy/fuel
        if (tryUseFluid(stack)) {
            powerMultiplier *= 2.5f;
        } else if (!tryUseEnergy(stack, getRfUsage(), player)) {
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
        var fluid = BuiltInRegistries.FLUID.getKey(fluidStack.getFluid());
        
        // this will currently only for instances of this class
        NetworkManager.sendToServer(new JetpackItem.JetpackUsageUpdatePacket(getStoredEnergy(stack), fluid.toString(), fluidStack.getAmount()));
        
        var playerForward = player.getForward();
        var playerRight = playerForward.normalize().yRot(-90);
        var particleCenter = player.getEyePosition().add(0, -1.1, 0).subtract(playerForward.scale(0.2f));
        var particlePosA = particleCenter.add(playerRight.scale(0.4f));
        var particlePosB = particleCenter.add(playerRight.scale(-0.4f));
        
        var direction = new Vec3(0, -1, 0);
        if (forward) direction = playerForward.normalize().scale(-1).add(0, -1, 0);
        
        ParticleContent.JETPACK_EXHAUST.spawn(world, particlePosA, direction);
        ParticleContent.JETPACK_EXHAUST.spawn(world, particlePosB, direction);
    }
    
    private static boolean isJetpackStarted(Player player, Level world, boolean up) {
        
        var grounded = player.onGround() || player.isUnderWater();
        
        if (grounded) {
            JetpackItem.LAST_GROUND_CONTACT = world.getGameTime();
            JetpackItem.PRESSED_SPACE = false;
            return false;
        } else {
            var flightTime = world.getGameTime() - JetpackItem.LAST_GROUND_CONTACT;
            
            if (flightTime < 5) return false;
            if (up) JetpackItem.PRESSED_SPACE = true;
            
            return JetpackItem.PRESSED_SPACE;
        }
    }
    
    private static void processUpwardsMotion(Player player, float powerMultiplier, boolean upOnly) {
        var velocity = player.getKnownMovement();
        
        var verticalMultiplier = LaserArmRenderer.lerp(powerMultiplier, 1, 0.6f);
        var power = 0.13f * verticalMultiplier;
        var dampeningFactor = 1.7f;
        
        if (!upOnly) power *= 0.7f;
        
        var speed = Math.max(velocity.y, 0.8);
        var addedVelocity = power / Math.pow(speed, dampeningFactor);
        
        player.setDeltaMovement(velocity.add(0, addedVelocity, 0));
    }
    
    private static void processSideMotion(Player player, boolean right, float powerMultiplier) {
        var modifier = right ? 1 : -1;  // either go full speed ahead, or slowly backwards
        var power = 0.04f * powerMultiplier;
        
        // get existing movement
        var movement = player.getKnownMovement();
        var horizontalMovement = new Vec3(movement.x, 0, movement.z);
        
        // get player facing
        var playerForward = player.getForward();
        playerForward = new Vec3(playerForward.x, 0, playerForward.z).normalize();
        var playerRight = playerForward.yRot(-90);
        
        // apply forward / back
        horizontalMovement = horizontalMovement.add(playerRight.scale(modifier * power));
        
        player.setDeltaMovement(horizontalMovement.x, movement.y, horizontalMovement.z);
    }
    
    private static void processForwardMotion(Player player, boolean forward, float powerMultiplier) {
        var modifier = forward ? 1f : -0.4;  // either go full speed ahead, or slowly backwards
        var power = 0.06f * powerMultiplier;
        
        // get existing movement
        var movement = player.getKnownMovement();
        var horizontalMovement = new Vec3(movement.x, 0, movement.z);
        
        // get player facing
        var playerForward = player.getForward();
        playerForward = new Vec3(playerForward.x, 0, playerForward.z).normalize();
        
        // apply forward / back
        horizontalMovement = horizontalMovement.add(playerForward.scale(modifier * power));
        
        player.setDeltaMovement(horizontalMovement.x, movement.y, horizontalMovement.z);
    }
    
    default boolean tryUseFluid(ItemStack stack) {
        var fluidStack = getStoredFluid(stack);
        if (fluidStack.getAmount() < getFuelUsage() || !isValidFuel(fluidStack.getFluid()))
            return false;
        var res = FluidStack.create(fluidStack.getFluid(), fluidStack.getAmount() - getFuelUsage());
        stack.set(ComponentContent.STORED_FLUID.get(), res);
        return true;
    }
    
    default FluidStack getStoredFluid(ItemStack stack) {
        return stack.getOrDefault(ComponentContent.STORED_FLUID.get(), FluidStack.empty());
    }
    
    default void addJetpackTooltip(ItemStack stack, List<Component> tooltip, boolean includeEnergy) {
        
        var text = Component.translatable("tooltip.oritech.energy_indicator", TooltipHelper.getEnergyText(this.getStoredEnergy(stack)), TooltipHelper.getEnergyText(this.getEnergyCapacity(stack)));
        if (includeEnergy) tooltip.add(text.withStyle(ChatFormatting.GOLD));
        
        var container = getStoredFluid(stack);
        var fluidText = Component.translatable("tooltip.oritech.jetpack_fuel", container.getAmount() * 1000 / FluidStackHooks.bucketAmount(), getFuelCapacity() * 1000 / FluidStackHooks.bucketAmount(), FluidStackHooks.getName(container).getString());
        tooltip.add(fluidText);
    }
    
    default int getJetpackBarColor(ItemStack stack) {
        
        var fluidStack = getStoredFluid(stack);
        if (fluidStack.getAmount() > getFuelUsage() && isValidFuel(fluidStack.getFluid())) {
            return 0xbafc03;
        }
        
        return 0xff7007;
    }
    
    default int getJetpackBarStep(ItemStack stack) {
        
        var fluidStack = getStoredFluid(stack);
        if (fluidStack.getAmount() > getFuelUsage() && isValidFuel(fluidStack.getFluid())) {
            var fillPercent = fluidStack.getAmount() * 100 / getFuelCapacity();
            return Math.round(fillPercent * BAR_STEP_COUNT) / 100;
        }
        
        return Math.round((getStoredEnergy(stack) * 100f / this.getEnergyCapacity(stack)) * BAR_STEP_COUNT) / 100;
    }
    
    default boolean isValidFuel(Fluid variant) {
        return variant.isSame(FluidContent.STILL_FUEL.get());
    }
    
    @Override
    default FluidApi.SingleSlotStorage getFluidStorage(ItemStack stack) {
        return new SimpleItemFluidStorage(getFuelCapacity(), stack) {
            @Override
            public long insert(FluidStack toInsert, boolean simulate) {
                var valid = isValidFuel(toInsert.getFluid());
                if (!valid) return 0L;
                return super.insert(toInsert, simulate);
            }
        };
    }
}
