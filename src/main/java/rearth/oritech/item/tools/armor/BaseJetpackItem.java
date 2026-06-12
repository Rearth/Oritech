package rearth.oritech.item.tools.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.client.renderers.LaserArmRenderer;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.util.TooltipHelper;

import java.util.List;

import static rearth.oritech.item.tools.harvesting.ChainsawItem.BAR_STEP_COUNT;


public interface BaseJetpackItem extends OritechEnergyItem, FluidProvider.Item {

    boolean requireUpward();

    int getRfUsage();

    int getFuelUsage();

    long getFuelCapacity();

    float getSpeed();

    default boolean requireTakeoff() {
        return true;
    }

    default void tickJetpack(ItemStack stack, Entity entity, Level level) {

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

        if (requireTakeoff() && !isJetpackStarted(player, level, up)) return;

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
        PacketDistributor.sendToServer(new JetpackItem.JetpackUsageUpdatePacket(getStoredEnergy(stack, ItemAccess.forStack(stack)), fluid.toString(), fluidStack.getAmount()));

        var playerForward = player.getForward();
        var playerRight = playerForward.normalize().yRot(-90);
        var particleCenter = player.getEyePosition().add(0, -1.1, 0).subtract(playerForward.scale(0.2f));
        var particlePosA = particleCenter.add(playerRight.scale(0.4f));
        var particlePosB = particleCenter.add(playerRight.scale(-0.4f));

        var direction = new Vec3(0, -1, 0);
        if (forward) direction = playerForward.normalize().scale(-1).add(0, -1, 0);

        level.addParticle(ParticleTypes.SMOKE,
                particlePosA.x + (level.getRandom().nextDouble() - 0.5) * 0.2,
                particlePosA.y + (level.getRandom().nextDouble() - 0.5) * 0.2,
                particlePosA.z + (level.getRandom().nextDouble() - 0.5) * 0.2,
                direction.x, direction.y, direction.z);
        level.addParticle(ParticleTypes.SMOKE,
                particlePosB.x + (level.getRandom().nextDouble() - 0.5) * 0.2,
                particlePosB.y + (level.getRandom().nextDouble() - 0.5) * 0.2,
                particlePosB.z + (level.getRandom().nextDouble() - 0.5) * 0.2,
                direction.x, direction.y, direction.z);
    }

    private static boolean isJetpackStarted(Player player, Level level, boolean up) {

        var grounded = player.onGround() || player.isUnderWater();

        if (grounded) {
            JetpackItem.LAST_GROUND_CONTACT = level.getGameTime();
            JetpackItem.PRESSED_SPACE = false;
            return false;
        } else {
            var flightTime = level.getGameTime() - JetpackItem.LAST_GROUND_CONTACT;

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
        var res = fluidStack.copyWithAmount(fluidStack.getAmount() - getFuelUsage());
        stack.set(ComponentContent.STORED_FLUID.get(), res);
        return true;
    }

    default FluidStack getStoredFluid(ItemStack stack) {
        return stack.getOrDefault(ComponentContent.STORED_FLUID.get(), FluidStack.EMPTY);
    }

    default void addJetpackTooltip(ItemStack stack, Consumer<Component> builder, boolean includeEnergy) {

        var text = Component.translatable("tooltip.oritech.energy_indicator", TooltipHelper.getEnergyText(this.getStoredEnergy(stack, ItemAccess.forStack(stack))), TooltipHelper.getEnergyText(this.getCapacity()));
        if (includeEnergy) builder.accept(text.withStyle(ChatFormatting.GOLD));

        var container = getStoredFluid(stack);
        var fluidText = Component.translatable("tooltip.oritech.jetpack_fuel", container.getAmount() * 1000 / FluidType.BUCKET_VOLUME, getFuelCapacity() * 1000 / FluidType.BUCKET_VOLUME, container.getHoverName().getString());
        builder.accept(fluidText);
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

        return Math.round((getStoredEnergy(stack, ItemAccess.forStack(stack)) * 100f / this.getCapacity()) * BAR_STEP_COUNT) / 100;
    }

    default boolean isValidFuel(Fluid variant) {
        return BuiltInRegistries.FLUID.wrapAsHolder(variant).is(TagContent.TURBOFUEL);
    }

    @Override
    default int getCapacity() {
        return (int) getFuelCapacity();
    }
}
