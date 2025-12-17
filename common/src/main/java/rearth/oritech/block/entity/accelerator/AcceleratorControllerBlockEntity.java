package rearth.oritech.block.entity.accelerator;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import io.wispforest.owo.util.VectorRandomUtils;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi.EnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.InOutInventoryStorage;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.AcceleratorScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.SoundContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

// networking: last event could be automated. Inject event can just be called on server, and let vanilla handle sounds. Trail should be sent normally,
// so maybe everything could just be moved to manually sent packets
public class AcceleratorControllerBlockEntity extends BlockEntity implements BlockEntityTicker<AcceleratorControllerBlockEntity>, ItemApi.BlockProvider, ExtendedMenuProvider, ScreenProvider {
    
    private AcceleratorParticleLogic.ActiveParticle particle;
    private AcceleratorParticleLogic.ActiveParticle lastParticle;
    public ItemStack activeItemParticle = ItemStack.EMPTY;
    
    private AcceleratorParticleLogic particleLogic;
    
    public final InOutInventoryStorage inventory = new InOutInventoryStorage(2, this::setChanged, new InventorySlotAssignment(0, 1, 1, 1));
    
    // client data
    public List<Vec3> displayTrail;
    public LastEventPacket lastEvent = new LastEventPacket(worldPosition, ParticleEvent.IDLE, 0, worldPosition, 1, ItemStack.EMPTY);
    
