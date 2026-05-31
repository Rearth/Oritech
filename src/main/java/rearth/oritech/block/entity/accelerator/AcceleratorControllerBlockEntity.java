package rearth.oritech.block.entity.accelerator;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.transfer.item.InOutInventoryStorage;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.AcceleratorScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.SoundContent;
import rearth.oritech.init.recipes.OritechRecipeInput;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.ScreenProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

// networking: last event could be automated. Inject event can just be called on server, and let vanilla handle sounds. Trail should be sent normally,
// so maybe everything could just be moved to manually sent packets
public class AcceleratorControllerBlockEntity extends BlockEntity implements BlockEntityTicker<AcceleratorControllerBlockEntity>, ItemProvider, MenuProvider, ScreenProvider {

    private AcceleratorParticleLogic.ActiveParticle particle;
    private AcceleratorParticleLogic.ActiveParticle lastParticle;
    public ItemStack activeItemParticle = ItemStack.EMPTY;

    private AcceleratorParticleLogic particleLogic;

    public final InOutInventoryStorage inventory = new InOutInventoryStorage(2, this::setChanged, new ContainerSlotAssignment(0, 1, 1, 1));

    // client data
    public List<Vec3> displayTrail;
    public LastEventPacket lastEvent = new LastEventPacket(worldPosition, ParticleEvent.IDLE, 0, worldPosition, 1, ItemStack.EMPTY);

