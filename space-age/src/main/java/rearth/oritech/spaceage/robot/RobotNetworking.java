package rearth.oritech.spaceage.robot;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import rearth.oritech.spaceage.OritechSpaceAge;

public final class RobotNetworking {

    private RobotNetworking() {}

    public static void register(PayloadRegistrar registrar) {

        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof RobotMenu menu) || menu.containerId != payload.menu()) return;
            menu.program = payload.program();
            menu.cursor = payload.cursor();
            menu.running = payload.running();
            menu.repeat = payload.repeat();
            menu.skip = payload.skip();
            menu.redstone = payload.redstone();
            menu.energy = payload.energy();
            menu.status = payload.status();
            menu.revision++;
        }));
    }

    public record Snapshot(int menu, RobotProgram program, int cursor, boolean running, boolean repeat,
                           boolean skip, int redstone, long energy, String status) implements CustomPacketPayload {

        public static final Type<Snapshot> TYPE = new Type<>(OritechSpaceAge.id("robot_menu"));
        private static final StreamCodec<RegistryFriendlyByteBuf, RobotProgram> PROGRAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(RobotProgram.CODEC);
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = new StreamCodec<>() {
            @Override
            public Snapshot decode(RegistryFriendlyByteBuf buffer) {

                return new Snapshot(buffer.readVarInt(), PROGRAM_CODEC.decode(buffer), buffer.readVarInt(), buffer.readBoolean(),
                        buffer.readBoolean(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarLong(), buffer.readUtf());
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
                buffer.writeVarLong(value.energy);
                buffer.writeUtf(value.status);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {

            return TYPE;
        }
    }
}
