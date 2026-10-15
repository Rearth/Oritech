package rearth.oritech.spaceage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.simulation.BeamAperture;
import rearth.oritech.spaceage.simulation.BeamReception;
import rearth.oritech.spaceage.simulation.SurfaceBeamData;

import java.util.Optional;

public class BeamApertureBlockEntity extends BlockEntity implements MenuProvider {

    public static final long CAPACITY = 20_000_000;
    public static final int MAX_OUTPUT = 1_000_000;

    public final DynamicEnergyStorage energy = new DynamicEnergyStorage(CAPACITY, 0, MAX_OUTPUT, 0, this::setChanged, false);
    private BeamAperture.Array array;
    private long received;
    private long exported;
    private double receiveRate;
    private double exportRate;
    private int rateTicks;
    private BeamReception intervalReception = BeamReception.EMPTY;
    private BeamReception receptionRate = BeamReception.EMPTY;

    public BeamApertureBlockEntity(BlockPos pos, BlockState state) {

        super(SpaceAgeBlockEntities.BEAM_APERTURE.get(), pos, state);
    }

    public BeamAperture.Array array() {

        if (array == null) refresh();
        return array;
    }

    private void refresh() {

        array = BeamAperture.scan(worldPosition, getBlockState().getValue(BlockStateProperties.FACING),
                pos -> level != null && level.hasChunkAt(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState());
    }

    public void tick() {

        if (!(level instanceof ServerLevel server) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 20 == 0 || array == null) refresh();
        SurfaceBeamData.get(server).receivers.put(worldPosition, this);
        if (++rateTicks == 20) {
            receiveRate = received / 20d;
            receptionRate = intervalReception.scaled(1 / 20d);
            intervalReception = BeamReception.EMPTY;
            exportRate = exported / 20d;
            received = 0;
            exported = 0;
            rateTicks = 0;
        }
        var remaining = (int) Math.min(energy.energy, energy.maxExtract);
        for (var direction : Direction.values()) {
            var pos = worldPosition.relative(direction);
            if (!level.hasChunkAt(pos)) continue;
            var target = level.getCapability(Capabilities.Energy.BLOCK, pos, null);
            if (target == null || target == energy) continue;
            try (var transaction = Transaction.openRoot()) {
                var inserted = target.insert(remaining, transaction);
                energy.extract(inserted, transaction);
                transaction.commit();
                remaining -= inserted;
                exported += inserted;
            }
        }
    }

    public void recordBeam(BeamReception reception) {

        intervalReception = intervalReception.plus(reception);
    }

    public double receive(double amount) {

        // Analytic event times can put an integral RF pulse just below its integer boundary.
        var accepted = (long) Math.min(Math.floor(amount + 1e-6), energy.capacity - energy.energy);
        energy.energy += accepted;
        received += accepted;
        if (accepted > 0) setChanged();
        return accepted;
    }

    public BeamApertureMenu.Snapshot snapshot() {

        refresh();
        var facing = getBlockState().getValue(BlockStateProperties.FACING);
        var blocks = BeamAperture.preview(worldPosition, facing,
                pos -> level.hasChunkAt(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState());
        var heat = level instanceof ServerLevel server && level.dimension() == Level.OVERWORLD
                ? SurfaceBeamData.get(server).temperature(worldPosition) : 0;
        return new BeamApertureMenu.Snapshot(blocks, array.width(), array.height(), array.area(), array.quality(),
                Optional.ofNullable(array.defect()), energy.energy, receiveRate, exportRate, heat,
                level.dimension() == Level.OVERWORLD, facing == Direction.UP, receptionRate);
    }

    @Override
    public Component getDisplayName() {

        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {

        return new BeamApertureMenu(id, inventory, worldPosition, snapshot());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);
        output.putLong("energy", energy.energy);
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        energy.energy = input.getLongOr("energy", 0);
    }
}
