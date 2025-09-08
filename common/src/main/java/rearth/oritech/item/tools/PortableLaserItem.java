package rearth.oritech.item.tools;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.Protection;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.renderers.PortableLaserRenderer;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.util.AutoPlayingSoundKeyframeHandler;
import rearth.oritech.util.TooltipHelper;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static rearth.oritech.block.entity.interaction.LaserArmBlockEntity.BLOCK_BREAK_ENERGY;
import static rearth.oritech.item.tools.harvesting.DrillItem.BAR_STEP_COUNT;


public class PortableLaserItem extends Item implements OritechEnergyItem, GeoItem {
    
    public static final int ACTION_COOLDOWN = 24;
    
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SHOOTING = RawAnimation.begin().thenPlay("shooting");
    private static final RawAnimation SINGLE_SHOT = RawAnimation.begin().thenPlay("singleshot");
    
    // client only
    public static long lastSingleShot = 0;
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private static final Map<Player, Tuple<BlockPos, Integer>> blockBreakStats = new HashMap<>();
    
    public PortableLaserItem(Properties settings) {
        super(settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        
        var stack = player.getItemInHand(hand);
        var energyUsed = Oritech.CONFIG.portableLaserConfig.energyPerBoom();
        
        if (world.isClientSide) {
            if (getStoredEnergy(stack) > energyUsed && !player.isShiftKeyDown() && !isMiningEnabled(stack))
                lastSingleShot = world.getGameTime();
            
            return InteractionResultHolder.consume(stack);
        }
        
        if (!(stack.getItem() instanceof PortableLaserItem laserItem)) return InteractionResultHolder.consume(stack);
        
        if (player.isShiftKeyDown()) {
            
            var lastMode = isMiningEnabled(stack);
            setMiningEnabled(stack, !lastMode);
            
            player.sendSystemMessage(Component.translatable("tooltip.oritech.portable_laser.status.begin").append(Component.literal(String.valueOf(!lastMode))));
            
            return InteractionResultHolder.consume(stack);
        }
        
        if (isMiningEnabled(stack)) {
            player.sendSystemMessage(Component.translatable("tooltip.oritech.portable_laser.status.shot_mining_error"));
            return InteractionResultHolder.pass(stack);
        }
        
        if (!laserItem.tryUseEnergy(stack, energyUsed, player)) {
            return InteractionResultHolder.pass(stack);
        }
        
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.getCooldowns().addCooldown(this, ACTION_COOLDOWN);
        
        Vec3 endPos;
        
        var hit = getPlayerTargetRay(player);
        
        if (hit != null) {
            var canInteract = true;
            if (Platform.isModLoaded("ftbchunks")) {
                canInteract = !FTBChunksAPI.api().getManager().shouldPreventInteraction(player, hand, BlockPos.containing(hit.getLocation()), Protection.EDIT_AND_INTERACT_BLOCK, null);
            }
            
            if (canInteract)
                world.explode(null, new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT), player),
                  null, hit.getLocation(), Oritech.CONFIG.portableLaserConfig.explosionStrength(), false, Level.ExplosionInteraction.MOB);
            
            endPos = hit.getLocation();
        } else {
            var startPos = player.getEyePosition();
            var lookVec = player.getViewVector(0F);
            endPos = startPos.add(lookVec.scale(128));
        }
        
        if (hit instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity livingEntity) {
            var canInteract = true;
            if (Platform.isModLoaded("ftbchunks")) {
                canInteract = !FTBChunksAPI.api().getManager().shouldPreventInteraction(player, hand, BlockPos.containing(hit.getLocation()), Protection.INTERACT_ENTITY, livingEntity);
            }
            
            if (canInteract)
                processEntityTarget(player, livingEntity, 20, stack, world);
        }
        
        triggerAnim(player, GeoItem.getId(stack), "laser", "singleshot");
        
