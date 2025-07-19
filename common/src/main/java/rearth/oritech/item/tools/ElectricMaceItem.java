package rearth.oritech.item.tools;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.init.SoundContent;
import rearth.oritech.item.tools.harvesting.ChainsawItem;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.util.TooltipHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElectricMaceItem extends MaceItem implements OritechEnergyItem {
    
    private static final int BASE_ATTACK_DAMAGE = Oritech.CONFIG.electricMace.baseDamage();
    private static final int RF_USAGE = Oritech.CONFIG.electricMace.energyUsage();
    
    public static final Map<Long, Runnable> PENDING_LIGHTNING_HITS = new HashMap<>();
    
    public ElectricMaceItem(Settings settings) {
        super(settings);
    }
    
    public static AttributeModifiersComponent createAttributeModifiers() {
        return AttributeModifiersComponent
                 .builder()
                 .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID, BASE_ATTACK_DAMAGE, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                 .add(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(BASE_ATTACK_SPEED_MODIFIER_ID, -3.4F, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                 .add(EntityAttributes.GENERIC_SAFE_FALL_DISTANCE, new EntityAttributeModifier(Oritech.id("mace_fall_protection"), 10, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                 .add(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, new EntityAttributeModifier(Oritech.id("mace_reach"), 3, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                 .build();
    }
    
    @Override
    public void postDamageEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        
        var bonus = 0f;
        
        var usedEnergy = tryUseEnergy(stack, RF_USAGE, null);
        if (usedEnergy && shouldDealAdditionalDamage(attacker)) {
            attacker.getWorld().playSound(null, target.getBlockPos(), SoundContent.ELECTRIC_SHOCK, SoundCategory.PLAYERS);
            attacker.onLanding();
            bonus = getBonusAttackDamage(target, BASE_ATTACK_DAMAGE, new DamageSource(attacker.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DamageTypes.LIGHTNING_BOLT), attacker));
        }
        
        
        if (attacker instanceof PlayerEntity player && attacker.getWorld() instanceof ServerWorld serverWorld) {
            if (player.getItemCooldownManager().isCoolingDown(this))
                return;
            
            player.getItemCooldownManager().set(this, 40);
            createLightningAttack(serverWorld, player, target, stack, (int) (BASE_ATTACK_DAMAGE / 2f + bonus / 2f));
        }
    }
    
    private void createLightningAttack(ServerWorld world, PlayerEntity attacker, LivingEntity target, ItemStack stack, int damage) {
        
        var usedEnergy = tryUseEnergy(stack, RF_USAGE * Oritech.CONFIG.electricMace.lightningCostMultiplier(), null);
        if (usedEnergy && attacker.getWorld() instanceof ServerWorld serverWorld) {
            
            var playerPos = attacker.getEyePos();
            var targetPos = target.getEyePos();
            var offset = targetPos.subtract(playerPos);
            var up = new Vec3d(0, 1, 0);
            var cross = offset.crossProduct(up).normalize();
            var pos = targetPos.add(cross.multiply(14)).addRandom(serverWorld.random, 3).add(0, 7, 0);
            
            createLightningBolt(serverWorld, pos, target.getEyePos().addRandom(serverWorld.random, 0.1f), 10, 0.8f, 4, ParticleTypes.ENCHANTED_HIT, 0.35f, 2, 0);
            
            for (int i = 1; i <= 5; i++) {
                final var ownPos = targetPos.add(cross.rotateY(i * 90).multiply(14)).addRandom(serverWorld.random, 5).add(0, 7, 0);
                PENDING_LIGHTNING_HITS.put(serverWorld.getTime() + 10 * i, () -> {
                    createLightningBolt(serverWorld, ownPos, target.getEyePos().addRandom(serverWorld.random, 0.1f), 10, 0.8f, 4, ParticleTypes.ENCHANTED_HIT, 0.35f, 2, 0);
                    target.hurtTime = 0;
                    target.damage(new DamageSource(serverWorld.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DamageTypes.LIGHTNING_BOLT), attacker), damage);
                });
            }
        }
    }
    
    public static void processLightningEvents(World world) {
        var toRemove = new ArrayList<Long>();
        for (var entry : PENDING_LIGHTNING_HITS.entrySet()) {
            var key = entry.getKey();
            if (world.getTime() > key) {
                var event = entry.getValue();
                event.run();
                toRemove.add(key);
            }
        }
        
        toRemove.forEach(PENDING_LIGHTNING_HITS::remove);
    }
    
    @Override
    public float getBonusAttackDamage(Entity target, float baseAttackDamage, DamageSource damageSource) {
        
        var attacker = damageSource.getSource();
        if (attacker instanceof LivingEntity livingEntity) {
            if (!shouldDealAdditionalDamage(livingEntity)) {
                return 0.0F;
            } else {
                float fallDist = livingEntity.fallDistance;
                float damage;
                if (fallDist <= 3.0F) {
                    damage = BASE_ATTACK_DAMAGE * fallDist;
                } else if (fallDist <= 8.0F) {
                    damage = BASE_ATTACK_DAMAGE * 4 + 4.0F * (fallDist - 3.0F);
                } else {
                    damage = BASE_ATTACK_DAMAGE * 6 + fallDist - 8.0F;
                }
                
                var world = livingEntity.getWorld();
                if (world instanceof ServerWorld serverWorld) {
                    return damage + EnchantmentHelper.getSmashDamagePerFallenBlock(serverWorld, livingEntity.getWeaponStack(), target, damageSource, 2.0F) * fallDist;
                } else {
                    return damage;
                }
            }
        } else {
            return 0.0F;
        }
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        var text = Text.translatable("tooltip.oritech.energy_indicator", TooltipHelper.getEnergyText(this.getStoredEnergy(stack)), TooltipHelper.getEnergyText(this.getEnergyCapacity(stack)));
        tooltip.add(text.formatted(Formatting.GOLD));
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            tooltip.add(Text.translatable("tooltip.oritech.electric_mace").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
            tooltip.add(Text.translatable("tooltip.oritech.electric_mace.1").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
        } else {
            tooltip.add(Text.translatable("tooltip.oritech.item_extra_info").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
        }
    }
    
    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round((getStoredEnergy(stack) * 100f / this.getEnergyCapacity(stack)) * ChainsawItem.BAR_STEP_COUNT) / 100;
    }
    
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0xff7007;
    }
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return Oritech.CONFIG.electricMace.energyCapacity();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return getEnergyCapacity(stack) / 10;
    }
    
    // this overrides the fabric specific extensions
    public boolean allowComponentsUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }
    
    public boolean allowContinuingBlockBreaking(PlayerEntity player, ItemStack oldStack, ItemStack newStack) {
        return true;
    }
    
    // this overrides the neoforge specific extensions
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return false;
    }
    
    public boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        return false;
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
    public static void createLightningBolt(ServerWorld level, Vec3d startPos, Vec3d endPos,
                                           int mainSegments, double jitterAmount,
                                           double particlesPerMeter, ParticleEffect particleEffect,
                                           float branchChance, int maxBranchDepth, int currentBranchDepth) {
        var random = level.getRandom();
        var direction = endPos.subtract(startPos);
        double totalDistance = direction.length();
        
        if (totalDistance < 0.1) { // Too short to draw
            // Optional: just spawn a particle at the point or do nothing
            level.spawnParticles(particleEffect, startPos.x, startPos.y, startPos.z, 5, 0.1, 0.1, 0.1, 0.05);
            return;
        }
        
        var segmentVector = direction.normalize().multiply(totalDistance / mainSegments);
        var previousPoint = startPos;
        
        // Sound for the main bolt (only if not a branch or first branch)
        if (currentBranchDepth == 0) {
            level.playSound(null, endPos.x, endPos.y, endPos.z, SoundContent.ELECTRIC_SHOCK, SoundCategory.PLAYERS, 0.8f, 0.5f + level.random.nextFloat() * 0.8f);
        }
        
        
        for (int i = 0; i < mainSegments; i++) {
            Vec3d currentTargetPoint;
            if (i < mainSegments - 1) {
                currentTargetPoint = startPos.add(segmentVector.multiply(i + 1));
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
                var branchEndOffset = new Vec3d(
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
    
    private static void spawnParticlesAlongSegment(ServerWorld level, Vec3d p1, Vec3d p2, double particlesPerMeter, ParticleEffect particleEffect) {
        var segment = p2.subtract(p1);
        var length = segment.length();
        if (length < 0.01) return; // Avoid division by zero or tiny segments
        
        var unit = segment.normalize();
        var numParticles = Math.max(1, (int) (length * particlesPerMeter));
        
        for (int i = 0; i < numParticles; i++) {
            double progress = (double) i / (double) numParticles;
            var particlePos = p1.add(unit.multiply(length * progress));
            level.spawnParticles(particleEffect, particlePos.x, particlePos.y, particlePos.z,
              1, 0, 0, 0, 0.0D); // count, dx, dy, dz, speed
        }
    }
}
