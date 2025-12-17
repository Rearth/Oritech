package rearth.oritech.block.entity.arcane;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.ComparatorOutputProvider;

import java.util.UUID;

public class SpawnerControllerBlockEntity extends BaseSoulCollectionEntity implements BlockEntityTicker<SpawnerControllerBlockEntity>, ComparatorOutputProvider {
    
    public int maxSouls = 100_000;
    public int collectedSouls = 0;
    
    public CompoundTag mobNbt = new CompoundTag();
    public Entity renderedEntity;
    private boolean networkDirty;
    public boolean hasCage;
    private int lastComparatorOutput;
    private boolean redstonePowered;
    
    
    // client only
    public float lastProgress = 0f;
    
    public SpawnerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.SPAWNER_CONTROLLER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, SpawnerControllerBlockEntity blockEntity) {
        
        if (world.isClientSide) return;
        
        if (mobNbt.isEmpty() || !hasCage || redstonePowered) return;
        
        if (collectedSouls >= maxSouls && world.getGameTime() % 4 == 0) {
            spawnMob();
            updateComparator();
        }
        
        if (networkDirty) {
            updateNetwork();
            DeathListener.resetEvents();
        }
        
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        nbt.putInt("souls", collectedSouls);
        nbt.putInt("maxSouls", maxSouls);
        nbt.putBoolean("cage", hasCage);
        nbt.putBoolean("redstone", redstonePowered);
        if (mobNbt != null) {
            nbt.put("mobNbt", mobNbt);
        }
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        hasCage = nbt.getBoolean("cage");
        maxSouls = nbt.getInt("maxSouls");
        collectedSouls = nbt.getInt("souls");
        redstonePowered = nbt.getBoolean("redstone");
        mobNbt = nbt.getCompound("mobNbt");
    }
    
    private void spawnMob() {
        // try and find a valid position within 10 attempts
        
        var spawned = EntityType.loadEntityRecursive(mobNbt, level, (entity) -> {
            var spawnRange = 4;
            var requiredHeight = Math.round(entity.getBbHeight() + 0.5f);
            var targetPosition = findSpawnPosition(spawnRange, requiredHeight);
            
            if (targetPosition == null) return null;
            entity.moveTo(Vec3.atLowerCornerOf(targetPosition));
            entity.setUUID(UUID.randomUUID());
            ParticleContent.SOUL_USED.spawn(level, targetPosition.getCenter(), maxSouls);
            
            return entity;
        });
        
        if (spawned == null) return;
        
        networkDirty = true;
        level.addFreshEntity(spawned);
        collectedSouls -= maxSouls;
        
    }
    
    private BlockPos findSpawnPosition(int spawnRange, int requiredHeight) {
        for (int i = 0; i < 10; i++) {
            var candidate = worldPosition.offset(level.random.nextIntBetweenInclusive(-spawnRange, spawnRange), 3, level.random.nextIntBetweenInclusive(-spawnRange, spawnRange));
            var foundFree = 0;
            for (int j = 0; j < 9; j++) {
                var state = level.getBlockState(candidate.below(j));
                if (state.isAir()) {
                    foundFree++;
                } else {
                    if (foundFree > requiredHeight) {
                        // found target
                        return candidate.below(j - 1);
                        
                    } else {
                        foundFree = 0;
                    }
                }
            }
        }
        
        return null;
    }
    
    private void updateNetwork() {
        networkDirty = false;
        
        if (!mobNbt.isEmpty())
            NetworkManager.sendBlockHandle(this, new SpawnerSyncPacket(worldPosition, mobNbt, hasCage, collectedSouls, maxSouls));
    }
    
    public static void receiveUpdatePacket(SpawnerSyncPacket message, Level world, RegistryAccess dynamicRegistryManager) {
        
        if (world.getBlockEntity(message.position) instanceof SpawnerControllerBlockEntity spawnerEntity) {
            spawnerEntity.mobNbt = message.spawnedMob;
            spawnerEntity.hasCage = message.hasCage;
            spawnerEntity.collectedSouls = message.collectedSouls;
            spawnerEntity.maxSouls = message.maxSouls;
            spawnerEntity.loadRendererFromUpdate();
        }
    }
    
    public void loadRendererFromUpdate() {
        
        var spawned = EntityType.loadEntityRecursive(mobNbt, level, (entity) -> entity);
        if (spawned == null) return;
        
        if (renderedEntity == null || spawned.getType() != renderedEntity.getType()) {
            renderedEntity = spawned;
        }
    }
    
    @Override
    public void setChanged() {
        super.setChanged();
        this.networkDirty = true;
    }
    
    @Override
    public boolean canAcceptSoul() {
        return collectedSouls < maxSouls;
    }
    
    private void updateComparator() {
        var progress = getComparatorOutput();
        if (lastComparatorOutput != progress) {
            lastComparatorOutput = progress;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        
    }
    
    @Override
    public int getComparatorOutput() {
        if (mobNbt.isEmpty() || maxSouls == 0) return 0;
        
        return (int) (collectedSouls / (float) maxSouls * 15);
    }
    
    public void setRedstonePowered(boolean active) {
        this.redstonePowered = active;
    }
    
    @Override
    public void onSoulIncoming(Vec3 source) {
        var distance = (float) source.distanceTo(worldPosition.getCenter());
        collectedSouls++;
        
        var soulPath = worldPosition.getCenter().subtract(source);
        var animData = new ParticleContent.SoulParticleData(soulPath, (int) getSoulTravelDuration(distance));
        
        ParticleContent.WANDERING_SOUL.spawn(level, source.add(0, 0.7f, 0), animData);
        networkDirty = true;
        updateComparator();
    }
    
    private int getSoulCost(int maxHp) {
        return (int) (Math.sqrt(maxHp) + 0.5f) * Oritech.CONFIG.spawnerCostMultiplier();
    }
    
    public void onEntitySteppedOn(Entity entity) {
        if (!mobNbt.isEmpty()) return;
        
        if (entity instanceof Mob mobEntity) {
            
            if (mobEntity.getType().arch$holder().is(TagContent.SPAWNER_BLACKLIST)) {
                Oritech.LOGGER.debug("Ignored blacklisted entity for spawner: " + mobEntity.getType().arch$registryName());
                return;
            }
            
            var nbt = new CompoundTag();
            
            mobEntity.save(nbt);
            this.mobNbt = nbt;
            
            networkDirty = true;
            maxSouls = getSoulCost((int) mobEntity.getMaxHealth());
            
            mobEntity.remove(Entity.RemovalReason.DISCARDED);
            reloadCage(null);
            
            this.setChanged();
        }
    }
    
    public void onBlockInteracted(Player player) {
        
        if (mobNbt.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.oritech.spawner.no_mob"));
            return;
        }
        
        networkDirty = true;
        
        reloadCage(player);
        
        if (hasCage)
            player.sendSystemMessage(Component.translatable("tooltip.oritech.spawner.collected_souls", collectedSouls, maxSouls));
    }
    
    private void reloadCage(@Nullable Player player) {
        
        var spawned = EntityType.loadEntityRecursive(mobNbt, level, (entity) -> entity);
        if (spawned == null) return;
        
        var cageSize = new Vec3i(Math.round(spawned.getBbWidth() * 2 + 0.5f), Math.round(spawned.getBbHeight() + 0.5f), Math.round(spawned.getBbWidth() * 2 + 0.5f));
        var offset = cageSize.getX() / 2;
        
        hasCage = true;
        
        for (int x = 0; x < cageSize.getX(); x++) {
            for (int y = 0; y < cageSize.getY(); y++) {
                for (int z = 0; z < cageSize.getZ(); z++) {
                    var candidate = worldPosition.offset(-offset + x, -y - 1, -offset + z);
                    
                    // block type is a placeholder
                    if (!level.getBlockState(candidate).getBlock().equals(BlockContent.SPAWNER_CAGE_BLOCK)) {
                        hasCage = false;
                        ParticleContent.DEBUG_BLOCK.spawn(level, Vec3.atLowerCornerOf(candidate));
                    }
                    
                }
            }
        }
        
        if (!hasCage && player != null) {
            player.sendSystemMessage(Component.translatable("message.oritech.spawner.no_cage"));
        }
        
        this.setChanged();
    }
    
    public record SpawnerSyncPacket(BlockPos position, CompoundTag spawnedMob, boolean hasCage, int collectedSouls,
                                    int maxSouls) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<SpawnerSyncPacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("spawner"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
    {
    }
}