        world.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1f);
        
        // Calculate the "right" direction based on the player's yaw
        float yawRadians = (player.getYRot() + 90) * (float) Math.PI / 180;
        double rightX = -Mth.sin(yawRadians);
        double rightZ = Mth.cos(yawRadians);
        Vec3 rightDir = new Vec3(rightX, 0, rightZ).normalize();
        
        var startPos = player.getEyePosition().add(endPos.subtract(player.getEyePosition()).scale(0.4f)).add(0, -0.5f, 0).add(rightDir.scale(0.3f));
        ParticleContent.LASER_BOOM.spawn(world, startPos, endPos);
        ParticleContent.MELTDOWN_IMMINENT.spawn(world, endPos, 6);
        
        return InteractionResultHolder.consume(stack);
    }
    
    public static void onUseTick(Player player) {
        var world = player.level();
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        
        if (!(stack.getItem() instanceof PortableLaserItem laserItem) || world == null) return;
        
        var rfUsage = Oritech.CONFIG.portableLaserConfig.energyPerTick();
        
        if (!laserItem.tryUseEnergy(stack, rfUsage, player)) {
            return;
        }
        
        var finalHit = getPlayerTargetRay(player);
        
        laserItem.triggerAnim(player, GeoItem.getId(stack), "laser", "shooting");
        
        if (finalHit instanceof BlockHitResult blockHitResult && laserItem.isMiningEnabled(stack)) {
            var blockPos = blockHitResult.getBlockPos();
            var blockState = world.getBlockState(blockPos);
            if (blockState.isAir() || blockState.is(TagContent.LASER_PASSTHROUGH)) return;
            
            var canInteract = true;
            if (Platform.isModLoaded("ftbchunks")) {
                canInteract = !FTBChunksAPI.api().getManager().shouldPreventInteraction(player, InteractionHand.MAIN_HAND, BlockPos.containing(finalHit.getLocation()), Protection.EDIT_AND_INTERACT_BLOCK, null);
            }
            
            if (canInteract)
                processBlockBreaking(blockPos, blockState, world, player, stack, rfUsage);
        } else if (finalHit instanceof EntityHitResult entityHitResult) {
            var target = entityHitResult.getEntity();
            if (!(target instanceof LivingEntity livingEntity)) return;
            var canInteract = true;
            if (Platform.isModLoaded("ftbchunks")) {
                canInteract = !FTBChunksAPI.api().getManager().shouldPreventInteraction(player, InteractionHand.MAIN_HAND, BlockPos.containing(finalHit.getLocation()), Protection.EDIT_AND_INTERACT_BLOCK, target);
            }
            
            if (canInteract)
                processEntityTarget(player, livingEntity, Oritech.CONFIG.portableLaserConfig.damageBase(), stack, world);
        }
        
        if (finalHit != null && finalHit.getType() != HitResult.Type.MISS && laserItem.isMiningEnabled(stack)) {
            ParticleContent.LASER_BEAM_EFFECT.spawn(world, finalHit.getLocation());
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
    
    private static void processBlockBreaking(BlockPos blockPos, BlockState blockState, Level world, Player player, ItemStack tool, int energyUsed) {
        
        // skip unbreakable blocks
        if (blockState.getDestroySpeed(world, blockPos) < 0) return;
        
        var stats = blockBreakStats.getOrDefault(player, new Tuple<>(BlockPos.ZERO, 0));
        if (!blockPos.equals(stats.getA())) {
            stats = new Tuple<>(blockPos, energyUsed);
        } else {
            stats = new Tuple<>(blockPos, stats.getB() + energyUsed);
        }
        
        if (blockState.is(TagContent.LASER_ACCELERATED)) {
            blockState.randomTick((ServerLevel) world, blockPos, world.random);
            ParticleContent.ACCELERATING.spawn(world, Vec3.atLowerCornerOf(blockPos));
            stats = new Tuple<>(blockPos, -1);
        }
        
        var blockEntity = world.getBlockEntity(blockPos);
        if (blockEntity instanceof MachineCoreEntity coreBlock && coreBlock.isEnabled()) {
            blockEntity = (BlockEntity) coreBlock.getCachedController();
        }
        if (blockEntity != null) {
            var storageCandidate = EnergyApi.BLOCK.find(world, blockPos, blockState, null, null);
            if (storageCandidate == null && blockEntity instanceof EnergyApi.BlockProvider provider) {
                storageCandidate = provider.getEnergyStorage(null);
            }
            
            if (storageCandidate instanceof DynamicEnergyStorage dynamicStorage) {
                var inserted = dynamicStorage.insertIgnoringLimit(energyUsed, false);
                if (inserted > 0)
                    dynamicStorage.update();
                
                return;
            } else if (storageCandidate != null) {
                var inserted = storageCandidate.insert(energyUsed, false);
                if (inserted > 0)
                    storageCandidate.update();
                
                return;
            }
        }
        
        var currentInvestedEnergy = stats.getB();
        var requiredBreakingEnergy = (int) (Math.sqrt(blockState.getDestroySpeed(world, blockPos)) * BLOCK_BREAK_ENERGY / Oritech.CONFIG.portableLaserConfig.blockBreakSpeed());
        var efficiencyLevel = getEnchantmentLevel(tool, Enchantments.EFFICIENCY);
        if (efficiencyLevel > 0) requiredBreakingEnergy = requiredBreakingEnergy / (efficiencyLevel + 1);
        
        var currentProgress = currentInvestedEnergy / (float) requiredBreakingEnergy;
        if (world instanceof ServerLevel serverLevel)
            serverLevel.destroyBlockProgress(0, blockPos, (int) (currentProgress * 10));
        
        if (currentInvestedEnergy > requiredBreakingEnergy) {
            stats = new Tuple<>(blockPos, 0);
            finishBlockBreaking(blockPos, blockState, world, player, tool);
        }
        
        blockBreakStats.put(player, stats);
    }
    
    private static void finishBlockBreaking(BlockPos targetPos, BlockState targetBlockState, Level world, Player player, ItemStack tool) {
        
        var targetEntity = world.getBlockEntity(targetPos);
        List<ItemStack> dropped;
        dropped = Block.getDrops(targetBlockState, (ServerLevel) world, targetPos, targetEntity, player, tool);
        
        var blockRecipe = LaserArmBlockEntity.tryGetRecipeOfBlock(targetBlockState, world);
        if (blockRecipe != null) {
            var recipe = blockRecipe.value();
            var farmedCount = 1;
            dropped = List.of(new ItemStack(recipe.getResults().get(0).getItem(), farmedCount));
            ParticleContent.CHARGING.spawn(world, Vec3.atLowerCornerOf(targetPos), 1);
        }
        
        // add stack to player inv, or spawn at block pos
        for (var stack : dropped) {
            if (!player.getInventory().add(stack))
                world.addFreshEntity(new ItemEntity(world, targetPos.getCenter().x, targetPos.getCenter().y, targetPos.getCenter().z, stack));
        }
        
        try {
            targetBlockState.getBlock().playerWillDestroy(world, targetPos, targetBlockState, player);
        } catch (Exception exception) {
            Oritech.LOGGER.warn("Laser arm block break event failure when breaking " + targetBlockState + " at " + targetPos + ": " + exception.getLocalizedMessage());
        }
        world.addDestroyBlockEffect(targetPos, world.getBlockState(targetPos));
        world.playSound(null, targetPos, targetBlockState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
        world.destroyBlock(targetPos, false);
    }
    
    private static void processEntityTarget(Player player, LivingEntity target, int damage, ItemStack tool, Level world) {
        
        // make creepers charged
        if (target.getType().equals(EntityType.CREEPER) && !target.getEntityData().get(Creeper.DATA_IS_POWERED)) {
            target.getEntityData().set(Creeper.DATA_IS_POWERED, true);
            return;
        }
        
        var sharpnessLevel = getEnchantmentLevel(tool, Enchantments.SHARPNESS);
        damage = (int) (damage * Math.sqrt(sharpnessLevel + 1));
        
        target.hurt(
          new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT), player),
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
    
    // this overrides the fabric specific extensions
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }
    
    public boolean allowContinuingBlockBreaking(Player player, ItemStack oldStack, ItemStack newStack) {
        return true;
    }
    
    // this overrides the neoforge specific extensions
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return false;
    }
    
    public boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        return false;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        var storedEnergy = TooltipHelper.getEnergyText(this.getStoredEnergy(stack));
        var capacity = TooltipHelper.getEnergyText(this.getEnergyCapacity(stack));
        var text = Component.translatable("tooltip.oritech.energy_indicator", storedEnergy, capacity);
        tooltip.add(text.withStyle(ChatFormatting.GOLD));
        
        var miningText = Component.translatable("tooltip.oritech.portable_laser.status.begin").withStyle(ChatFormatting.GRAY)
                           .append(Component.literal(String.valueOf(isMiningEnabled(stack))).withStyle(ChatFormatting.GOLD))
                           .append(Component.translatable("tooltip.oritech.portable_laser.status.hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(miningText);
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            for (int i = 1; i <= 5; i++) {
                tooltip.add(Component.translatable("tooltip.oritech.portable_laser." + i).withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
    }
    
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getEnchantmentValue() {
        return 10;
    }
    
    public boolean isMiningEnabled(ItemStack stack) {
        return stack.getOrDefault(ComponentContent.IS_AOE_ACTIVE.get(), false);
    }
    
    public void setMiningEnabled(ItemStack stack, boolean status) {
        stack.set(ComponentContent.IS_AOE_ACTIVE.get(), status);
    }
    
    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((getStoredEnergy(stack) * 100f / this.getEnergyCapacity(stack)) * BAR_STEP_COUNT) / 100;
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
    public long getEnergyCapacity(ItemStack stack) {
        return Oritech.CONFIG.portableLaserConfig.energyCapacity();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return Oritech.CONFIG.portableLaserConfig.energyCapacity() / 80;
    }
    
    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return Oritech.CONFIG.portableLaserConfig.energyCapacity() / 80;
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
          this,
          "laser",
          0,
          state -> {
              if (state.getController().getAnimationState().equals(AnimationController.State.STOPPED))
                  return state.setAndContinue(IDLE);
              return PlayState.CONTINUE;
          })
                          .triggerableAnim("idle", IDLE)
                          .triggerableAnim("singleshot", SINGLE_SHOT)
                          .triggerableAnim("shooting", SHOOTING).setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));
    }
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private PortableLaserRenderer renderer;
            
            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
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
    
    public static void receiveUsePacket(LaserPlayerUsePacket packet, Player player, RegistryAccess dynamicRegistryManager) {
        PortableLaserItem.onUseTick(player);
    }
    
    public record LaserPlayerUsePacket() implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<LaserPlayerUsePacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("laser_use"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
