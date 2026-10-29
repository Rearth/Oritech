package rearth.oritech.spaceage.block.launch_ramp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.spaceage.block.cubesat.CubeSatBlock;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulationSavedData;
import rearth.oritech.spaceage.simulation.flight.LaunchSpeedRules;
import rearth.oritech.spaceage.simulation.mission.MissionController;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.mission.RocketDocking;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.CubeSatData;
import rearth.oritech.spaceage.simulation.rocket.RocketRecovery;
import rearth.oritech.spaceage.simulation.rocket.RocketSimulationController;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Shoots cube-sats arriving as accelerator particles into space. The plan belongs to the ramp: it is stored in the
 * simulation of the last player who saved one, keyed by the ramp's position, and applies to every cube with the same
 * layout as the pattern it was made for.
 */
public class LaunchRampBlockEntity extends BlockEntity implements MenuProvider {

    // Distance in particle direction at which too slow cubes come down again.
    private static final int FALLBACK_DISTANCE = 4;

    // The cube-sat design the plan is made for. It is never launched itself.
    public final SimpleContainer pattern = new SimpleContainer(1) {

        @Override
        public void setChanged() {

            super.setChanged();
            LaunchRampBlockEntity.this.setChanged();
        }
    };
    private @Nullable UUID owner;
    private String ownerName = "";
    private long planFingerprint;
    private long lastSpeed;
    private Outcome lastOutcome = Outcome.NONE;

    public LaunchRampBlockEntity(BlockPos pos, BlockState state) {

        super(SpaceAgeBlockEntities.LAUNCH_RAMP.get(), pos, state);
    }

    private static String name(CubeSatData cube, SpaceSimulation.FlightPlan plan) {

        if (!cube.name().isBlank()) return cube.name();
        return plan.name().isBlank() ? "Cube-Sat" : plan.name();
    }

    public long lastSpeed() {

        return lastSpeed;
    }

    public Outcome lastOutcome() {

        return lastOutcome;
    }

    public String ownerName() {

        return ownerName;
    }

    /**
     * Whether the saved plan was made for the current pattern's layout.
     */
    public boolean planForPattern() {

        var data = CubeSatBlock.data(pattern.getItem(0));
        return owner != null && data != null && data.fingerprint() == planFingerprint;
    }

    /**
     * Particles that aren't cube-sats are stopped and lost.
     */
    public void absorb(long particleSpeed) {

        record(particleSpeed, Outcome.LOST);
    }

    private void record(long particleSpeed, Outcome outcome) {

        lastSpeed = particleSpeed;
        lastOutcome = outcome;
        setChanged();
    }

    public BlockPos launchPosition() {

        return worldPosition.above();
    }

    /**
     * The pattern's rocket as previewed in the planner. Its ID only depends on the layout.
     */
    public @Nullable ActiveRocketData createPreview() {

        var data = CubeSatBlock.data(pattern.getItem(0));
        return data == null ? null : unfold(data, UUID.nameUUIDFromBytes(Long.toString(data.fingerprint()).getBytes(StandardCharsets.UTF_8)));
    }

    private ActiveRocketData unfold(CubeSatData data, UUID id) {

        var rocket = data.unfold(id, null);
        rocket.setLaunchPosition(launchPosition());
        rocket.setLaunchPadCenter(launchPosition());
        return rocket;
    }

    /**
     * Saving a plan makes the player the owner, since the plan's targets come from their solar system.
     */
    public void savePlan(ServerPlayer player, UUID previewId, SpaceSimulation.FlightPlan plan) {

        var data = CubeSatBlock.data(pattern.getItem(0));
        var rocket = createPreview();
        if (data == null || rocket == null) return;
        if (!rocket.getRocketId().equals(previewId)) {
            player.sendOverlayMessage(Component.translatable("message.oritech_space_age.launch_ramp.pattern_changed"));
            return;
        }
        SpaceSimulationSavedData.updateFlightPlan(player, GlobalPos.of(player.level().dimension(), worldPosition), plan, rocket);
        owner = player.getUUID();
        ownerName = player.getName().getString();
        planFingerprint = data.fingerprint();
        setChanged();
    }

