package rearth.oritech.item.tools;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import rearth.oritech.Oritech;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.interaction.EndericLaserBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.renderers.PortableLaserRenderer;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.util.MachineSoundHandler;
import rearth.oritech.util.PermissionHelpers;
import rearth.oritech.util.TooltipHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static rearth.oritech.item.tools.harvesting.DrillItem.BAR_STEP_COUNT;


public class EndericRailgunItem extends Item implements OritechEnergyItem, GeoItem {

    public static final int ACTION_COOLDOWN = 24;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SHOOTING = RawAnimation.begin().thenPlay("shooting");
    private static final RawAnimation SINGLE_SHOT = RawAnimation.begin().thenPlay("singleshot");

    // client only
    public static long lastSingleShot = 0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final Map<Player, Tuple<BlockPos, Integer>> blockBreakStats = new HashMap<>();

    public EndericRailgunItem(Properties settings) {
        super(settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        var stack = player.getItemInHand(hand);
        var energyUsed = OritechStartupConfig.portableLaserConfig.energyPerBoom.get();

        if (level.isClientSide()) {
            if (getStoredEnergy(stack, ItemAccess.forStack(stack)) > energyUsed && !player.isShiftKeyDown() && !isMiningEnabled(stack))
                lastSingleShot = level.getGameTime();

            return InteractionResult.CONSUME;
        }

        if (!(stack.getItem() instanceof EndericRailgunItem laserItem)) return InteractionResult.CONSUME;

        if (player.isShiftKeyDown()) {

            var lastMode = isMiningEnabled(stack);
            setMiningEnabled(stack, !lastMode);

            player.sendSystemMessage(Component.translatable("tooltip.oritech.portable_laser.status.begin").append(Component.literal(String.valueOf(!lastMode))));

            return InteractionResult.CONSUME;
        }

        if (isMiningEnabled(stack)) {
            player.sendSystemMessage(Component.translatable("tooltip.oritech.portable_laser.status.shot_mining_error"));
            return InteractionResult.PASS;
        }

        if (!laserItem.tryUseEnergy(stack, energyUsed, player)) {
            return InteractionResult.PASS;
        }

        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        player.getCooldowns().addCooldown(stack, ACTION_COOLDOWN);

        Vec3 endPos;

        var hit = getPlayerTargetRay(player);

        if (hit != null) {
            var targetBlockPos = BlockPos.containing(hit.getLocation());
            var canInteract = PermissionHelpers.CanPlayerBreakBlock(level, targetBlockPos, level.getBlockState(targetBlockPos), (ServerPlayer) player);

            if (canInteract)
                level.explode(null, level.damageSources().lightningBolt(),
                        null, hit.getLocation(), OritechStartupConfig.portableLaserConfig.explosionStrength.get(), false, Level.ExplosionInteraction.MOB);

            endPos = hit.getLocation();
        } else {
            var startPos = player.getEyePosition();
            var lookVec = player.getViewVector(0F);
            endPos = startPos.add(lookVec.scale(128));
        }

        if (hit instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity livingEntity) {

            var source = level.damageSources().source(DamageTypes.LIGHTNING_BOLT, player);
            var canInteract = PermissionHelpers.CanPlayerAttackEntity(livingEntity, level, 20, source);

            if (canInteract)
                processEntityTarget(player, livingEntity, 20, stack, level);
        }

        triggerAnim(player, GeoItem.getId(stack), "laser", "singleshot");

        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1f);

        // Calculate the "right" direction based on the player's yaw
        float yawRadians = (player.getYRot() + 90) * (float) Math.PI / 180;
        double rightX = -Mth.sin(yawRadians);
        double rightZ = Mth.cos(yawRadians);
        Vec3 rightDir = new Vec3(rightX, 0, rightZ).normalize();

        var startPos = player.getEyePosition().add(endPos.subtract(player.getEyePosition()).scale(0.4f)).add(0, -0.5f, 0).add(rightDir.scale(0.3f));
        ParticleContent.LaserBoom(level, startPos, endPos);
        if (level instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.LAVA, endPos.x, endPos.y, endPos.z, 6, 1, 1, 1, 0);

        return InteractionResult.CONSUME;
    }

