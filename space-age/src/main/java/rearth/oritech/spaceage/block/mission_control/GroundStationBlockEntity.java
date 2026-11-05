package rearth.oritech.spaceage.block.mission_control;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulationSavedData;
import rearth.oritech.spaceage.simulation.mission.MissionController;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Mission Control detects passive antennas; ground scanners draw RF from adjacent batteries.
 */
public class GroundStationBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<BlockPos> antennas = new ArrayList<>();
    public UUID owner = new UUID(0, 0);
    public int connectedAntennas;

    public GroundStationBlockEntity(BlockPos pos, BlockState state) {

        super(SpaceAgeBlockEntities.GROUND_STATION.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        if (getBlockState().is(SpaceAgeBlocks.MISSION_CONTROL))
            controllers.add(new AnimationController<>("idle", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {

        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);
        output.store("owner", UUIDUtil.CODEC, owner);
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(new UUID(0, 0));
    }

    public void tick() {

        if (!(level instanceof ServerLevel server) || level.dimension() != Level.OVERWORLD)
            return;
        if (getBlockState().is(SpaceAgeBlocks.SPACE_SCANNER)) {
            var data = SpaceSimulationSavedData.get(server.getServer());
            var system = data.getOrCreate(owner);
            var undiscovered = system.truth().stream().anyMatch(object -> object.type() == SpaceObjects.ObjectType.ASTEROID
                    && Math.hypot(object.x() - SpaceObjects.EARTH_SURFACE_X, object.y()) <= 180_000
                    && system.earthKnowledge.contact(object.id()) == null);
            if (undiscovered && draw(worldPosition, SpaceBalance.SCAN_RF)) {
                system.earthKnowledge.scan(system.truth(), MissionController.missionTime(level.getServer()), SpaceObjects.EARTH_SURFACE_X, 0, 180_000);
                data.setDirty();
            }
        }
        if (!getBlockState().is(SpaceAgeBlocks.MISSION_CONTROL)) return;
        if (level.getGameTime() % 20 == 0) findAntennas();
        connectedAntennas = 0;
        for (var pos : antennas) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(SpaceAgeBlocks.ANTENNA)) connectedAntennas++;
        }
        MissionSavedData.get(server.getServer()).stations.put(GlobalPos.of(level.dimension(), worldPosition), this);
    }

    private void findAntennas() {

        antennas.clear();
        var open = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        open.add(worldPosition);
        while (!open.isEmpty() && visited.size() < 256) {
            var pos = open.removeFirst();
            if (!visited.add(pos) || !level.hasChunkAt(pos)) continue;
            if (!pos.equals(worldPosition) && !level.getBlockState(pos).is(SpaceAgeBlocks.ANTENNA)) continue;
            if (!pos.equals(worldPosition)) antennas.add(pos);
            for (var direction : Direction.values()) open.add(pos.relative(direction));
        }
    }

    private boolean draw(BlockPos pos, long amount) {

        for (var direction : Direction.values()) {
            var other = pos.relative(direction);
            if (!level.hasChunkAt(other)) continue;
            var storage = level.getCapability(Capabilities.Energy.BLOCK, other, null);
            if (storage == null) continue;
            try (var transaction = Transaction.openRoot()) {
                if (storage.extract((int) amount, transaction) == amount) {
                    transaction.commit();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Component getDisplayName() {

        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {

        return new MissionControlMenu(id, inventory, worldPosition);
    }
}
