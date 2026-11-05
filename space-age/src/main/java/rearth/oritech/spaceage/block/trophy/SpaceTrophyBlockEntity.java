package rearth.oritech.spaceage.block.trophy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;
import java.util.List;

/**
 * The inscription travels with the trophy through item components and block entity saves.
 */
public class SpaceTrophyBlockEntity extends BlockEntity implements GeoBlockEntity, MultiblockMachineController {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ArrayList<BlockPos> cores = new ArrayList<>();
    private @Nullable TrophyInscription data;

    public SpaceTrophyBlockEntity(BlockPos pos, BlockState state) {

        super(SpaceAgeBlockEntities.SPACE_TROPHY.get(), pos, state);
    }

    public @Nullable TrophyInscription data() {

        return data;
    }

    public void setData(TrophyInscription data) {

        this.data = data;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);
        serializeMultiblock(output);
        if (data != null) output.store("inscription", TrophyInscription.CODEC, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        deserializeMultiblock(input);
        data = input.read("inscription", TrophyInscription.CODEC).orElse(null);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {

        super.applyImplicitComponents(components);
        data = components.get(SpaceAgeComponents.TROPHY_INSCRIPTION.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {

        super.collectImplicitComponents(components);
        if (data == null) return;
        components.set(SpaceAgeComponents.TROPHY_INSCRIPTION.get(), data);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {

        super.removeComponentsFromTag(output);
        output.discard("inscription");
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {

        return saveWithoutMetadata(registries);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {

        return cache;
    }

    @Override
    public List<Vec3i> getCorePositions() {

        return List.of(new Vec3i(0, 1, 0));
    }

    @Override
    public Direction getFacingForMultiblock() {

        return getBlockState().getValue(SpaceTrophyBlock.FACING);
    }

    @Override
    public BlockPos getPosForMultiblock() {

        return worldPosition;
    }

    @Override
    public Level getWorldForMultiblock() {

        return level;
    }

    @Override
    public ArrayList<BlockPos> getConnectedCores() {

        return cores;
    }

    @Override
    public void setCoreQuality(float quality) {
    }

    @Override
    public float getCoreQuality() {

        return 1;
    }

    @Override
    public StacksResourceHandler<ItemStack, ItemResource> getInventoryForMultiblock() {

        return null;
    }

    @Override
    public DynamicEnergyStorage getEnergyStorageForMultiblock(Direction direction) {

        return null;
    }

    @Override
    public void triggerSetupAnimation() {
    }

    @Override
    public void onCoreBroken(BlockPos corePos) {

        // The automatically placed cores belong to the trophy, rather than leaving loose plating behind.
        cores.remove(corePos);
        level.destroyBlock(worldPosition, true);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {

        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide()) return;
        var connected = List.copyOf(cores);
        onControllerBroken();
        for (var core : connected) level.removeBlock(core, false);
    }
}