    public static void onUseTick(Player player) {
        var level = player.level();
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!(stack.getItem() instanceof EndericRailgunItem laserItem) || !player.mayBuild()) return;

        var rfUsage = OritechStartupConfig.portableLaserConfig.energyPerTick.get();

        if (!laserItem.tryUseEnergy(stack, rfUsage, player)) {
            return;
        }

        var finalHit = getPlayerTargetRay(player);

        laserItem.triggerAnim(player, GeoItem.getId(stack), "laser", "shooting");

        if (finalHit instanceof BlockHitResult blockHitResult && laserItem.isMiningEnabled(stack)) {
            var blockPos = blockHitResult.getBlockPos();
            var blockState = level.getBlockState(blockPos);
            if (blockState.isAir() || blockState.is(TagContent.LASER_PASSTHROUGH)) return;

            var canInteract = player instanceof ServerPlayer serverPlayer && PermissionHelpers.CanPlayerBreakBlock(level, blockPos, blockState, serverPlayer);

            if (canInteract)
                processBlockBreaking(blockPos, blockState, level, player, stack, rfUsage);
        } else if (finalHit instanceof EntityHitResult entityHitResult) {

            var target = entityHitResult.getEntity();
            if (!(target instanceof LivingEntity livingEntity)) return;

            var source = level.damageSources().source(DamageTypes.LIGHTNING_BOLT, player);
            var canInteract = PermissionHelpers.CanPlayerAttackEntity(livingEntity, level, 20f, source);

            if (canInteract)
                processEntityTarget(player, livingEntity, OritechStartupConfig.portableLaserConfig.damageBase.get(), stack, level);
        }

