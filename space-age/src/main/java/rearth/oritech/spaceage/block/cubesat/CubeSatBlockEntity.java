package rearth.oritech.spaceage.block.cubesat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.spaceage.simulation.rocket.CubeSatData;

/**
 * Holds the compacted rocket of a placed cube-sat. The item name (e.g. from an anvil) is the craft name.
 */
public class CubeSatBlockEntity extends BlockEntity {

    private @Nullable CubeSatData data;

    public CubeSatBlockEntity(BlockPos pos, BlockState state) {

        super(SpaceAgeBlockEntities.CUBE_SAT.get(), pos, state);
    }

    public @Nullable CubeSatData data() {

        return data;
    }

    public void setData(CubeSatData data) {

        this.data = data;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);
        if (data != null) output.store("cube", CubeSatData.CODEC, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        data = input.read("cube", CubeSatData.CODEC).orElse(null);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {

        super.applyImplicitComponents(components);
        var stored = components.get(SpaceAgeComponents.CUBE_SAT.get());
        var name = components.get(DataComponents.CUSTOM_NAME);
        data = stored == null ? null : stored.withName(name == null ? "" : name.getString());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {

        super.collectImplicitComponents(components);
        if (data == null) return;
        components.set(SpaceAgeComponents.CUBE_SAT.get(), data);
        if (!data.name().isBlank()) components.set(DataComponents.CUSTOM_NAME, Component.literal(data.name()));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {

        super.removeComponentsFromTag(output);
        output.discard("cube");
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {

        return saveWithoutMetadata(registries);
    }
}
