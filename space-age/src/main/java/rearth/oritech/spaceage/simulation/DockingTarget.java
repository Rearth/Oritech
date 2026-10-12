package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.HardwareRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.List;
import java.util.UUID;

/**
 * Owner-visible stationary hardware, separate from celestial survey contacts.
 */
public record DockingTarget(UUID id, String name, ActiveRocketData rocket, MissionState.Position position,
                            long revision, List<HardwareRef> ports, List<UUID> visitors, List<DockingLink> links,
                            double visitorMass) {

    public static final Codec<DockingTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(DockingTarget::id),
            Codec.STRING.fieldOf("name").forGetter(DockingTarget::name),
            ActiveRocketData.CODEC.fieldOf("rocket").forGetter(DockingTarget::rocket), MissionState.Position.CODEC.fieldOf("position").forGetter(DockingTarget::position),
            Codec.LONG.fieldOf("revision").forGetter(DockingTarget::revision), HardwareRef.CODEC.listOf().fieldOf("ports").forGetter(DockingTarget::ports),
            UUIDUtil.STRING_CODEC.listOf().fieldOf("visitors").forGetter(DockingTarget::visitors),
            DockingLink.CODEC.listOf().fieldOf("links").forGetter(DockingTarget::links),
            Codec.DOUBLE.fieldOf("visitor_mass").forGetter(DockingTarget::visitorMass)
    ).apply(i, DockingTarget::new));

    public DockingTarget(UUID id, String name, ActiveRocketData rocket, MissionState.Position position,
                         long revision, List<HardwareRef> ports, List<UUID> visitors) {

        this(id, name, rocket, position, revision, ports, visitors, List.of(), 0);
    }

    public DockingTarget {

        ports = List.copyOf(ports);
        visitors = List.copyOf(visitors);
        links = List.copyOf(links);
    }

    public SpaceObjectData point() {

        return new SpaceObjectData(id, SpaceObjects.ObjectType.CRAFT, (float) position.x(), (float) position.y(),
                0, 0, 0, 0, 0, SpaceObjects.DetectionState.PRECISE, name, List.of());
    }

    public RocketServiceSettings.Docking rendezvous(HardwareRef local, HardwareRef remote) {

        return new RocketServiceSettings.Docking(id, local, remote, revision, position.x(), position.y());
    }
}
