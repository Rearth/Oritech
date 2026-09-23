package rearth.oritech.spaceage.simulation;

/** Adjusts preview service budgets using current mission progress. */
public final class MissionForecast {
    /** The current navigation and its successors, anchored to the authoritative transfer start. */
    public record Navigation(RocketFlightPathCalculator.FlightPath flight) {
        public static final Navigation EMPTY = new Navigation(new RocketFlightPathCalculator.FlightPath(java.util.List.of(), java.util.List.of(), 0));
        public java.util.List<RocketFlightPathCalculator.CraftPath> paths() { return flight.paths(); }
        public static Navigation forClient(RocketFlightPathCalculator.FlightPath flight) {
            // Trajectory telemetry must not transmit composition learned only by onboard scanners.
            var arrivals = flight.arrivalPredictions().stream().map(a -> new RocketFlightPathCalculator.ArrivalPrediction(
                    a.actionId(), a.branchId(), a.targetId(), withoutMaterials(a.impact()))).toList();
            var asteroids = flight.asteroidPaths().stream().map(a -> new RocketFlightPathCalculator.AsteroidPath(
                    a.asteroidId(), a.samples(), a.earthImpact() == null ? null : withoutMaterials(a.earthImpact()),
                    a.landingUncertaintyBlocks())).toList();
            return new Navigation(new RocketFlightPathCalculator.FlightPath(flight.paths(), flight.boosterEvents(),
                    flight.lastCommandSeconds(), arrivals, asteroids, flight.navigationAborts(), flight.scanEstimates(), flight.stationKeepingEstimates()));
        }
        private static AsteroidImpactRules.ImpactPrediction withoutMaterials(AsteroidImpactRules.ImpactPrediction impact) {
            var fragments = impact.fragments().stream().map(f -> new AsteroidImpactRules.AsteroidFragment(
                    f.mass(), f.radius(), java.util.List.of())).toList();
            return new AsteroidImpactRules.ImpactPrediction(impact.outcome(), impact.relativeSpeedMetersPerSecond(),
                    impact.kineticEnergyJoules(), impact.craterRadiusBlocks(), impact.fragmentationMode(), fragments,
                    impact.remainingTargetMass(), java.util.List.of(), impact.landingX(), impact.landingZ());
        }
    }

    /** Timed service forecasts start with the time remaining at the last report. */
    public static SpaceSimulation.FlightPlan remainingServices(SpaceSimulation.FlightPlan plan, long actionTicks) {
        var current = plan.root().actions().stream().filter(a -> !a.isGenerated()).findFirst().orElse(null);
        if (current == null || actionTicks == 0) return plan;
        var settings = current.service();
        SpaceSimulation.FlightPlanAction remaining;
        if (current.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION && settings.timeoutTicks() > 0) {
            remaining = current.withService(new SpaceSimulation.ServiceSettings(settings.durationTicks(),
                    (int) Math.max(1, settings.timeoutTicks() - actionTicks), settings.slot()));
        } else if (current.type() == SpaceSimulation.ActionType.RELAY) {
            remaining = current.withService(new SpaceSimulation.ServiceSettings(
                    (int) Math.max(1, settings.durationTicks() - actionTicks), settings.timeoutTicks(), settings.slot()));
        } else return plan;
        return plan.withBranches(plan.branches().stream().map(branch -> branch.isRoot() ? branch.withActions(
                branch.actions().stream().map(action -> action.id().equals(current.id()) ? remaining : action).toList()) : branch).toList());
    }

    private MissionForecast() { }
}
