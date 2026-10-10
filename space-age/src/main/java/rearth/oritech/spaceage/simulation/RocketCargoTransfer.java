package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import rearth.oritech.spaceage.block.BlockPairController;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.Direction;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.Exchange;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.HardwareRef;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.ResourceExchange;

import java.util.List;

/**
 * Whole cell pairs move atomically at an event boundary; overflow remains at its source.
 */
public final class RocketCargoTransfer {

    private RocketCargoTransfer() {
    }

    public static Result exchange(ActiveRocketData visitor, ActiveRocketData host, Exchange settings) {

        var moved = 0;
        var remaining = 0;
        if (settings.cargo() != Direction.NONE) {
            var send = settings.cargo() == Direction.SEND;
            var source = send ? visitor : host;
            var destination = send ? host : visitor;
            var sourceRefs = selected(source, send ? settings.local() : settings.remote());
            var destinationRefs = selected(destination, send ? settings.remote() : settings.local());
            for (var ref : sourceRefs) {
                var pair = RocketLayout.pair(source, ref);
                var first = RocketLayout.state(source, ref.segment(), pair.left());
                var second = RocketLayout.state(source, ref.segment(), pair.right());
                if (first.isAir() && second.isAir()) continue;
                // Air is the card's "all cargo" filter; matching either cell moves the complete pair.
                if (!settings.filter().equals(Identifier.parse("minecraft:air"))
                        && !BuiltInRegistries.BLOCK.getKey(first.getBlock()).equals(settings.filter())
                                && !BuiltInRegistries.BLOCK.getKey(second.getBlock()).equals(settings.filter()))
                    continue;
                if (RocketLayout.busy(source, pair)) {
                    remaining++;
                    continue;
                }
                var target = destinationRefs.stream().map(r -> RocketLayout.pair(destination, r)).filter(p -> empty(destination, p)).findFirst().orElse(null);
                if (target == null) {
                    remaining++;
                    continue;
                }
                var rotation = rotation(source, pair, destination, target);
                // Fill both destination cells before clearing the pair; partial pairs are never merged.
                RocketLayout.setPair(destination, target, first.rotate(rotation), second.rotate(rotation));
                RocketLayout.setPair(source, pair, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState());
                moved++;
            }
        }
        var fuel = resource(visitor, host, settings.fuel(), true);
        var rf = resource(visitor, host, settings.rf(), false);
        return new Result(moved, remaining, fuel, rf);
    }

    private static long resource(ActiveRocketData visitor, ActiveRocketData host, ResourceExchange settings, boolean fuel) {

        if (settings.direction() == Direction.NONE) return 0;
        return settings.direction() == Direction.SEND
                ? RocketResourceTransfer.move(visitor, host, fuel, settings.limit(), settings.reserve())
                : RocketResourceTransfer.move(host, visitor, fuel, settings.limit(), 0);
    }

    private static List<HardwareRef> selected(ActiveRocketData rocket, List<HardwareRef> selection) {

        var available = RocketLayout.controllers(rocket, false);
        return selection.isEmpty() ? available : available.stream().filter(selection::contains).toList();
    }

    public static boolean empty(ActiveRocketData rocket, RocketControllerState.Pair pair) {

        return !RocketLayout.busy(rocket, pair) && RocketLayout.state(rocket, pair.controller().segment(), pair.left()).isAir()
                && RocketLayout.state(rocket, pair.controller().segment(), pair.right()).isAir();
    }

    private static Rotation rotation(ActiveRocketData source, RocketControllerState.Pair a, ActiveRocketData destination, RocketControllerState.Pair b) {

        var from = BlockPairController.left(RocketLayout.state(source, a.controller().segment(), a.controller().position()));
        var to = BlockPairController.left(RocketLayout.state(destination, b.controller().segment(), b.controller().position()));
        for (var rotation : Rotation.values()) {
            if (rotation.rotate(from) == to) return rotation;
        }
        return Rotation.NONE;
    }

    public record Result(int moved, int remaining, long fuel, long rf) {

        public static final Codec<Result> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("moved").forGetter(Result::moved), Codec.INT.fieldOf("remaining").forGetter(Result::remaining),
                Codec.LONG.fieldOf("fuel").forGetter(Result::fuel), Codec.LONG.fieldOf("rf").forGetter(Result::rf)
        ).apply(i, Result::new));
    }
}
