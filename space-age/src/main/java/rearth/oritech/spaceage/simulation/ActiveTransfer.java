package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.MissionState.Telemetry;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.FlightPath;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** A transfer's frozen inputs and clock survive reloads; compiled guidance is rebuilt lazily. */
final class ActiveTransfer {

    static final Codec<ActiveTransfer> CODEC = RecordCodecBuilder.create(i -> i.group(
            Telemetry.CODEC.fieldOf("start").forGetter(t -> t.start),
            SpaceObjectData.CODEC.listOf().fieldOf("objects").forGetter(t -> t.objects),
            DockingTarget.CODEC.listOf().fieldOf("stations").forGetter(t -> t.stations),
            Codec.LONG.fieldOf("ticks").forGetter(t -> t.ticks),
            BlockPos.CODEC.optionalFieldOf("landing").forGetter(t -> Optional.ofNullable(t.landing))
    ).apply(i, ActiveTransfer::new));
    final Telemetry start;
    final List<SpaceObjectData> objects;
    final List<DockingTarget> stations;
    long ticks;
    BlockPos landing;
    FlightPath path;
    private FlightPlan forecastPlan;
    private Collection<RecipeHolder<VacuumRecipe>> forecastRecipes;
    private MissionForecast.Navigation forecast = MissionForecast.Navigation.EMPTY;

    ActiveTransfer(Telemetry start, List<SpaceObjectData> objects, List<DockingTarget> stations) {

        this(start, objects, stations, 0, Optional.empty());
    }

    private ActiveTransfer(Telemetry start, List<SpaceObjectData> objects, List<DockingTarget> stations,
                           long ticks, Optional<BlockPos> landing) {

        this.start = start;
        this.objects = List.copyOf(objects);
        this.stations = List.copyOf(stations);
        this.ticks = ticks;
        this.landing = landing.orElse(null);
    }

    MissionForecast.Navigation forecast(FlightPlan plan, Collection<RecipeHolder<VacuumRecipe>> recipes) {

        if (!plan.equals(forecastPlan) || forecastRecipes != recipes) {
            var flight = RocketFlightPathCalculator.calculateFrom(start.rocket(), objects, plan, start.position(), recipes, stations);
            forecast = MissionForecast.Navigation.forClient(flight);
            forecastPlan = plan;
            forecastRecipes = recipes;
        }
        return forecast;
    }
}