    /**
     * Every arriving cube is launched. A matching cube follows the stored plan, starting with the particle's speed.
     * Without a matching plan, or if its first leg can't be flown at this speed, the cube coasts.
     */
    public void launch(CubeSatData cube, long particleSpeed, Vec3 direction) {

        if (!(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != Level.OVERWORLD) {
            Containers.dropItemStack(serverLevel, worldPosition.getX() + .5, worldPosition.getY() + 1, worldPosition.getZ() + .5, CubeSatBlock.createStack(cube));
            record(particleSpeed, Outcome.DROPPED);
            return;
        }
        var server = serverLevel.getServer();
        var launcher = owner != null ? owner : Optional.ofNullable(serverLevel.getNearestPlayer(worldPosition.getX(), worldPosition.getY(),
                worldPosition.getZ(), -1, false)).map(Player::getUUID).orElse(null);
        if (launcher == null) {
            Containers.dropItemStack(serverLevel, worldPosition.getX() + .5, worldPosition.getY() + 1, worldPosition.getZ() + .5, CubeSatBlock.createStack(cube));
            record(particleSpeed, Outcome.DROPPED);
            return;
        }
        var rocket = unfold(cube, UUID.randomUUID());
        var velocity = LaunchSpeedRules.velocity(particleSpeed);
        var system = SpaceSimulationSavedData.get(server).getOrCreate(launcher);
        var plan = owner == null || cube.fingerprint() != planFingerprint ? SpaceSimulation.FlightPlan.empty()
                : system.flightPlan(GlobalPos.of(serverLevel.dimension(), worldPosition));
        if (firstLegFlies(rocket, plan, system, launcher, particleSpeed, serverLevel)) {
            MissionController.launch(serverLevel, launcher, rocket, plan.withName(name(cube, plan)), launchPosition(), velocity);
            record(particleSpeed, Outcome.PLAN);
        } else record(particleSpeed, coast(serverLevel, launcher, rocket, cube, velocity, direction));
    }

    private boolean firstLegFlies(ActiveRocketData rocket, SpaceSimulation.FlightPlan plan, SpaceSimulation system, UUID launcher,
                                  long particleSpeed, ServerLevel level) {

        var objects = system.createObjectData();
        var stations = RocketDocking.targets(MissionSavedData.get(level.getServer()), launcher);
        return LaunchSpeedRules.valid(plan, rocket, objects, particleSpeed, VacuumRecipe.all(level.getServer()), stations);
    }

    /**
     * Without a usable plan, a fast enough cube becomes a stopped craft in low Earth orbit. Slower cubes fall back
     * and land as blocks next to the ramp.
     */
    private Outcome coast(ServerLevel level, UUID launcher, ActiveRocketData rocket, CubeSatData cube, double velocity, Vec3 direction) {

        if (velocity < LaunchSpeedRules.orbitVelocity()) {
            var x = worldPosition.getX() + (int) Math.round(direction.x * FALLBACK_DISTANCE);
            var z = worldPosition.getZ() + (int) Math.round(direction.z * FALLBACK_DISTANCE);
            if (RocketRecovery.restore(level, rocket, RocketRecovery.landingPosition(level, rocket, x, z)))
                return Outcome.FELL_BACK;
            Containers.dropItemStack(level, x + .5, worldPosition.getY() + 1, z + .5, CubeSatBlock.createStack(cube));
            return Outcome.DROPPED;
        }
        var system = SpaceSimulationSavedData.get(level.getServer()).getOrCreate(launcher);
        var earth = system.createObjectData().stream().filter(object -> object.id().equals(SpaceObjects.EARTH_ID)).findFirst().orElseThrow();
        var angle = SpaceBalance.surfaceAngle(worldPosition.getX(), worldPosition.getZ());
        var distance = earth.radius() + SpaceSimulation.OrbitBand.LOW.altitude();
        var orbit = new MissionState.Position(earth.x() + Math.cos(angle) * distance, earth.y() + Math.sin(angle) * distance, 0, 0,
                SpaceObjects.EARTH_ID, SpaceSimulation.OrbitBand.LOW, -1, 1, SpaceSimulation.FlightPlanAction.NO_TARGET,
                new SpaceSimulation.SegmentRef(BlockPos.ZERO));
        rocket.setLaunchPosition(launchPosition());
        var mission = new MissionState(launcher, rocket, SpaceSimulation.FlightPlan.empty().withName(name(cube, SpaceSimulation.FlightPlan.empty())),
                orbit, system.earthKnowledge.copy(), launchPosition());
        var data = MissionSavedData.get(level.getServer());
        data.craft.put(rocket.getRocketId(), mission);
        data.setDirty();
        RocketSimulationController.launchMissionVisual(level, rocket, mission.plan, launchPosition(), velocity);
        return Outcome.ORBIT;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {

        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, pattern);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);
        if (!pattern.isEmpty()) output.store("pattern", ItemStack.CODEC, pattern.getItem(0));
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
        output.putString("owner_name", ownerName);
        output.putLong("plan_fingerprint", planFingerprint);
        output.putLong("last_speed", lastSpeed);
        output.putString("last_outcome", lastOutcome.name());
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        pattern.setItem(0, input.read("pattern", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr("owner_name", "");
        planFingerprint = input.getLongOr("plan_fingerprint", 0);
        lastSpeed = input.getLongOr("last_speed", 0);
        lastOutcome = Outcome.valueOf(input.getStringOr("last_outcome", Outcome.NONE.name()));
    }

    @Override
    public Component getDisplayName() {

        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {

        return new LaunchRampMenu(id, inventory, this);
    }

    /**
     * What happened to the last particle that hit the ramp.
     */
    public enum Outcome {

        NONE,
        PLAN,
        ORBIT,
        FELL_BACK,
        DROPPED,
        LOST
    }
}
