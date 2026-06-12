package rearth.oritech.item.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.init.SoundContent;
import rearth.oritech.item.tools.harvesting.ChainsawItem;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.util.TooltipHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ElectricMaceItem extends MaceItem implements OritechEnergyItem {


    public static final Map<Long, Runnable> PENDING_LIGHTNING_HITS = new HashMap<>();

    public ElectricMaceItem(Properties settings) {
        super(settings);
    }

    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers
                .builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 8, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.4F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.SAFE_FALL_DISTANCE, new AttributeModifier(Oritech.id("mace_fall_protection"), 10, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(Oritech.id("mace_reach"), 3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        var bonus = 0f;

        var usedEnergy = tryUseEnergy(stack, OritechStartupConfig.electricMace.energyUsage.get(), null);
        if (usedEnergy && canSmashAttack(attacker)) {
            attacker.level().playSound(null, target.blockPosition(), SoundContent.ELECTRIC_SHOCK.value(), SoundSource.PLAYERS);
            attacker.resetFallDistance();
            bonus = getAttackDamageBonus(target, getAttackDamage(), attacker.level().damageSources().lightningBolt());
        }


        if (attacker instanceof Player player && attacker.level() instanceof ServerLevel serverLevel) {
            if (player.getCooldowns().isOnCooldown(stack))
                return;

            player.getCooldowns().addCooldown(stack, 40);
            createLightningAttack(serverLevel, player, target, stack, (int) (getAttackDamage() / 2f + bonus / 2f));
        }
    }

    private void createLightningAttack(ServerLevel level, Player attacker, LivingEntity target, ItemStack stack, int damage) {

        var usedEnergy = tryUseEnergy(stack, (int) OritechStartupConfig.electricMace.energyUsage.get() * OritechStartupConfig.electricMace.lightningCostMultiplier.get(), null);
        if (usedEnergy && attacker.level() instanceof ServerLevel serverLevel) {

            var playerPos = attacker.getEyePosition();
            var targetPos = target.getEyePosition();
            var offset = targetPos.subtract(playerPos);
            var up = new Vec3(0, 1, 0);
            var cross = offset.cross(up).normalize();
            var pos = targetPos.add(cross.scale(14)).offsetRandom(serverLevel.getRandom(), 3).add(0, 7, 0);

            createLightningBolt(serverLevel, pos, target.getEyePosition().offsetRandom(serverLevel.getRandom(), 0.1f), 10, 0.8f, 4, ParticleTypes.ENCHANTED_HIT, 0.35f, 2, 0);

            for (int i = 1; i <= 5; i++) {
                final var ownPos = targetPos.add(cross.yRot(i * 90).scale(14)).offsetRandom(serverLevel.getRandom(), 5).add(0, 7, 0);
                PENDING_LIGHTNING_HITS.put(serverLevel.getGameTime() + 10 * i, () -> {
                    createLightningBolt(serverLevel, ownPos, target.getEyePosition().offsetRandom(serverLevel.getRandom(), 0.1f), 10, 0.8f, 4, ParticleTypes.ENCHANTED_HIT, 0.35f, 2, 0);
                    target.hurtTime = 0;
                    target.hurt(level.damageSources().lightningBolt(), damage);
                });
            }
        }
    }

    public static void processLightningEvents(Level level) {
        var toRemove = new ArrayList<Long>();
        for (var entry : PENDING_LIGHTNING_HITS.entrySet()) {
            var key = entry.getKey();
            if (level.getGameTime() > key) {
                var event = entry.getValue();
                event.run();
                toRemove.add(key);
            }
        }

        toRemove.forEach(PENDING_LIGHTNING_HITS::remove);
    }

    @Override
    public float getAttackDamageBonus(Entity target, float baseAttackDamage, DamageSource damageSource) {

        var attacker = damageSource.getDirectEntity();
        if (attacker instanceof LivingEntity livingEntity) {
            if (!canSmashAttack(livingEntity)) {
                return 0.0F;
            } else {
                var fallDist = livingEntity.fallDistance;
                double damage;
                if (fallDist <= 3.0F) {
                    damage = getAttackDamage() * fallDist;
                } else if (fallDist <= 8.0F) {
                    damage = getAttackDamage() * 4 + 4.0F * (fallDist - 3.0F);
                } else {
                    damage = getAttackDamage() * 6 + fallDist - 8.0F;
                }

                var level = livingEntity.level();
                if (level instanceof ServerLevel serverLevel) {
                    return (float) (damage + EnchantmentHelper.modifyFallBasedDamage(serverLevel, livingEntity.getWeaponItem(), target, damageSource, 2.0F) * fallDist);
                } else {
                    return (float) damage;
                }
            }
        } else {
            return 0.0F;
        }
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

        var text = Component.translatable("tooltip.oritech.energy_indicator", TooltipHelper.getEnergyText(this.getStoredEnergy(itemStack, ItemAccess.forStack(itemStack))), TooltipHelper.getEnergyText(this.getCapacity()));
        builder.accept(text.withStyle(ChatFormatting.GOLD));

        var showExtra = Minecraft.getInstance().hasControlDown();

        if (showExtra) {
            builder.accept(Component.translatable("tooltip.oritech.electric_mace").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
            builder.accept(Component.translatable("tooltip.oritech.electric_mace.1").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        } else {
            builder.accept(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((getStoredEnergy(stack, ItemAccess.forStack(stack)) * 100f / this.getCapacity()) * ChainsawItem.BAR_STEP_COUNT) / 100;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xff7007;
    }

    // this overrides the neoforge specific extensions
    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        return false;
    }

    @Override
    public boolean isCombineRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    private int getAttackDamage() {
        return 8;
    }

    /**
     * Creates a lightning effect between two points.
     *
     * @param level              The ServerLevel to spawn the effect in.
     * @param startPos           The starting position of the lightning.
     * @param endPos             The ending position of the lightning.
     * @param mainSegments       Number of main segments for the lightning path (more = more jagged).
     * @param jitterAmount       Maximum random offset for each segment point.
     * @param particlesPerMeter  How many particles to spawn per meter of segment length.
     * @param particleEffect     The particle type to use (e.g., ParticleTypes.ELECTRIC_SPARK).
     * @param branchChance       Chance (0.0 to 1.0) for a branch to occur at a segment point.
     * @param maxBranchDepth     Maximum recursion depth for branches.
     * @param currentBranchDepth Current depth (used internally for recursion).
     */
    public static void createLightningBolt(ServerLevel level, Vec3 startPos, Vec3 endPos,
                                           int mainSegments, double jitterAmount,
                                           double particlesPerMeter, ParticleOptions particleEffect,
                                           float branchChance, int maxBranchDepth, int currentBranchDepth) {
        var random = level.getRandom();
        var direction = endPos.subtract(startPos);
        double totalDistance = direction.length();

        if (totalDistance < 0.1) { // Too short to draw
            // Optional: just spawn a particle at the point or do nothing
            level.sendParticles(particleEffect, startPos.x, startPos.y, startPos.z, 5, 0.1, 0.1, 0.1, 0.05);
            return;
        }

        var segmentVector = direction.normalize().scale(totalDistance / mainSegments);
        var previousPoint = startPos;

        // Sound for the main bolt (only if not a branch or first branch)
        if (currentBranchDepth == 0) {
            level.playSound(null, endPos.x, endPos.y, endPos.z, SoundContent.ELECTRIC_SHOCK, SoundSource.PLAYERS, 0.8f, 0.5f + level.getRandom().nextFloat() * 0.8f);
        }


        for (int i = 0; i < mainSegments; i++) {
            Vec3 currentTargetPoint;
            if (i < mainSegments - 1) {
                currentTargetPoint = startPos.add(segmentVector.scale(i + 1));
                // Add jitter
                currentTargetPoint = currentTargetPoint.add(
                        (random.nextDouble() - 0.5) * 2 * jitterAmount,
                        (random.nextDouble() - 0.5) * 2 * jitterAmount,
                        (random.nextDouble() - 0.5) * 2 * jitterAmount
                );
            } else {
                currentTargetPoint = endPos; // Ensure the last segment goes exactly to the end point
            }

            // Spawn particles along the segment from previousPoint to currentTargetPoint
            spawnParticlesAlongSegment(level, previousPoint, currentTargetPoint, particlesPerMeter, particleEffect);

            // Branching logic
            if (currentBranchDepth < maxBranchDepth && random.nextFloat() < branchChance && i < mainSegments - 1) { // Don't branch from the very last segment point
                var branchEndOffset = new Vec3(
                        (random.nextDouble() - 0.5) * totalDistance * 0.3, // Branch length relative to main bolt
                        (random.nextDouble() - 0.5) * totalDistance * 0.3,
                        (random.nextDouble() - 0.5) * totalDistance * 0.3
                );
                var branchEnd = currentTargetPoint.add(branchEndOffset);
                // Make branches shorter and less detailed
                createLightningBolt(level, currentTargetPoint, branchEnd,
                        Math.max(1, mainSegments / 2), jitterAmount * 0.7,
                        particlesPerMeter * 0.7, particleEffect,
                        branchChance * 0.5f, maxBranchDepth, currentBranchDepth + 1);
            }
            previousPoint = currentTargetPoint;
        }
    }

    private static void spawnParticlesAlongSegment(ServerLevel level, Vec3 p1, Vec3 p2, double particlesPerMeter, ParticleOptions particleEffect) {
        var segment = p2.subtract(p1);
        var length = segment.length();
        if (length < 0.01) return; // Avoid division by zero or tiny segments

        var unit = segment.normalize();
        var numParticles = Math.max(1, (int) (length * particlesPerMeter));

        for (int i = 0; i < numParticles; i++) {
            double progress = (double) i / (double) numParticles;
            var particlePos = p1.add(unit.scale(length * progress));
            level.sendParticles(particleEffect, particlePos.x, particlePos.y, particlePos.z,
                    1, 0, 0, 0, 0.0D); // count, dx, dy, dz, speed
        }
    }

    @Override
    public int getCapacity() {
        return OritechStartupConfig.electricMace.energyCapacity.get();
    }

    @Override
    public int getMaxInsert() {
        return getCapacity() / 10;
    }

    @Override
    public int getMaxExtract() {
        return getCapacity() / 10;
    }
}
