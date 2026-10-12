package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;

import java.util.List;
import java.util.UUID;

/**
 * Typed parameters for explicit rocket services. A reference always belongs to its containing segment.
 */
public record RocketServiceSettings(Processing processing, Docking docking, Exchange exchange) {

    public static final RocketServiceSettings DEFAULT = new RocketServiceSettings(Processing.DEFAULT, Docking.DEFAULT, Exchange.DEFAULT);
    public static final Codec<RocketServiceSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Processing.CODEC.fieldOf("processing").forGetter(RocketServiceSettings::processing), Docking.CODEC.fieldOf("docking").forGetter(RocketServiceSettings::docking),
            Exchange.CODEC.fieldOf("exchange").forGetter(RocketServiceSettings::exchange)
    ).apply(i, RocketServiceSettings::new));

    public RocketServiceSettings withProcessing(Processing value) {

        return new RocketServiceSettings(value, docking, exchange);
    }

    public RocketServiceSettings withDocking(Docking value) {

        return new RocketServiceSettings(processing, value, exchange);
    }

    public RocketServiceSettings withExchange(Exchange value) {

        return new RocketServiceSettings(processing, docking, value);
    }

    public enum SourceScope {

        BOTH, REQUESTER, MACHINE;
        public static final Codec<SourceScope> CODEC = Codec.STRING.xmap(SourceScope::valueOf, SourceScope::name);
    }

    public enum Direction {

        NONE, SEND, RECEIVE;
        public static final Codec<Direction> CODEC = Codec.STRING.xmap(Direction::valueOf, Direction::name);
    }

    public record HardwareRef(SegmentRef segment, BlockPos position) {

        public static final HardwareRef NONE = new HardwareRef(new SegmentRef(BlockPos.ZERO), BlockPos.ZERO);
        public static final Codec<HardwareRef> CODEC = RecordCodecBuilder.create(i -> i.group(
                SegmentRef.CODEC.fieldOf("segment").forGetter(HardwareRef::segment), BlockPos.CODEC.fieldOf("position").forGetter(HardwareRef::position)
        ).apply(i, HardwareRef::new));
    }

    public record Processing(List<HardwareRef> modules, SourceScope scope, UUID host) {

        public static final Processing DEFAULT = new Processing(List.of(), SourceScope.BOTH, FlightPlanAction.NO_TARGET);
        public static final Codec<Processing> CODEC = RecordCodecBuilder.create(i -> i.group(
                HardwareRef.CODEC.listOf().fieldOf("modules").forGetter(Processing::modules),
                SourceScope.CODEC.fieldOf("scope").forGetter(Processing::scope), UUIDUtil.STRING_CODEC.fieldOf("host").forGetter(Processing::host)
        ).apply(i, Processing::new));

        public Processing {

            modules = List.copyOf(modules);
        }
    }

    public record Docking(UUID host, HardwareRef localPort, HardwareRef hostPort, long revision, double x, double y) {

        public static final Docking DEFAULT = new Docking(FlightPlanAction.NO_TARGET, HardwareRef.NONE, HardwareRef.NONE, -1, 0, 0);
        public static final Codec<Docking> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("host").forGetter(Docking::host), HardwareRef.CODEC.fieldOf("local_port").forGetter(Docking::localPort),
                HardwareRef.CODEC.fieldOf("host_port").forGetter(Docking::hostPort), Codec.LONG.fieldOf("revision").forGetter(Docking::revision),
                Codec.DOUBLE.fieldOf("x").forGetter(Docking::x), Codec.DOUBLE.fieldOf("y").forGetter(Docking::y)
        ).apply(i, Docking::new));
    }

    public record ResourceExchange(Direction direction, long limit, long reserve) {

        public static final ResourceExchange DEFAULT = new ResourceExchange(Direction.NONE, Long.MAX_VALUE, 0);
        public static final Codec<ResourceExchange> CODEC = RecordCodecBuilder.create(i -> i.group(
                Direction.CODEC.fieldOf("direction").forGetter(ResourceExchange::direction), Codec.LONG.fieldOf("limit").forGetter(ResourceExchange::limit),
                Codec.LONG.fieldOf("reserve").forGetter(ResourceExchange::reserve)
        ).apply(i, ResourceExchange::new));
        public ResourceExchange {

            limit = Math.max(0, limit);
            reserve = Math.max(0, reserve);
        }
    }

    public record Exchange(Direction cargo, List<HardwareRef> local, List<HardwareRef> remote, Identifier filter,
                           ResourceExchange fuel, ResourceExchange rf) {

        public static final Exchange DEFAULT = new Exchange(Direction.NONE, List.of(), List.of(), Identifier.parse("minecraft:air"), ResourceExchange.DEFAULT,
                ResourceExchange.DEFAULT);
        public static final Codec<Exchange> CODEC = RecordCodecBuilder.create(i -> i.group(
                Direction.CODEC.fieldOf("cargo").forGetter(Exchange::cargo), HardwareRef.CODEC.listOf().fieldOf("local").forGetter(Exchange::local),
                HardwareRef.CODEC.listOf().fieldOf("remote").forGetter(Exchange::remote), Identifier.CODEC.fieldOf("filter").forGetter(Exchange::filter),
                ResourceExchange.CODEC.fieldOf("fuel").forGetter(Exchange::fuel), ResourceExchange.CODEC.fieldOf("rf").forGetter(Exchange::rf)
        ).apply(i, Exchange::new));

        public Exchange {

            local = List.copyOf(local);
            remote = List.copyOf(remote);
        }
    }
}
