package rearth.oritech.spaceage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram;

public final class RobotNetworking {

    private RobotNetworking() {
    }

    public static void register(PayloadRegistrar registrar) {

        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, (payload, context) -> context.enqueueWork(() -> {
            var menu = context.player().containerMenu;
            if (menu instanceof Receiver receiver && menu.containerId == payload.menu()) receiver.receive(payload);
        }));
    }

    /**
     * A menu showing a playbook, either on an arm or on a held card.
     */
    public interface Receiver {

        void receive(Snapshot snapshot);
    }

    public record Snapshot(int menu, RobotProgram program, int cursor, boolean running, boolean repeat,
                           boolean skip, int redstone, String status) implements CustomPacketPayload {

        public static final Type<Snapshot> TYPE = new Type<>(OritechSpaceAge.id("robot_menu"));
        private static final StreamCodec<RegistryFriendlyByteBuf, RobotProgram> PROGRAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(RobotProgram.CODEC);
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = new StreamCodec<>() {
            @Override
            public Snapshot decode(RegistryFriendlyByteBuf buffer) {

                return new Snapshot(buffer.readVarInt(), PROGRAM_CODEC.decode(buffer), buffer.readVarInt(), buffer.readBoolean(),
                        buffer.readBoolean(), buffer.readBoolean(), buffer.readVarInt(), buffer.readUtf());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Snapshot value) {

                buffer.writeVarInt(value.menu);
                PROGRAM_CODEC.encode(buffer, value.program);
                buffer.writeVarInt(value.cursor);
                buffer.writeBoolean(value.running);
                buffer.writeBoolean(value.repeat);
                buffer.writeBoolean(value.skip);
                buffer.writeVarInt(value.redstone);
                buffer.writeUtf(value.status);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {

            return TYPE;
        }
    }
}