        if (finalHit != null && finalHit.getType() != HitResult.Type.MISS && laserItem.isMiningEnabled(stack)) {
            if (level instanceof ServerLevel sl) {
                var loc = finalHit.getLocation();
                sl.sendParticles(ParticleTypes.SMALL_FLAME, loc.x, loc.y, loc.z, 1, 0.4, 0.3, 0.4, 0);
            }
        }

    }

    public static @Nullable HitResult getPlayerTargetRay(Player player) {

        // block raycast
        var blockHit = player.pick(128, 0, true);

        // entity raycast
        // possible idea for future optimization: do a custom raycast here with slightly inflated bounding boxes to make aiming easier
        var startPos = player.getEyePosition();
        var lookVec = player.getViewVector(0F);
        var endPos = startPos.add(lookVec.scale(128));
        var entityHit = ProjectileUtil.getEntityHitResult(
                player,
                startPos,
                endPos,
                new AABB(startPos, endPos),
                entity -> !entity.isSpectator() && entity.isAttackable() && entity.isAlive() && entity != player,
                128 * 128
        );

        // Determine the closest hit
        HitResult finalHit = null;
        var blockDistance = blockHit.getType() == HitResult.Type.BLOCK ? startPos.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;
        var entityDistance = entityHit != null ? startPos.distanceToSqr(entityHit.getLocation()) : Double.MAX_VALUE;

        if (blockDistance < entityDistance) {
            finalHit = blockHit;
        } else if (entityHit != null) {
            finalHit = entityHit;
        }
        return finalHit;
    }

    private static void processBlockBreaking(BlockPos blockPos, BlockState blockState, Level level, Player player, ItemStack tool, int energyUsed) {

        // skip unbreakable blocks
        if (blockState.getDestroySpeed(level, blockPos) < 0) return;

        var stats = blockBreakStats.getOrDefault(player, new Tuple<>(BlockPos.ZERO, 0));
        if (!blockPos.equals(stats.getA())) {
            stats = new Tuple<>(blockPos, energyUsed);
        } else {
            stats = new Tuple<>(blockPos, stats.getB() + energyUsed);
        }

        if (blockState.is(TagContent.LASER_ACCELERATED)) {
            blockState.randomTick((ServerLevel) level, blockPos, level.getRandom());
            ParticleContent.Accelerating(level, Vec3.atLowerCornerOf(blockPos));
            stats = new Tuple<>(blockPos, -1);
        }

        var blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof MachineCoreEntity coreBlock && coreBlock.isEnabled()) {
            blockEntity = (BlockEntity) coreBlock.getCachedController();
        }
        if (blockEntity != null) {
            var storageCandidate = level.getCapability(Capabilities.Energy.BLOCK, blockPos, blockState, blockEntity, null);
            if (storageCandidate == null && blockEntity instanceof EnergyProvider energyProvider) {
                storageCandidate = energyProvider.getEnergyLookup(null);
            }

            if (storageCandidate instanceof DynamicEnergyStorage dynamicStorage) {
                try (var transaction = Transaction.openRoot()) {
                    var inserted = dynamicStorage.internalInsert(energyUsed, transaction);
                    if (inserted > 0) {
                        transaction.commit();
                    }
                }
                return;
            } else if (storageCandidate != null) {
                try (var transaction = Transaction.openRoot()) {
                    var inserted = storageCandidate.insert(energyUsed, transaction);
                    if (inserted > 0) {
                        transaction.commit();
                    }
                }
                return;
            }
        }

        var currentInvestedEnergy = stats.getB();
        var requiredBreakingEnergy = (int) (Math.sqrt(blockState.getDestroySpeed(level, blockPos)) * OritechConfig.endericLaserConfig.blockBreakEnergyBase.get() / OritechStartupConfig.portableLaserConfig.blockBreakSpeed.get());
        var efficiencyLevel = getEnchantmentLevel(tool, Enchantments.EFFICIENCY);
        if (efficiencyLevel > 0) requiredBreakingEnergy = requiredBreakingEnergy / (efficiencyLevel + 1);

        var currentProgress = currentInvestedEnergy / (float) requiredBreakingEnergy;
        if (level instanceof ServerLevel serverLevel)
            serverLevel.destroyBlockProgress(0, blockPos, (int) (currentProgress * 10));

        if (currentInvestedEnergy > requiredBreakingEnergy) {
            stats = new Tuple<>(blockPos, 0);
            finishBlockBreaking(blockPos, blockState, level, player, tool);
        }

        blockBreakStats.put(player, stats);
    }

    private static void finishBlockBreaking(BlockPos targetPos, BlockState targetBlockState, Level level, Player player, ItemStack tool) {

        var targetEntity = level.getBlockEntity(targetPos);
        List<ItemStack> dropped;
        dropped = Block.getDrops(targetBlockState, (ServerLevel) level, targetPos, targetEntity, player, tool);

        var blockRecipe = EndericLaserBlockEntity.tryGetRecipeOfBlock(targetBlockState, (ServerLevel) level);
        if (blockRecipe != null) {
            var recipe = blockRecipe.value();
            var farmedCount = 1;
            dropped = List.of(new ItemStack(recipe.itemResults().get(0).item(), farmedCount));
            if (level instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.SONIC_BOOM, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 1, 0.6, 0.6, 0.6, 0);
        }

        // add stack to player inv, or spawn at block pos
        for (var stack : dropped) {
            if (!player.getInventory().add(stack))
                level.addFreshEntity(new ItemEntity(level, targetPos.getCenter().x, targetPos.getCenter().y, targetPos.getCenter().z, stack));
        }

        try {
            targetBlockState.getBlock().playerWillDestroy(level, targetPos, targetBlockState, player);
        } catch (Exception exception) {
            Oritech.LOGGER.warn("Laser arm block break event failure when breaking " + targetBlockState + " at " + targetPos + ": " + exception.getLocalizedMessage());
        }
        level.addDestroyBlockEffect(targetPos, level.getBlockState(targetPos));
        level.playSound(null, targetPos, targetBlockState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
        level.destroyBlock(targetPos, false);
    }

    private static void processEntityTarget(Player player, LivingEntity target, int damage, ItemStack tool, Level level) {

        // make creepers charged
        if (target.getType().equals(EntityType.CREEPER) && !target.getEntityData().get(Creeper.DATA_IS_POWERED)) {
            target.getEntityData().set(Creeper.DATA_IS_POWERED, true);
            return;
        }

        var sharpnessLevel = getEnchantmentLevel(tool, Enchantments.SHARPNESS);
        damage = (int) (damage * Math.sqrt(sharpnessLevel + 1));

        target.hurt(
                level.damageSources().source(DamageTypes.LIGHTNING_BOLT, player),
                damage);

    }

    // A hack to do this without context of the DRM
    public static int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        var enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchantments.keySet()) {
            if (entry.unwrapKey().isPresent() && entry.unwrapKey().get().equals(enchantment)) {
                return enchantments.getLevel(entry);
            }
        }
        return 0;
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag type) {
        super.appendHoverText(stack, context, display, builder, type);
        var storedEnergy = TooltipHelper.getEnergyText(this.getStoredEnergy(stack, ItemAccess.forStack(stack)));
        var capacity = TooltipHelper.getEnergyText(this.getEnergyCapacity());
        var text = Component.translatable("tooltip.oritech.energy_indicator", storedEnergy, capacity);
        builder.accept(text.withStyle(ChatFormatting.GOLD));

        var miningText = Component.translatable("tooltip.oritech.portable_laser.status.begin").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(isMiningEnabled(stack))).withStyle(ChatFormatting.GOLD))
                .append(Component.translatable("tooltip.oritech.portable_laser.status.hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        builder.accept(miningText);

        var showExtra = Minecraft.getInstance().hasControlDown();

        if (showExtra) {
            for (int i = 1; i <= 5; i++) {
                builder.accept(Component.translatable("tooltip.oritech.portable_laser." + i).withStyle(ChatFormatting.GRAY));
            }
        } else {
            builder.accept(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
    }

    public boolean isMiningEnabled(ItemStack stack) {
        return stack.getOrDefault(ComponentContent.IS_AOE_ACTIVE.get(), false);
    }

    public void setMiningEnabled(ItemStack stack, boolean status) {
        stack.set(ComponentContent.IS_AOE_ACTIVE.get(), status);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((getStoredEnergy(stack, ItemAccess.forStack(stack)) * 100f / this.getEnergyCapacity()) * BAR_STEP_COUNT) / 100;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xff7007;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                "laser",
                0,
                state -> {
                    if (state.controller().getPlayState().equals(PlayState.STOP))
                        return state.setAndContinue(IDLE);
                    return PlayState.CONTINUE;
                })
                .triggerableAnim("idle", IDLE)
                .triggerableAnim("singleshot", SINGLE_SHOT)
                .triggerableAnim("shooting", SHOOTING).setSoundKeyframeHandler(new MachineSoundHandler<>()));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private PortableLaserRenderer renderer;

            @Override
            public @NonNull GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new PortableLaserRenderer("portable_laser");
                return renderer;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static void receiveUsePacket(LaserPlayerUsePacket packet, IPayloadContext context) {
        EndericRailgunItem.onUseTick(context.player());
    }

    @Override
    public int getEnergyCapacity() {
        return OritechStartupConfig.portableLaserConfig.energyCapacity.get();
    }

    @Override
    public int getMaxRFInputRate() {
        return OritechStartupConfig.portableLaserConfig.energyCapacity.get() / 20;
    }

    @Override
    public int getMaxRFOutputRate() {
        return OritechStartupConfig.portableLaserConfig.energyCapacity.get() / 20;
    }

    public record LaserPlayerUsePacket() implements CustomPacketPayload {

        public static final Type<LaserPlayerUsePacket> PACKET_ID = new Type<>(Oritech.id("laser_use"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
