package rearth.oritech.spaceage.simulation;

public class RocketSimulationController {

    // todo field here

    public static void LaunchRocket(ActiveRocketData rocket, RocketFlightPlan flightPlan) {

        // todo calculate different events that need to be processed on either server or client

        // calculations needed (in static utility method):
        // available delta-V of rocket (based on engines and loaded fuel type / RF)
        // available functions (none for now)

        // currently needed events (expanded later):
        // rocket takeoff (send relevant data, including acceleration and path / direction to client to render the entire rocket)
        // rocket collisions during ascent path (world scan happens at takeoff time)
        // events in space / orbit (skipped for now)
        // rocket re-enters atmosphere (if flightplan plans / wants it, assumed always for now. Data needs to be sent to client again)
        // rocket collision with ground

    }

    // todo serialize / deserialize events to level saveddata (including planned events, currently moving rockets, etc.)

}
