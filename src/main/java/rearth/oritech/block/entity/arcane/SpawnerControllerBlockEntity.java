package rearth.oritech.block.entity.arcane;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;

public class SpawnerControllerBlockEntity extends BaseSoulCollectionEntity implements BlockEntityTicker<SpawnerControllerBlockEntity> {
    
    public int maxSouls = 10;
    public int collectedSouls = 0;
    
    public EntityType<?> spawnedMob;
    public Entity renderedEntity;
    private boolean networkDirty;
    public boolean hasCage;
    
    // client only
    public float lastProgress = 0f;
    
    public SpawnerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.SPAWNER_CONTROLLER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, SpawnerControllerBlockEntity blockEntity) {
        
        if (world.isClient) return;
        
        if (collectedSouls >= maxSouls && world.getTime() % 4 == 0) {
            spawnMob();
        }
        
        if (networkDirty)
            updateNetwork();
        
    }
    
    private void spawnMob() {
        // try and find a valid position within 10 attempts
        var spawnRange = 4;
        var requiredHeight = Math.round(spawnedMob.getHeight() + 0.5f);
        
        var targetPosition = findSpawnPosition(spawnRange, requiredHeight);
        
        System.out.println("spawning at " + targetPosition);
        
        if (targetPosition == null) return;
        networkDirty = true;
        
        spawnedMob.spawn((ServerWorld) world, targetPosition, SpawnReason.SPAWNER);
        collectedSouls -= maxSouls;
        ParticleContent.SOUL_USED.spawn(world, targetPosition.toCenterPos(), maxSouls);
        
    }
    
    private BlockPos findSpawnPosition(int spawnRange, int requiredHeight) {
        for (int i = 0; i < 10; i++) {
            var candidate = pos.add(world.random.nextBetween(-spawnRange, spawnRange), 3, world.random.nextBetween(-spawnRange, spawnRange));
            var foundFree = 0;
            for (int j = 0; j < 9; j++) {
                var state = world.getBlockState(candidate.down(j));
                if (state.isAir()) {
                    foundFree++;
                } else {
                    if (foundFree > requiredHeight) {
                        // found target
                        return candidate.down(j - 1);
                        
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
        
        if (spawnedMob != null)
            NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.SpawnerSyncPacket(pos, Registries.ENTITY_TYPE.getId(spawnedMob), hasCage, collectedSouls, maxSouls));
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        this.networkDirty = true;
    }
    
    public void loadEntityFromIdentifier(Identifier identifier) {
        var newMob = Registries.ENTITY_TYPE.get(identifier);
        
        if (newMob != spawnedMob) {
            spawnedMob = newMob;
            System.out.println("got new mob on client: " + spawnedMob);
            renderedEntity = spawnedMob.create(world);
        }
        
    }
    
    @Override
    public boolean canAcceptSoul() {
        return collectedSouls < maxSouls;
    }
    
    @Override
    public void onSoulIncoming(Vec3d source) {
        var distance = (float) source.distanceTo(pos.toCenterPos());
        collectedSouls++;
        
        var soulPath = pos.toCenterPos().subtract(source);
        var animData = new ParticleContent.SoulParticleData(soulPath, (int) getSoulTravelDuration(distance));
        
        ParticleContent.WANDERING_SOUL.spawn(world, source.add(0, 0.7f, 0), animData);
        networkDirty = true;
    }
    
    private int getSoulCost(int maxHp) {
        return (int) (Math.sqrt(maxHp) + 0.5f);
    }
    
    public void onEntitySteppedOn(Entity entity) {
        // if (spawnedMob != null) return;
        
        if (entity instanceof MobEntity mobEntity) {
            spawnedMob = mobEntity.getType();
            networkDirty = true;
            maxSouls = getSoulCost((int) mobEntity.getMaxHealth());
            
            mobEntity.remove(Entity.RemovalReason.DISCARDED);
            reloadCage();
        }
    }
    
    public void onBlockInteracted() {
        if (spawnedMob == null) return;
        
        networkDirty = true;
        
        reloadCage();
        
    }
    
    private void reloadCage() {
        var cageSize = new Vec3i(Math.round(spawnedMob.getWidth() * 2 + 0.5f), Math.round(spawnedMob.getHeight() + 0.5f), Math.round(spawnedMob.getWidth() * 2 + 0.5f));
        var offset = cageSize.getX() / 2;
        
        hasCage = true;
        
        for (int x = 0; x < cageSize.getX(); x++) {
            for (int y = 0; y < cageSize.getY(); y++) {
                for (int z = 0; z < cageSize.getZ(); z++) {
                    var candidate = pos.add(-offset + x, -y - 1, -offset + z);
                    
                    // block type is a placeholder
                    if (!world.getBlockState(candidate).getBlock().equals(BlockContent.SPAWNER_CAGE_BLOCK)) {
                        hasCage = false;
                        ParticleContent.DEBUG_BLOCK.spawn(world, Vec3d.of(candidate));
                    }
                    
                }
            }
        }
    }
}
