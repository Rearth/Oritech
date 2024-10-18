package rearth.oritech.item.tools.armor;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import rearth.oritech.client.renderers.LaserArmRenderer;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.FluidStack;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

import static rearth.oritech.item.tools.harvesting.ChainsawItem.BAR_STEP_COUNT;

public interface BaseJetpackItem extends OritechEnergyItem {
    
    boolean requireUpward();
    int getRfUsage();
    int getFuelUsage();
    long getFuelCapacity();
    float getSpeed();
    
    default void tickJetpack(ItemStack stack, Entity entity) {
        
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
        if (!requireUpward()) isActive = up || horizontal;
        
        if (!isActive || player.isOnGround() || player.isSubmergedInWater()) return;
        
        var powerMultiplier = getSpeed();
        
        // try using energy/fuel
        if (tryUseFluid(stack)) {
            powerMultiplier *= 2.5f;
        } else if (!tryUseEnergy(stack, getRfUsage())) {
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
        
        // todo particles
    }
    
    private static void processUpwardsMotion(PlayerEntity player, float powerMultiplier, boolean upOnly) {
        var velocity = player.getMovement();
        
        var verticalMultiplier = LaserArmRenderer.lerp(powerMultiplier, 1, 0.6f);
        var power = 0.14f * verticalMultiplier;
        var dampeningFactor = 1.7f;
        
        if (!upOnly) power *= 0.7f;
        
        var speed = Math.max(velocity.y, 0.8);
        var addedVelocity = power / Math.pow(speed, dampeningFactor);
        
        player.setVelocity(velocity.add(0, addedVelocity, 0));
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
    
    default boolean tryUseFluid(ItemStack stack) {
        var fluidStack = getStoredFluid(stack);
        if (fluidStack.amount() < getFuelUsage() || !isValidFuel(fluidStack.variant()))
            return false;
        var res = new FluidStack(fluidStack.variant(), fluidStack.amount() - getFuelUsage());
        stack.set(ComponentContent.STORED_FLUID, res);
        return true;
    }
    
    default FluidStack getStoredFluid(ItemStack stack) {
        return stack.getOrDefault(ComponentContent.STORED_FLUID, new FluidStack(FluidVariant.blank(), 0));
    }
    
    default void addJetpackTooltip(ItemStack stack, List<Text> tooltip, boolean includeEnergy) {
        var text = Text.translatable("tooltip.oritech.energy_indicator", this.getStoredEnergy(stack), this.getEnergyCapacity(stack));
        if (includeEnergy) tooltip.add(text.formatted(Formatting.GOLD));
        
        var container = getStoredFluid(stack);
        var fluidText = Text.translatable("tooltip.oritech.jetpack_fuel", container.amount() * 1000 / FluidConstants.BUCKET, getFuelCapacity() * 1000 / FluidConstants.BUCKET, FluidVariantAttributes.getName(container.variant()).getString());
        tooltip.add(fluidText);
    }
    
    default int getJetpackBarColor(ItemStack stack) {
        
        var fluidStack = getStoredFluid(stack);
        if (fluidStack.amount() > getFuelUsage() && isValidFuel(fluidStack.variant())) {
            return 0xff1f8f;
        }
        
        return 0xff7007;
    }
    
    default int getJetpackBarStep(ItemStack stack) {
        
        var fluidStack = getStoredFluid(stack);
        if (fluidStack.amount() > getFuelUsage() && isValidFuel(fluidStack.variant())) {
            var fillPercent = fluidStack.amount() * 100 / getFuelCapacity();
            return Math.round(fillPercent * BAR_STEP_COUNT) / 100;
        }
        
        var energyItem = (SimpleEnergyItem) stack.getItem();
        return Math.round((energyItem.getStoredEnergy(stack) * 100f / energyItem.getEnergyCapacity(stack)) * BAR_STEP_COUNT) / 100;
    }
    
    default boolean isValidFuel(FluidVariant variant) {
        return variant.isOf(FluidContent.STILL_FUEL);
    }
    
    
}
