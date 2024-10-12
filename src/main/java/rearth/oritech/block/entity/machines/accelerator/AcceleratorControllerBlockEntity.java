package rearth.oritech.block.entity.machines.accelerator;

import io.wispforest.owo.util.VectorRandomUtils;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Portal;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.AcceleratorScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.SoundContent;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;

import java.util.List;

public class AcceleratorControllerBlockEntity extends BlockEntity implements BlockEntityTicker<AcceleratorControllerBlockEntity>, InventoryProvider, ExtendedScreenHandlerFactory, ScreenProvider {
    
    private AcceleratorParticleLogic.ActiveParticle particle;
    public ItemStack activeItemParticle = ItemStack.EMPTY;
    
    private AcceleratorParticleLogic particleLogic;
    
    private final SimpleInventory inventory = new SimpleSidedInventory(2, new InventorySlotAssignment(0, 1, 1, 1));   // 0 = input, 1 = output
    
    // client data
    public List<Vec3d> displayTrail;
    public LastEventPacket lastEvent = new LastEventPacket(pos, ParticleEvent.IDLE, 0, pos, 1, ItemStack.EMPTY);
    private MovingSoundInstance movingSound;
    
    public AcceleratorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ACCELERATOR_CONTROLLER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, AcceleratorControllerBlockEntity blockEntity) {
        if (world.isClient) return;
        initParticleLogic();
        
        // try insert item as particle
        if (particle == null && !inventory.getStack(0).isEmpty() && inventory.getStack(1).isEmpty()) {
            injectParticle();
        }
        
        if (particle != null)
            particleLogic.update(particle);
        
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        
        if (particle != null && activeItemParticle != null && activeItemParticle != ItemStack.EMPTY) {
            var data = new NbtCompound();
            data.putFloat("speed", particle.velocity);
            data.putFloat("posX", (float) particle.position.x);
            data.putFloat("posY", (float) particle.position.y);
            data.putFloat("posZ", (float) particle.position.z);
            data.putLong("lastGate", particle.lastGate.asLong());
            data.putLong("nextGate", particle.nextGate.asLong());
            data.put("item", activeItemParticle.encode(registryLookup));
            nbt.put("particle", data);
        } else {
            nbt.remove("particle");
        }
        
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        
        if (nbt.contains("particle")) {
            var data = nbt.getCompound("particle");
            var speed = data.getFloat("speed");
            var posX = data.getFloat("posX");
            var posY = data.getFloat("posY");
            var posZ = data.getFloat("posZ");
            var lastGate = BlockPos.fromLong(data.getLong("lastGate"));
            var nextGate = BlockPos.fromLong(data.getLong("nextGate"));
            var item = ItemStack.fromNbt(registryLookup, data.get("item"));
            
            item.ifPresent(stack -> activeItemParticle = stack);
            particle = new AcceleratorParticleLogic.ActiveParticle(new Vec3d(posX, posY, posZ), speed, lastGate, nextGate);
        }
        
    }
    
    private void initParticleLogic() {
        if (particleLogic == null) particleLogic = new AcceleratorParticleLogic(pos, (ServerWorld) world, this);
    }
    
    public void injectParticle() {
        
        var facing = getCachedState().get(Properties.HORIZONTAL_FACING);
        var posBehind = Geometry.offsetToWorldPosition(facing, new Vec3i(1, 0, 0), pos);
        var directionRight = Geometry.getRight(facing);
        
        var candidateBlock = world.getBlockState(new BlockPos(posBehind));
        if (candidateBlock.getBlock().equals(BlockContent.ACCELERATOR_RING)) {
            var startPosition = (BlockPos) posBehind;
            var nextGate = particleLogic.findNextGate(startPosition, directionRight, 1);
            particle = new AcceleratorParticleLogic.ActiveParticle(startPosition.toCenterPos(), 1, nextGate, startPosition);
            activeItemParticle = inventory.getStack(0).split(1);
            
            NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.AcceleratorParticleInsertEventPacket(pos));
        }
    }
    
    public void removeParticleDueToCollision() {
        this.particle = null;
        this.activeItemParticle = ItemStack.EMPTY;
    }
    
    public void onParticleExited(Vec3d from, Vec3d to, BlockPos lastGate, Vec3d exitDirection, ParticleEvent reason) {
        
        var eventPosition = BlockPos.ofFloored(particle.position);
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new LastEventPacket(pos, reason, particle.velocity, eventPosition, particle.lastBendDistance + particle.lastBendDistance2, activeItemParticle));
        
        this.particle = null;
        
        var renderedTrail = List.of(from, to);
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.AcceleratorParticleRenderPacket(pos, renderedTrail));
    }
    
    public void onParticleCollided(float relativeSpeed, Vec3d collision, BlockPos secondController, AcceleratorControllerBlockEntity secondControllerEntity) {
        
        // create end portal area when two ender pearls collide, nether portal for two firecharges
        if (relativeSpeed > Oritech.CONFIG.endPortalRequiredSpeed() && activeItemParticle.getItem().equals(Items.ENDER_PEARL) && secondControllerEntity.activeItemParticle.getItem().equals(Items.ENDER_PEARL)) {
            spawnEndPortal(BlockPos.ofFloored(collision));
        } else if (relativeSpeed > Oritech.CONFIG.netherPortalRequiredSpeed() && activeItemParticle.getItem().equals(Items.FIRE_CHARGE) && secondControllerEntity.activeItemParticle.getItem().equals(Items.FIRE_CHARGE)) {
            spawnNetherPortal(BlockPos.ofFloored(collision));
        } else {
            var success = tryCraftResult(relativeSpeed, activeItemParticle, secondControllerEntity.activeItemParticle);
        }
        
        
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new LastEventPacket(pos, ParticleEvent.COLLIDED, relativeSpeed, BlockPos.ofFloored(particle.position), particle.lastBendDistance + particle.lastBendDistance2, activeItemParticle));
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new LastEventPacket(secondController, ParticleEvent.COLLIDED, relativeSpeed, BlockPos.ofFloored(particle.position), particle.lastBendDistance + particle.lastBendDistance2, activeItemParticle));
        
        this.removeParticleDueToCollision();
        secondControllerEntity.removeParticleDueToCollision();
        
        var particleCount = Math.max(Math.sqrt(Math.sqrt(relativeSpeed)), 3);
        for (int i = 0; i < particleCount + 5; i++) {
            var offset = VectorRandomUtils.getRandomOffset(world, collision, particleCount * 1.5);
            ParticleContent.WEED_KILLER.spawn(world, collision, new ParticleContent.LineData(collision, offset));
        }
        
        ParticleContent.PARTICLE_COLLIDE.spawn(world, collision);
    }
    
    private boolean tryCraftResult(float speed, ItemStack inputA, ItemStack inputB) {
        
        if (inputA == null || inputA.isEmpty() || inputB == null || inputB.isEmpty()) return false;
        
        var inputInv = new SimpleCraftingInventory(inputA, inputB);
        var candidate = world.getRecipeManager().getFirstMatch(RecipeContent.PARTICLE_COLLISION, inputInv, world);
        
        if (candidate.isEmpty()) {
            // try again in different order
            inputInv = new SimpleCraftingInventory(inputB, inputA);
            candidate = world.getRecipeManager().getFirstMatch(RecipeContent.PARTICLE_COLLISION, inputInv, world);
        }
        
        if (candidate.isEmpty()) return false;
        
        var recipe = candidate.get().value();
        
        var requiredSpeed = recipe.getTime();
        if (speed < requiredSpeed) return false;
        
        var result = recipe.getResults();
        if (inventory.heldStacks.get(1).getItem().equals(result.get(0).getItem())) {
            inventory.heldStacks.get(1).increment(1);
        } else {
            inventory.setStack(1, result.get(0).copy());
        }
        
        return true;
    }
    
    private void spawnEndPortal(BlockPos pos) {
        
        // create small end area around the portal
        for (var candidate : BlockPos.iterateOutwards(pos, 8, 4, 8)) {
            
            var dist = candidate.toCenterPos().distanceTo(pos.toCenterPos());
            if (world.random.nextFloat() < dist / 8) continue;
            
            var candidateState = world.getBlockState(candidate);
            if (candidateState.isAir() || candidateState.isReplaceable() || candidateState.getBlock().getHardness() < 0)
                continue;
            
            world.setBlockState(candidate, Blocks.END_STONE.getDefaultState());
            
            // generate chorus flowers
            if (world.random.nextFloat() > 0.8) {
                var stateAbove = world.getBlockState(candidate.up());
                if (stateAbove.isAir() || stateAbove.isReplaceable()) {
                    for (int i = 1; i < world.random.nextBetween(3, 6); i++) {
                        stateAbove = world.getBlockState(candidate.up(i));
                        if (stateAbove.isAir() || stateAbove.isReplaceable())
                            world.setBlockState(candidate.up(i), Blocks.CHORUS_PLANT.getDefaultState());
                    }
                }
            }
        }
        
        // create portal itself
        world.setBlockState(pos, Blocks.END_PORTAL.getDefaultState());
        world.setBlockState(pos.north(), Blocks.END_STONE.getDefaultState());
        world.setBlockState(pos.east(), Blocks.END_STONE.getDefaultState());
        world.setBlockState(pos.south(), Blocks.END_STONE.getDefaultState());
        world.setBlockState(pos.west(), Blocks.END_STONE.getDefaultState());
    }
    
    private void spawnNetherPortal(BlockPos pos) {
        
        // create small nether area around the portal
        for (var candidate : BlockPos.iterateOutwards(pos, 12, 4, 12)) {
            
            var dist = candidate.toCenterPos().distanceTo(pos.toCenterPos());
            if (world.random.nextFloat() < dist / 12) continue;
            
            var candidateState = world.getBlockState(candidate);
            if (candidateState.isAir() || candidateState.isReplaceable() || candidateState.getBlock().getHardness() < 0)
                continue;
            
            world.setBlockState(candidate, Blocks.NETHERRACK.getDefaultState());
            
            // generate fires
            if (world.random.nextFloat() > 0.8) {
                var stateAbove = world.getBlockState(candidate.up());
                if (stateAbove.isAir() || stateAbove.isReplaceable()) {
                    world.setBlockState(candidate.up(), Blocks.FIRE.getDefaultState());
                }
            }
        }
        
        // spawn obsidian frame (3x4), with 2 portal blocks in the center
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 4; y++) {
                world.setBlockState(pos.add(x, y, 0), Blocks.OBSIDIAN.getDefaultState());
            }
        }
        
        world.setBlockState(pos.add(1, 1, 0), Blocks.NETHER_PORTAL.getDefaultState());
        world.setBlockState(pos.add(1, 2, 0), Blocks.NETHER_PORTAL.getDefaultState());
        
    }
    
    public void onParticleMoved(List<Vec3d> positions) {
        
        if (positions.size() <= 1) return;
        
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.AcceleratorParticleRenderPacket(pos, positions));
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new LastEventPacket(pos, ParticleEvent.ACCELERATING, particle.velocity, BlockPos.ofFloored(particle.position), particle.lastBendDistance + particle.lastBendDistance2, activeItemParticle));
        
    }
    
    public AcceleratorParticleLogic.ActiveParticle getParticle() {
        return particle;
    }
    
    public void onParticleInsertedClient() {
        var soundPos = pos.toCenterPos();
        world.playSound(soundPos.x, soundPos.y, soundPos.z, SoundContent.CABLE_MOVING, SoundCategory.BLOCKS, 1f, 1f, true);
    }
    
    public void onReceiveMovement(List<Vec3d> displayTrail) {
        this.displayTrail = displayTrail;
        
        if (displayTrail.size() >= 2) {
            var pitch = Math.pow(lastEvent.lastEventSpeed, 0.1);
            for (int i = 1; i < displayTrail.size(); i++) {
                var soundPos = displayTrail.get(i);
                world.playSound(soundPos.x, soundPos.y, soundPos.z, SoundContent.PARTICLE_MOVING, SoundCategory.BLOCKS, 2f, (float) pitch, true);
            }
        }
        
    }
    
    // returns the amount of moment used
    public float handleParticleEntityCollision(BlockPos checkPos, AcceleratorParticleLogic.ActiveParticle particle, float remainingMomentum, LivingEntity mob) {
        
        var maxApplicableDamage = mob.getHealth();
        var inflictedDamage = Math.min(remainingMomentum, maxApplicableDamage);
        mob.damage(world.getDamageSources().magic(), remainingMomentum);
        var position = mob.getBoundingBox().getCenter();
        position = new Vec3d(position.x, particle.position.y, position.z);
        ParticleContent.BIG_HIT.spawn(world, position);
        
        return inflictedDamage;
    }
    
    public float handleParticleBlockCollision(BlockPos checkPos, AcceleratorParticleLogic.ActiveParticle particle, float remainingMomentum, BlockState hitState) {
        
        var blockHardness = hitState.getHardness(world, checkPos);
        
        // hit portal, create black hole with explosion
        if (remainingMomentum > Oritech.CONFIG.blackHoleRequiredSpeed() && hitState.getBlock() instanceof Portal) {
            createBlackHole(checkPos);
            return remainingMomentum;
        }
        
        if (blockHardness < 0)  // unbreakable block
            return remainingMomentum;
        
        if (remainingMomentum > blockHardness) {
            world.addBlockBreakParticles(checkPos, hitState);
            world.playSound(null, checkPos, hitState.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1f, 1f);
            world.breakBlock(checkPos, true);
        }
        
        return blockHardness;
    }
    
    private void createBlackHole(BlockPos checkPos) {
        ParticleContent.MELTDOWN_IMMINENT.spawn(world, checkPos.toCenterPos(), 30);
        
        var center = checkPos.toCenterPos();
        world.createExplosion(null, center.x, center.y, center.z, 10, false, World.ExplosionSourceType.BLOCK);
        
        world.removeBlock(checkPos, false);
        world.setBlockState(checkPos, BlockContent.BLACK_HOLE_BLOCK.getDefaultState());
    }
    
    public void handleParticleMotorInteraction(BlockPos motorBlock) {
        
        var entity = world.getBlockEntity(motorBlock);
        if (!(entity instanceof AcceleratorMotorBlockEntity motorEntity)) return;
        
        var storage = motorEntity.getStorage(null);
        var availableEnergy = storage.getAmount();
        
        var speed = particle.velocity;
        var cost = speed * Oritech.CONFIG.accelerationRFCost();
        if (availableEnergy < cost) return;
        
        try (var tx = Transaction.openOuter()) {
            storage.extract((long) cost, tx);
            tx.commit();
        }
        
        particle.velocity += 1;
        
    }
    
    public void onReceivedEvent(LastEventPacket event) {
        this.lastEvent = event;
        
        var soundPos = event.lastEventPosition.toCenterPos();
        if (event.lastEvent.equals(ParticleEvent.COLLIDED)) {
            world.playSound(soundPos.x, soundPos.y, soundPos.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.BLOCKS, 5f, 1, true);
        } else if (event.lastEvent.equals(ParticleEvent.EXITED_FAST) || event.lastEvent.equals(ParticleEvent.EXITED_NO_GATE)) {
            world.playSound(soundPos.x, soundPos.y, soundPos.z, SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST.value(), SoundCategory.BLOCKS, 3f, 1, true);
        }
        
    }
    
    @Override
    public InventoryStorage getInventory(Direction direction) {
        return InventoryStorage.of(inventory, direction);
    }
    
    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        return new ModScreens.BasicData(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.literal("");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
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
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
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
    
    public record LastEventPacket(BlockPos position,
                                  ParticleEvent lastEvent,
// for no gate found events, we can calculate the acceptable dist based on speed
                                  float lastEventSpeed,
// this is particle speed usually, and collision speed for collisions
                                  BlockPos lastEventPosition,  // where it collided/exited
                                  float minBendDist,   // acceptable dist can be calculated from dist
                                  ItemStack activeParticle
    ) {
    }
    
    public enum ParticleEvent {
        IDLE,   // nothing was insert yet
        ERROR,  // no ring was found
        ACCELERATING,   // particle is in collider
        COLLIDED,
        EXITED_FAST,    // particle was too fast to take curve
        EXITED_NO_GATE  // no gate found in range
    }
}
