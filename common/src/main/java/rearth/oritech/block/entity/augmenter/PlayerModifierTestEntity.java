package rearth.oritech.block.entity.augmenter;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.blocks.augmenter.AugmenterResearchStationBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.PlayerModifierScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.AutoPlayingSoundKeyframeHandler;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventoryProvider;
import rearth.oritech.util.MultiblockMachineController;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.containers.SimpleEnergyStorage;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerModifierTestEntity extends BlockEntity implements BlockEntityTicker<PlayerModifierTestEntity>, MultiblockMachineController, GeoBlockEntity, ExtendedScreenHandlerFactory, EnergyApi.BlockProvider {
    
    public final Set<Identifier> researchedAugments = new HashSet<>();
    
    // config
    public static long maxEnergyTransfer = 50_000;
    public static long maxEnergyStored = 5_000_000;
    public static long energyUsageRate = 2048;
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;
    
    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    // working state
    private boolean networkDirty = true;
    private final List<Block> availableStations = new ArrayList<>();
    
    private final EnergyApi.EnergyContainer energyStorage = new SimpleEnergyStorage(maxEnergyTransfer, 0, maxEnergyStored, this::markDirty);
    
    
    public PlayerModifierTestEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PLAYER_MODIFIER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, PlayerModifierTestEntity blockEntity) {
        
        if (world.isClient) return;
        
        if (networkDirty) {
            updateNetwork();
        }
    }
    
    public void researchAugment(Identifier augment) {
        System.out.println("researching augment: " + augment);
        
        if (!PlayerAugments.allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        if (researchedAugments.contains(augment)) {
            Oritech.LOGGER.warn("Player tried to research already researched augment " + augment);
            return;
        }
        
        researchedAugments.add(augment);
        this.markNetDirty();
    }
    
    public void installAugmentToPlayer(Identifier augment, PlayerEntity player) {
        System.out.println("adding augment: " + augment);
        
        if (!PlayerAugments.allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        if (!researchedAugments.contains(augment)) {
            Oritech.LOGGER.warn("Player tried to install augment with id" + augment + " without researching it.");
            return;
        }
        
        var augmentInstance = PlayerAugments.allAugments.get(augment);
        augmentInstance.installToPlayer(player);
        this.markNetDirty();
    }
    
    public void removeAugmentFromPlayer(Identifier augment, PlayerEntity player) {
        System.out.println("removing augment: " + augment);
        
        if (!PlayerAugments.allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        var augmentInstance = PlayerAugments.allAugments.get(augment);
        augmentInstance.removeFromPlayer(player);
        this.markNetDirty();
    }
    
    public static void toggleAugmentForPlayer(Identifier augment, PlayerEntity player) {
        System.out.println("toggling augment: " + augment);
        
        if (!PlayerAugments.allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        var augmentInstance = PlayerAugments.allAugments.get(augment);
        
        if (!augmentInstance.isInstalled(player)) {
            Oritech.LOGGER.error("Tried toggling not-installed augment id: " + augment + ". This should never happen");
            return;
        }
        
        augmentInstance.toggle(player);
    }
    
    public boolean hasPlayerAugment(Identifier augment, PlayerEntity player) {
        
        if (!PlayerAugments.allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return false;
        }
        
        var augmentInstance = PlayerAugments.allAugments.get(augment);
        return augmentInstance.isInstalled(player);
        
    }
    
    public void loadResearchesFromPlayer(PlayerEntity player) {
        
        for (var augmentId : PlayerAugments.allAugments.keySet()) {
            var augment = PlayerAugments.allAugments.get(augmentId);
            var isInstalled = augment.isInstalled(player);
            var isResearched = researchedAugments.contains(augmentId);
            
            if (isInstalled && !isResearched) {
                researchedAugments.add(augmentId);
            }
        }
    }
    
    // todo remove this debug
    public void onUse(PlayerEntity player) {
        
        if (player.isSneaking()) {
            System.out.println("resetting all augments!");
            
            for (var augmentId : PlayerAugments.allAugments.keySet()) {
                var isInstalled = hasPlayerAugment(augmentId, player);
                if (isInstalled)
                    removeAugmentFromPlayer(augmentId, player);
            }
            
        }
        
    }
    
    public void loadAvailableStations(PlayerEntity player) {
        var facing = this.getCachedState().get(Properties.HORIZONTAL_FACING);
        
        var targetPositions = List.of(
          new BlockPos(0, 0, -2),
          new BlockPos(1, 0, 2),
          new BlockPos(2, 0, -1)
        );
        
        for (var candidatePosOffset : targetPositions) {
            var candidatePos = new BlockPos(Geometry.offsetToWorldPosition(facing, candidatePosOffset, pos));
            
            var candidateState = world.getBlockState(candidatePos);
            if (!(candidateState.getBlock() instanceof AugmenterResearchStationBlock)) continue;
            if (!candidateState.get(MultiblockMachine.ASSEMBLED)) continue;
            availableStations.add(candidateState.getBlock());
        }
        
    }
    
    private void markNetDirty() {
        this.networkDirty = true;
    }
    
    private void updateNetwork() {
        this.networkDirty = false;
        
        // collect researched augments, send them to client
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.AugmentResearchList(pos, researchedAugments.stream().toList()));
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.GenericEnergySyncPacket(pos, energyStorage.getAmount(), energyStorage.getCapacity()));
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 0, 1),
          new Vec3i(0, 0, -1),
          new Vec3i(-1, 0, 0),
          new Vec3i(-1, 0, 1),
          new Vec3i(-1, 0, -1),
          new Vec3i(0, 1, 1),
          new Vec3i(0, 1, -1),
          new Vec3i(-1, 1, 0),
          new Vec3i(-1, 1, 1),
          new Vec3i(-1, 1, -1)
        );
    }
    
    @Override
    public Direction getFacingForMultiblock() {
        var state = getCachedState();
        return state.get(Properties.HORIZONTAL_FACING).getOpposite();
    }
    
    @Override
    public BlockPos getMachinePos() {
        return pos;
    }
    
    @Override
    public World getMachineWorld() {
        return world;
    }
    
    @Override
    public ArrayList<BlockPos> getConnectedCores() {
        return coreBlocksConnected;
    }
    
    @Override
    public void setCoreQuality(float quality) {
        this.coreQuality = quality;
    }
    
    @Override
    public float getCoreQuality() {
        return coreQuality;
    }
    
    @Override
    public InventoryProvider getInventoryForLink() {
        return null;
    }
    
    @Override
    public EnergyApi.EnergyContainer getEnergyStorageForLink() {
        return energyStorage;
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        this.markNetDirty();
    }
    
    @Override
    public void playSetupAnimation() {
    
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "machine", 5, state -> PlayState.CONTINUE)
                          .setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        return new ModScreens.BasicData(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.empty();
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        updateNetwork();
        return new PlayerModifierScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public EnergyApi.EnergyContainer getStorage(Direction direction) {
        return energyStorage;
    }
}
