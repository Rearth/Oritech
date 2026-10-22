package rearth.oritech.spaceage.block.robot_arm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record RobotProgram(Optional<GlobalPos> binding, List<Action> actions) {

    public static final int LIMIT = 256;
    public static final RobotProgram EMPTY = new RobotProgram(Optional.empty(), List.of());
    public static final Codec<RobotProgram> CODEC = RecordCodecBuilder.create(i -> i.group(
            GlobalPos.CODEC.optionalFieldOf("binding").forGetter(RobotProgram::binding),
            Action.CODEC.listOf().fieldOf("actions").forGetter(RobotProgram::actions)
    ).apply(i, RobotProgram::new));

    public RobotProgram {

        actions = List.copyOf(actions);
    }

    public RobotProgram withActions(List<Action> updated) {

        return new RobotProgram(binding, updated);
    }

    public RobotProgram withMatch(int index, Match match) {

        var updated = new ArrayList<>(actions);
        updated.set(index, actions.get(index).withMatch(match));
        return withActions(updated);
    }

    public RobotProgram without(int index) {

        var updated = new ArrayList<>(actions);
        updated.remove(index);
        return withActions(updated);
    }

    public enum Kind { PLACE, DESTROY, INTERACT }

    /**
     * How strictly the block at the target has to match the recorded one before a destroy or use action.
     * Placements only need a replaceable target, so they ignore it.
     */
    public enum Match {
        IGNORE, BLOCK, STATE;

        public boolean test(BlockState recorded, BlockState current) {

            return switch (this) {
                case IGNORE -> true;
                case BLOCK -> current.is(recorded.getBlock());
                case STATE -> current == recorded;
            };
        }
    }

    public record Action(Kind kind, BlockPos pos, BlockState state, Direction face, Vec3 hit,
                         ItemStack item, float yaw, float pitch, boolean sneaking, Match match) {

        public static final Codec<Action> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.xmap(Kind::valueOf, Kind::name).fieldOf("kind").forGetter(Action::kind),
                BlockPos.CODEC.fieldOf("pos").forGetter(Action::pos),
                BlockState.CODEC.fieldOf("state").forGetter(Action::state),
                Direction.CODEC.fieldOf("face").forGetter(Action::face),
                Vec3.CODEC.fieldOf("hit").forGetter(Action::hit),
                ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(Action::item),
                Codec.FLOAT.fieldOf("yaw").forGetter(Action::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(Action::pitch),
                Codec.BOOL.fieldOf("sneaking").forGetter(Action::sneaking),
                Codec.STRING.xmap(Match::valueOf, Match::name).optionalFieldOf("match", Match.IGNORE).forGetter(Action::match)
        ).apply(i, Action::new));

        public Action withMatch(Match updated) {

            return new Action(kind, pos, state, face, hit, item, yaw, pitch, sneaking, updated);
        }
    }
}