    public AcceleratorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ACCELERATOR_CONTROLLER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AcceleratorControllerBlockEntity blockEntity) {
        if (level.isClientSide()) return;
        initParticleLogic();

        // try insert item as particle
        if (particle == null && !inventory.getResource(0).isEmpty() && inventory.getResource(1).isEmpty()) {
            injectParticle();
        }

        if (particle != null)
            particleLogic.update(particle);

    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input);
    }

    private void initParticleLogic() {
        if (particleLogic == null)
            particleLogic = new AcceleratorParticleLogic(worldPosition, (ServerLevel) level, this);
    }

    public void injectParticle() {

        var facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        var posBehind = Geometry.offsetToWorldPosition(facing, new Vec3i(1, 0, 0), worldPosition);
        var directionRight = Geometry.getRight(facing);

        var candidateBlock = level.getBlockState(new BlockPos(posBehind));
        if (candidateBlock.getBlock().equals(BlockContent.ACCELERATOR_RING.get())) {
            var startPosition = (BlockPos) posBehind;
            var nextGate = particleLogic.findNextGate(startPosition, directionRight, 1);
            particle = new AcceleratorParticleLogic.ActiveParticle(startPosition.getCenter(), 1, nextGate, startPosition);
            activeItemParticle = inventory.getStacks().getFirst().split(1);

            var soundPos = worldPosition.getCenter();
            level.playSound(null, soundPos.x, soundPos.y, soundPos.z, SoundEvents.BAMBOO_WOOD_TRAPDOOR_OPEN, SoundSource.BLOCKS);
            this.setChanged();
        }
    }

    public void removeParticleDueToCollision() {
        this.particle = null;
        this.activeItemParticle = ItemStack.EMPTY;
    }

    public void onParticleExited(Vec3 from, Vec3 to, BlockPos lastGate, Vec3 exitDirection, ParticleEvent reason) {

        var eventPosition = BlockPos.containing(particle.position);
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, ChunkPos.containing(worldPosition), new LastEventPacket(worldPosition, reason, particle.velocity, eventPosition, AcceleratorParticleLogic.getParticleBendDist(particle.lastBendDistance, particle.lastBendDistance2), activeItemParticle));

        this.lastParticle = particle;
        this.particle = null;

        var renderedTrail = List.of(from, to);
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, ChunkPos.containing(worldPosition), new ParticleRenderTrail(worldPosition, renderedTrail));

        this.setChanged();
    }

    public void onParticleCollided(float relativeSpeed, Vec3 collision, AcceleratorControllerBlockEntity secondControllerEntity) {

        // create end portal area when two ender pearls collide, nether portal for two firecharges
        if (relativeSpeed > OritechConfig.endPortalRequiredSpeed.get() && activeItemParticle.getItem().equals(Items.ENDER_PEARL) && secondControllerEntity.activeItemParticle.getItem().equals(Items.ENDER_PEARL)) {
            spawnEndPortal(BlockPos.containing(collision));
        } else if (relativeSpeed > OritechConfig.netherPortalRequiredSpeed.get() && activeItemParticle.getItem().equals(Items.FIRE_CHARGE) && secondControllerEntity.activeItemParticle.getItem().equals(Items.FIRE_CHARGE)) {
            spawnNetherPortal(BlockPos.containing(collision));
        } else {
            var success = tryCraftResult(relativeSpeed, activeItemParticle, secondControllerEntity.activeItemParticle);
        }

        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, ChunkPos.containing(worldPosition), new LastEventPacket(worldPosition, ParticleEvent.COLLIDED, relativeSpeed, BlockPos.containing(collision), AcceleratorParticleLogic.getParticleBendDist(particle.lastBendDistance, particle.lastBendDistance2), activeItemParticle));
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, ChunkPos.containing(secondControllerEntity.getBlockPos()), new LastEventPacket(secondControllerEntity.getBlockPos(), ParticleEvent.COLLIDED, relativeSpeed, BlockPos.containing(collision), AcceleratorParticleLogic.getParticleBendDist(particle.lastBendDistance, particle.lastBendDistance2), activeItemParticle));

        this.removeParticleDueToCollision();
        secondControllerEntity.removeParticleDueToCollision();

        var particleCount = Math.pow(relativeSpeed, 0.5) / 2f + 1;
        createCollisionParticles((int) relativeSpeed, collision, (int) particleCount);

        if (level instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.GUST, collision.x, collision.y, collision.z, 1, 0, 0, 0, 0);
        this.setChanged();
    }

    private void createCollisionParticles(int collisionEnergy, Vec3 collisionPosition, int shotCount) {

        var energyMultiplier = 4 * OritechConfig.tachyonCollisionEnergyFactor.get();
        int energyPotential = (int) (Math.pow(collisionEnergy / 2f, 2) * energyMultiplier * OritechConfig.accelerationRFCost.get());    // exactly N times the amount of energy used to accelerate
        var energyPerRay = energyPotential / shotCount;
        var rayRange = shotCount / 3;

        var caughtParticles = 0;

        for (int i = 0; i < shotCount; i++) {
            var r = level.getRandom();
            var offset = collisionPosition.add(r.nextFloat() * rayRange * 2 - rayRange, r.nextFloat() * rayRange * 2 - rayRange, r.nextFloat() * rayRange * 2 - rayRange);
            var direction = offset.subtract(collisionPosition).normalize();

            var impactPos = BlackHoleBlockEntity.basicRaycast(collisionPosition.add(direction.scale(1.2)), direction, rayRange, level);
            if (impactPos != null) {
                ParticleContent.BlackHoleEmission(level, collisionPosition, impactPos.getCenter());

                var candidate = level.getBlockEntity(impactPos);
                if (candidate instanceof ParticleCollectorBlockEntity collectorEntity) {
                    collectorEntity.onParticleCollided(energyPerRay);
                    caughtParticles++;
                }
            } else {
                ParticleContent.BlackHoleEmission(level, collisionPosition, offset);
            }

            // System.out.println("caught: " + caughtParticles + " of " + shotCount);
        }

    }

    private boolean tryCraftResult(float speed, ItemStack inputA, ItemStack inputB) {

        if (inputA == null || inputA.isEmpty() || inputB == null || inputB.isEmpty() || !(level instanceof ServerLevel serverLevel))
            return false;

        var inputInv = new OritechRecipeInput(List.of(inputA, inputB), FluidStack.EMPTY);
        var candidate = serverLevel.recipeAccess().getRecipeFor(RecipeContent.PARTICLE_COLLISION.get(), inputInv, level);

        // we only need to check once, as the recipe matches does fuzzy matching now (e.g. order doesnt matter)
        if (candidate.isEmpty()) return false;

        var recipe = candidate.get().value();

        var requiredSpeed = recipe.time();
        if (speed < requiredSpeed) return false;

        var result = recipe.itemResults();

        try (var transaction = Transaction.openRoot()) {
            var resultingStack = result.getFirst().create();
            var inserted = inventory.getOutputContainer().insert(ItemResource.of(resultingStack), resultingStack.count(), transaction);
            if (inserted == resultingStack.count()) {
                transaction.commit();
                return true;
            }
        }

        return false;
    }

    private void spawnEndPortal(BlockPos pos) {

        // create small end area around the portal
        for (var candidate : BlockPos.withinManhattan(pos, 8, 4, 8)) {

            var dist = candidate.getCenter().distanceTo(pos.getCenter());
            if (level.getRandom().nextFloat() < dist / 8) continue;

            var candidateState = level.getBlockState(candidate);
            if (candidateState.isAir() || candidateState.canBeReplaced() || candidateState.getBlock().defaultDestroyTime() < 0)
                continue;

            if (!level.getBlockState(candidate.below()).getBlock().equals(Blocks.CHORUS_PLANT))
                level.setBlockAndUpdate(candidate, Blocks.END_STONE.defaultBlockState());

            // generate chorus flowers
            if (level.getRandom().nextFloat() > 0.8) {
                var stateAbove = level.getBlockState(candidate.above());
                if (stateAbove.isAir() || stateAbove.canBeReplaced()) {
                    for (int i = 1; i < level.getRandom().nextIntBetweenInclusive(3, 6); i++) {
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
            if (level.getRandom().nextFloat() < dist / 12) continue;

            var candidateState = level.getBlockState(candidate);
            if (candidateState.isAir() || candidateState.canBeReplaced() || candidateState.getBlock().defaultDestroyTime() < 0)
                continue;

            level.setBlockAndUpdate(candidate, Blocks.NETHERRACK.defaultBlockState());

            // generate fires
            if (level.getRandom().nextFloat() > 0.8) {
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

        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, ChunkPos.containing(worldPosition), new ParticleRenderTrail(worldPosition, resultList));
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, ChunkPos.containing(worldPosition), new LastEventPacket(worldPosition, ParticleEvent.ACCELERATING, particle.velocity, BlockPos.containing(particle.position), AcceleratorParticleLogic.getParticleBendDist(particle.lastBendDistance, particle.lastBendDistance2), activeItemParticle));

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
        if (level instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.SONIC_BOOM, position.x, position.y, position.z, 1, 0.3, 0.3, 0.3, 0);

        return inflictedDamage;
    }

    public float handleParticleBlockCollision(BlockPos checkPos, AcceleratorParticleLogic.ActiveParticle particle, float remainingMomentum, BlockState hitState) {

        var blockHardness = hitState.getDestroySpeed(level, checkPos);

        // hit portal, create black hole with explosion
        if (remainingMomentum > OritechConfig.blackHoleRequiredSpeed.get() && hitState.getBlock() instanceof Portal) {
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
        if (level instanceof ServerLevel sl) {
            var c = checkPos.getCenter();
            sl.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 30, 1, 1, 1, 0);
        }

        var center = checkPos.getCenter();
        level.explode(null, center.x, center.y, center.z, 10, false, Level.ExplosionInteraction.BLOCK);

        level.removeBlock(checkPos, false);
        level.setBlockAndUpdate(checkPos, BlockContent.BLACK_HOLE_BLOCK.get().defaultBlockState());
    }

    public void handleParticleMotorInteraction(BlockPos motorBlock) {

        var entity = level.getBlockEntity(motorBlock);
        if (!(entity instanceof AcceleratorMotorBlockEntity motorEntity)) return;

        var storage = motorEntity.getEnergyLookup(null);
        var availableEnergy = storage.getAmountAsLong();

        var speed = particle.velocity;
        var cost = speed * OritechConfig.accelerationRFCost.get();
        if (availableEnergy < cost) return;

        try (var transaction = Transaction.openRoot()) {
            var extracted = storage.extract((int) cost, transaction);
            if (extracted <= 0) return;
            transaction.commit();
        }

        particle.velocity += 1;

    }

    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
        return inventory;
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
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
    public StacksResourceHandler<ItemStack, ItemResource> getDisplayedInventory() {
        return inventory;
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.ACCELERATOR_SCREEN.get();
    }

    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }

    @Override
    public boolean showProgress() {
        return false;
    }

    public static void receiveTrail(ParticleRenderTrail packet, IPayloadContext context) {
        Level level = context.player().level();
        if (level.getBlockEntity(packet.position) instanceof AcceleratorControllerBlockEntity acceleratorBlock) {
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
            level.playLocalSound(soundPos.x, soundPos.y, soundPos.z, SoundContent.PARTICLE_MOVING.value(), SoundSource.BLOCKS, 2f, (float) pitch, true);

        }
    }

    public static void receiveEvent(LastEventPacket packet, IPayloadContext context) {
        Level level = context.player().level();
        if (level.getBlockEntity(packet.position) instanceof AcceleratorControllerBlockEntity acceleratorBlock) {
            acceleratorBlock.lastEvent = packet;

            var soundPos = packet.lastEventPosition.getCenter();
            if (packet.lastEvent.equals(ParticleEvent.COLLIDED)) {
                level.playLocalSound(soundPos.x, soundPos.y, soundPos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 5f, 1, true);
            } else if (packet.lastEvent.equals(ParticleEvent.EXITED_FAST) || packet.lastEvent.equals(ParticleEvent.EXITED_NO_GATE)) {
                level.playLocalSound(soundPos.x, soundPos.y, soundPos.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.BLOCKS, 3f, 1, true);
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

        public static final Type<LastEventPacket> PACKET_ID = new Type<>(Oritech.id("accel_event"));

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

        public static final Type<ParticleRenderTrail> PACKET_ID = new Type<>(Oritech.id("accel_render"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
