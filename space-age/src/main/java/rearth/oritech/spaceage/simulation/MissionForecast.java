package rearth.oritech.spaceage.simulation;

/** Adjusts preview service budgets using current mission progress. */
public final class MissionForecast {
    /** Timed service forecasts start with the time remaining at the last report. */
    public static SpaceSimulation.FlightPlan remainingServices(SpaceSimulation.FlightPlan plan, long actionTicks) {
        var current = plan.root().actions().stream().filter(a -> !a.isGenerated()).findFirst().orElse(null);
        if (current == null || actionTicks == 0) return plan;
        var settings = current.service();
        SpaceSimulation.FlightPlanAction remaining;
        if (current.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION && settings.timeoutTicks() > 0) {
            remaining = current.withService(new SpaceSimulation.ServiceSettings(settings.durationTicks(), settings.untilPrecise(),
                    (int) Math.max(1, settings.timeoutTicks() - actionTicks), settings.slot()));
        } else if (current.type() == SpaceSimulation.ActionType.RELAY
                || current.type() == SpaceSimulation.ActionType.SCAN && !settings.untilPrecise()) {
            remaining = current.withService(new SpaceSimulation.ServiceSettings(
                    (int) Math.max(1, settings.durationTicks() - actionTicks), settings.untilPrecise(), settings.timeoutTicks(), settings.slot()));
        } else return plan;
        return plan.withBranches(plan.branches().stream().map(branch -> branch.isRoot() ? branch.withActions(
                branch.actions().stream().map(action -> action.id().equals(current.id()) ? remaining : action).toList()) : branch).toList());
    }

    private MissionForecast() { }
}