    public AcceleratorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ACCELERATOR_CONTROLLER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, AcceleratorControllerBlockEntity blockEntity) {
        if (world.isClientSide) return;
        initParticleLogic();
        
        // try insert item as particle
        if (particle == null && !inventory.getItem(0).isEmpty() && inventory.getItem(1).isEmpty()) {
            injectParticle();
        }
        
        if (particle != null)
            particleLogic.update(particle);
        
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
        
        if (particle != null && activeItemParticle != null && activeItemParticle != ItemStack.EMPTY) {
            var data = new CompoundTag();
            data.putFloat("speed", particle.velocity);
            data.putFloat("posX", (float) particle.position.x);
            data.putFloat("posY", (float) particle.position.y);
            data.putFloat("posZ", (float) particle.position.z);
            data.putLong("lastGate", particle.lastGate.asLong());
            data.putLong("nextGate", particle.nextGate.asLong());
            data.put("item", activeItemParticle.save(registryLookup));
            nbt.put("particle", data);
        } else {
            nbt.remove("particle");
        }
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
        
        if (nbt.contains("particle")) {
            var data = nbt.getCompound("particle");
            var speed = data.getFloat("speed");
            var posX = data.getFloat("posX");
            var posY = data.getFloat("posY");
            var posZ = data.getFloat("posZ");
            var lastGate = BlockPos.of(data.getLong("lastGate"));
            var nextGate = BlockPos.of(data.getLong("nextGate"));
            var item = ItemStack.parse(registryLookup, data.get("item"));
            
            item.ifPresent(stack -> activeItemParticle = stack);
            particle = new AcceleratorParticleLogic.ActiveParticle(new Vec3(posX, posY, posZ), speed, lastGate, nextGate);
        }
    }
    
    private void initParticleLogic() {
        if (particleLogic == null) particleLogic = new AcceleratorParticleLogic(worldPosition, (ServerLevel) level, this);
    }
    
    public void injectParticle() {
        
        var facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        var posBehind = Geometry.offsetToWorldPosition(facing, new Vec3i(1, 0, 0), worldPosition);
        var directionRight = Geometry.getRight(facing);
        
        var candidateBlock = level.getBlockState(new BlockPos(posBehind));
        if (candidateBlock.getBlock().equals(BlockContent.ACCELERATOR_RING)) {
            var startPosition = (BlockPos) posBehind;
            var nextGate = particleLogic.findNextGate(startPosition, directionRight, 1);
            particle = new AcceleratorParticleLogic.ActiveParticle(startPosition.getCenter(), 1, nextGate, startPosition);
            activeItemParticle = inventory.getItem(0).split(1);
            
            var soundPos = worldPosition.getCenter();
            level.playSound(null, soundPos.x, soundPos.y, soundPos.z, SoundEvents.BAMBOO_WOOD_TRAPDOOR_OPEN, SoundSource.BLOCKS);
        }
    }
    
    public void removeParticleDueToCollision() {
        this.particle = null;
        this.activeItemParticle = ItemStack.EMPTY;
    }
    
    public void onParticleExited(Vec3 from, Vec3 to, BlockPos lastGate, Vec3 exitDirection, ParticleEvent reason) {
        
        var eventPosition = BlockPos.containing(particle.position);
        NetworkManager.sendBlockHandle(this, new LastEventPacket(worldPosition, reason, particle.velocity, eventPosition, AcceleratorParticleLogic.getParticleBendDist(particle.lastBendDistance, particle.lastBendDistance2), activeItemParticle));
        
        this.lastParticle = particle;
        this.particle = null;
        
        var renderedTrail = List.of(from, to);
        NetworkManager.sendBlockHandle(this, new ParticleRenderTrail(worldPosition, renderedTrail));
        
        this.setChanged();
    }
    
    public void onParticleCollided(float relativeSpeed, Vec3 collision, AcceleratorControllerBlockEntity secondControllerEntity) {
        
        // create end portal area when two ender pearls collide, nether portal for two firecharges
        if (relativeSpeed > Oritech.CONFIG.endPortalRequiredSpeed() && activeItemParticle.getItem().equals(Items.ENDER_PEARL) && secondControllerEntity.activeItemParticle.getItem().equals(Items.ENDER_PEARL)) {
            spawnEndPortal(BlockPos.containing(collision));
        } else if (relativeSpeed > Oritech.CONFIG.netherPortalRequiredSpeed() && activeItemParticle.getItem().equals(Items.FIRE_CHARGE) && secondControllerEntity.activeItemParticle.getItem().equals(Items.FIRE_CHARGE)) {
            spawnNetherPortal(BlockPos.containing(collision));
        } else {
            var success = tryCraftResult(relativeSpeed, activeItemParticle, secondControllerEntity.activeItemParticle);
        }
        
        NetworkManager.sendBlockHandle(this, new LastEventPacket(worldPosition, ParticleEvent.COLLIDED, relativeSpeed, BlockPos.containing(particle.position), AcceleratorParticleLogic.getParticleBendDist(particle.lastBendDistance, particle.lastBendDistance2), activeItemParticle));
        NetworkManager.sendBlockHandle(this, new LastEventPacket(secondControllerEntity.getBlockPos(), ParticleEvent.COLLIDED, relativeSpeed, BlockPos.containing(particle.position), AcceleratorParticleLogic.getParticleBendDist(particle.lastBendDistance, particle.lastBendDistance2), activeItemParticle));
        
        this.removeParticleDueToCollision();
        secondControllerEntity.removeParticleDueToCollision();
        
        var particleCount = Math.pow(relativeSpeed, 0.5) / 2f + 1;
        createCollisionParticles((int) relativeSpeed, collision, (int) particleCount);
        
        ParticleContent.PARTICLE_COLLIDE.spawn(level, collision);
        this.setChanged();
    }
    
    private void createCollisionParticles(int collisionEnergy, Vec3 collisionPosition, int shotCount) {
        
        var energyMultiplier = 3 * Oritech.CONFIG.tachyonCollisionEnergyFactor();
        int energyPotential = (int) (Math.pow(collisionEnergy / 2f, 2) * energyMultiplier * Oritech.CONFIG.accelerationRFCost());    // exactly N times the amount of energy used to accelerate
        var energyPerRay = energyPotential / shotCount;
        var rayRange = shotCount / 3;
        
        var caughtParticles = 0;
        
        for (int i = 0; i < shotCount; i++) {
            var offset = VectorRandomUtils.getRandomOffset(level, collisionPosition, rayRange);
            var direction = offset.subtract(collisionPosition).normalize();
            
            var impactPos = BlackHoleBlockEntity.basicRaycast(collisionPosition.add(direction.scale(1.2)), direction, rayRange, level);
            if (impactPos != null) {
                ParticleContent.BLACK_HOLE_EMISSION.spawn(level, collisionPosition, impactPos.getCenter());
                // ParticleContent.DEBUG_BLOCK.spawn(world, Vec3d.of(impactPos));
                
                var candidate = level.getBlockEntity(impactPos);
                if (candidate instanceof ParticleCollectorBlockEntity collectorEntity) {
                    collectorEntity.onParticleCollided(energyPerRay);
                    caughtParticles++;
                }
            } else {
                ParticleContent.BLACK_HOLE_EMISSION.spawn(level, collisionPosition, offset);
            }
            
            // System.out.println("caught: " + caughtParticles + " of " + shotCount);
        }
    
    }
    
    private boolean tryCraftResult(float speed, ItemStack inputA, ItemStack inputB) {
        
        if (inputA == null || inputA.isEmpty() || inputB == null || inputB.isEmpty()) return false;
        
        var inputInv = new SimpleCraftingInventory(inputA, inputB);
        var candidate = level.getRecipeManager().getRecipeFor(RecipeContent.PARTICLE_COLLISION, inputInv, level);
        
        if (candidate.isEmpty()) {
            // try again in different order
            inputInv = new SimpleCraftingInventory(inputB, inputA);
            candidate = level.getRecipeManager().getRecipeFor(RecipeContent.PARTICLE_COLLISION, inputInv, level);
        }
        
        if (candidate.isEmpty()) return false;
        
        var recipe = candidate.get().value();
        
        var requiredSpeed = recipe.getTime();
        if (speed < requiredSpeed) return false;
        
        var result = recipe.getResults();
        if (inventory.heldStacks.get(1).getItem().equals(result.get(0).getItem())) {
            inventory.heldStacks.get(1).grow(1);
        } else {
            inventory.setItem(1, result.get(0).copy());
        }
        
        return true;
    }
    
    private void spawnEndPortal(BlockPos pos) {
        
        // create small end area around the portal
        for (var candidate : BlockPos.withinManhattan(pos, 8, 4, 8)) {
            
            var dist = candidate.getCenter().distanceTo(pos.getCenter());
            if (level.random.nextFloat() < dist / 8) continue;
            
            var candidateState = level.getBlockState(candidate);
            if (candidateState.isAir() || candidateState.canBeReplaced() || candidateState.getBlock().defaultDestroyTime() < 0)
                continue;
            
            if (!level.getBlockState(candidate.below()).getBlock().equals(Blocks.CHORUS_PLANT))
                level.setBlockAndUpdate(candidate, Blocks.END_STONE.defaultBlockState());
            
            // generate chorus flowers
            if (level.random.nextFloat() > 0.8) {
                var stateAbove = level.getBlockState(candidate.above());
                if (stateAbove.isAir() || stateAbove.canBeReplaced()) {
                    for (int i = 1; i < level.random.nextIntBetweenInclusive(3, 6); i++) {
                        stateAbove = level.getBlockState(candidate.above(i));
                        if (stateAbove.isAir() || stateAbove.canBeReplaced())
                            level.setBlockAndUpdate(candidate.above(i), Blocks.CHORUS_PLANT.defaultBlockState());
                    }
                }
            }
        }
        
        // create portal itself
        level.setBlockAndUpdate(pos, Blocks.END_PORTAL.defaultBlockState());
        level.setBlockAndUpdate(pos.north(), Blocks.END_STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Blocks.END_STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.south(), Blocks.END_STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.west(), Blocks.END_STONE.defaultBlockState());
    }
    
    private void spawnNetherPortal(BlockPos pos) {
        
        // create small nether area around the portal
        for (var candidate : BlockPos.withinManhattan(pos, 12, 4, 12)) {
            
            var dist = candidate.getCenter().distanceTo(pos.getCenter());
            if (level.random.nextFloat() < dist / 12) continue;
            
            var candidateState = level.getBlockState(candidate);
            if (candidateState.isAir() || candidateState.canBeReplaced() || candidateState.getBlock().defaultDestroyTime() < 0)
                continue;
            
            level.setBlockAndUpdate(candidate, Blocks.NETHERRACK.defaultBlockState());
            
            // generate fires
            if (level.random.nextFloat() > 0.8) {
                var stateAbove = level.getBlockState(candidate.above());
                if (stateAbove.isAir() || stateAbove.canBeReplaced()) {
                    level.setBlockAndUpdate(candidate.above(), Blocks.FIRE.defaultBlockState());
                }
            }
        }
        
        // spawn obsidian frame (3x4), with 2 portal blocks in the center
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 4; y++) {
                level.setBlockAndUpdate(pos.offset(x, y, 0), Blocks.OBSIDIAN.defaultBlockState());
            }
        }
        
        level.setBlockAndUpdate(pos.offset(1, 1, 0), Blocks.NETHER_PORTAL.defaultBlockState());
        level.setBlockAndUpdate(pos.offset(1, 2, 0), Blocks.NETHER_PORTAL.defaultBlockState());
        
    }
    
    public void onParticleMoved(List<Vec3> positions) {
        
        if (positions.size() <= 1) return;
        
        var resultList = new ArrayList<Vec3>();
        
        // deduplicate / shorten list
        var positionSet = new HashSet<Vec3>();
        for (var position : positions) {
            if (positionSet.contains(position)) {
                // loop reached, stop the list
                break;
            }
            
            positionSet.add(position);
            resultList.add(position);
        }
        
        NetworkManager.sendBlockHandle(this, new ParticleRenderTrail(worldPosition, resultList));
        NetworkManager.sendBlockHandle(this, new LastEventPacket(worldPosition, ParticleEvent.ACCELERATING, particle.velocity, BlockPos.containing(particle.position), AcceleratorParticleLogic.getParticleBendDist(particle.lastBendDistance, particle.lastBendDistance2), activeItemParticle));
        
    }
    
    public AcceleratorParticleLogic.ActiveParticle getParticle() {
        if (particle == null && lastParticle != null) return lastParticle;  // helper for edge case collisions
        return particle;
    }
    
    // returns the amount of moment used
    public float handleParticleEntityCollision(BlockPos checkPos, AcceleratorParticleLogic.ActiveParticle particle, float remainingMomentum, LivingEntity mob) {
        
        var maxApplicableDamage = mob.getHealth();
        var inflictedDamage = Math.min(remainingMomentum, maxApplicableDamage);
        mob.hurt(level.damageSources().magic(), remainingMomentum);
        var position = mob.getBoundingBox().getCenter();
        position = new Vec3(position.x, particle.position.y, position.z);
        ParticleContent.BIG_HIT.spawn(level, position);
        
        return inflictedDamage;
    }
    
    public float handleParticleBlockCollision(BlockPos checkPos, AcceleratorParticleLogic.ActiveParticle particle, float remainingMomentum, BlockState hitState) {
        
        var blockHardness = hitState.getDestroySpeed(level, checkPos);
        
        // hit portal, create black hole with explosion
        if (remainingMomentum > Oritech.CONFIG.blackHoleRequiredSpeed() && hitState.getBlock() instanceof Portal) {
            createBlackHole(checkPos);
            return remainingMomentum;
        }
        
        if (blockHardness < 0)  // unbreakable block
            return remainingMomentum;
        
        if (remainingMomentum > blockHardness) {
            level.addDestroyBlockEffect(checkPos, hitState);
            level.playSound(null, checkPos, hitState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
            level.destroyBlock(checkPos, true);
        }
        
        return blockHardness;
    }
    
    private void createBlackHole(BlockPos checkPos) {
        ParticleContent.MELTDOWN_IMMINENT.spawn(level, checkPos.getCenter(), 30);
        
        var center = checkPos.getCenter();
        level.explode(null, center.x, center.y, center.z, 10, false, Level.ExplosionInteraction.BLOCK);
        
        level.removeBlock(checkPos, false);
        level.setBlockAndUpdate(checkPos, BlockContent.BLACK_HOLE_BLOCK.defaultBlockState());
    }
    
    public void handleParticleMotorInteraction(BlockPos motorBlock) {
        
        var entity = level.getBlockEntity(motorBlock);
        if (!(entity instanceof AcceleratorMotorBlockEntity motorEntity)) return;
        
        var storage = motorEntity.getEnergyStorage(null);
        var availableEnergy = storage.getAmount();
        
        var speed = particle.velocity;
        var cost = speed * Oritech.CONFIG.accelerationRFCost();
        if (availableEnergy < cost) return;
        
        storage.extract((long) cost, false);
        storage.update();
        
        particle.velocity += 1;
        
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new AcceleratorScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 7, 10),
          new GuiSlot(1, 7, 60, true));
    }
    
    @Override
    public boolean showEnergy() {
        return false;
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return 0;
    }
    
    @Override
    public float getProgress() {
        return 0;
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }
    
    @Override
    public Container getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.ACCELERATOR_SCREEN;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public boolean showProgress() {
        return false;
    }
    
    public static void receiveTrail(ParticleRenderTrail packet, Level world, RegistryAccess dynamicRegistryManager) {
        if (world.getBlockEntity(packet.position) instanceof AcceleratorControllerBlockEntity acceleratorBlock) {
            var displayTrail = packet.particleTrail;
            acceleratorBlock.displayTrail = displayTrail;
            if (displayTrail.size() < 2) return;
            
            var playerPos = Minecraft.getInstance().player.position();
            
            // play sound pos at closest segment
            var minDist = Double.MAX_VALUE;
            var soundPos = displayTrail.getFirst();
            for (var candidate : displayTrail) {
                var dist = candidate.distanceTo(playerPos);
                if (dist < minDist) {
                    minDist = dist;
                    soundPos = candidate;
                }
            }
            
            var pitch = Math.pow(acceleratorBlock.lastEvent.lastEventSpeed, 0.1);
            world.playLocalSound(soundPos.x, soundPos.y, soundPos.z, SoundContent.PARTICLE_MOVING, SoundSource.BLOCKS, 2f, (float) pitch, true);
            
        }
    }
    
    public static void receiveEvent(LastEventPacket packet, Level world, RegistryAccess dynamicRegistryManager) {
        if (world.getBlockEntity(packet.position) instanceof AcceleratorControllerBlockEntity acceleratorBlock) {
            acceleratorBlock.lastEvent = packet;
            
            var soundPos = packet.lastEventPosition.getCenter();
            if (packet.lastEvent.equals(ParticleEvent.COLLIDED)) {
                world.playLocalSound(soundPos.x, soundPos.y, soundPos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 5f, 1, true);
            } else if (packet.lastEvent.equals(ParticleEvent.EXITED_FAST) || packet.lastEvent.equals(ParticleEvent.EXITED_NO_GATE)) {
                world.playLocalSound(soundPos.x, soundPos.y, soundPos.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.BLOCKS, 3f, 1, true);
            }
        }
    }
    
    public record LastEventPacket(BlockPos position,
                                  ParticleEvent lastEvent,
// for no gate found events, we can calculate the acceptable dist based on speed
                                  float lastEventSpeed,
// this is particle speed usually, and collision speed for collisions
                                  BlockPos lastEventPosition,  // where it collided/exited
                                  float minBendDist,   // acceptable dist can be calculated from dist
                                  ItemStack activeParticle
    ) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<LastEventPacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("accel_event"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
    public enum ParticleEvent {
        IDLE,   // nothing was insert yet
        ERROR,  // no ring was found
        ACCELERATING,   // particle is in collider
        COLLIDED,
        EXITED_FAST,    // particle was too fast to take curve
        EXITED_NO_GATE  // no gate found in range
    }
    
    public record ParticleRenderTrail(BlockPos position, List<Vec3> particleTrail) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<ParticleRenderTrail> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("accel_render"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
