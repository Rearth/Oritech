package rearth.oritech.block.entity.arcane;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import rearth.oritech.util.ComparatorOutputProvider;

import java.util.UUID;

public class SpawnerControllerBlockEntity extends BaseSoulCollectionEntity implements BlockEntityTicker<SpawnerControllerBlockEntity>, ComparatorOutputProvider {
    private static final String[] STRIPPED_MOB_NBT_KEYS = {
        "ArmorDropChances", "ArmorItems", "BodyArmorDropChance", "body_armor_drop_chance", "BodyArmorItem", "body_armor_item",
        "CanPickUpLoot", "ChestedHorse", "DecorItem", "HandDropChances", "HandItems", "Inventory", "Item", "Items",
        "Leash", "Passengers", "SaddleItem"
    };
    
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

        if (networkDirty) {
            updateNetwork();
            DeathListener.resetEvents();
        }

        if (mobNbt.isEmpty() || !hasCage || redstonePowered) return;
        
        if (collectedSouls >= maxSouls && world.getGameTime() % 4 == 0) {
            spawnMob();
            updateComparator();
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
        mobNbt = sanitizeMobNbt(nbt.getCompound("mobNbt"));
        lastComparatorOutput = getComparatorOutput();
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
            clearMobEquipment(entity);
            if (level instanceof ServerLevel sl) { var c = targetPosition.getCenter(); sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y, c.z, maxSouls, 1.2, 1.2, 1.2, 0); }
            
            return entity;
        });
        
        if (spawned == null) return;
        
        level.addFreshEntity(spawned);
        collectedSouls -= maxSouls;
        this.setChanged();
        
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
                    if (foundFree >= requiredHeight) {
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
            spawnerEntity.lastComparatorOutput = spawnerEntity.getComparatorOutput();
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
        if (this.redstonePowered == active) return;
        this.redstonePowered = active;
        this.setChanged();
    }
    
    @Override
    public void onSoulIncoming(Vec3 source) {
        var distance = (float) source.distanceTo(worldPosition.getCenter());
        collectedSouls++;
        
        var soulPath = worldPosition.getCenter().subtract(source);
        ParticleContent.WanderingSoul(level, source.add(0, 0.7f, 0), soulPath, (int) getSoulTravelDuration(distance));
        updateComparator();
        this.setChanged();
    }
    
    private int getSoulCost(int maxHp) {
        return (int) (Math.sqrt(maxHp) + 0.5f) * OritechConfig.spawnerCostMultiplier.get();
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
            this.mobNbt = sanitizeMobNbt(nbt);
            
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
                        ParticleContent.DebugBlock(level, Vec3.atLowerCornerOf(candidate));
                    }
                    
                }
            }
        }
        
        if (!hasCage && player != null) {
            player.sendSystemMessage(Component.translatable("message.oritech.spawner.no_cage"));
        }
        
        this.setChanged();
    }

    private CompoundTag sanitizeMobNbt(CompoundTag nbt) {
        var sanitizedNbt = nbt.copy();
        for (var key : STRIPPED_MOB_NBT_KEYS) {
            sanitizedNbt.remove(key);
        }
        return sanitizedNbt;
    }

    private void clearMobEquipment(Entity entity) {
        if (!(entity instanceof Mob mobEntity)) return;

        for (var slot : EquipmentSlot.values()) {
            mobEntity.setItemSlot(slot, ItemStack.EMPTY);
        }
    }
    
    public record SpawnerSyncPacket(BlockPos position, CompoundTag spawnedMob, boolean hasCage, int collectedSouls,
                                    int maxSouls) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<SpawnerSyncPacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("spawner"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
