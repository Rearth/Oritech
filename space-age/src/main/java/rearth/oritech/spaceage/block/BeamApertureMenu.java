package rearth.oritech.spaceage.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import rearth.oritech.spaceage.init.SpaceAgeMenus;
import rearth.oritech.spaceage.network.RocketNetworking.AperturePayload;
import rearth.oritech.spaceage.simulation.StaticRocketSegment.BlockData;
import rearth.oritech.spaceage.simulation.BeamReception;

import java.util.List;
import java.util.Optional;

/** Read-only receiver dashboard; only viewers receive the bounded structure snapshot. */
public final class BeamApertureMenu extends AbstractContainerMenu {

    public final BlockPos pos;
    public Snapshot snapshot;
    private final Player viewer;
    private int syncTicks = 19;
    private boolean initialSync = true;

    public BeamApertureMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {

        this(id, inventory, buffer.readBlockPos(), Snapshot.EMPTY);
    }

    public BeamApertureMenu(int id, Inventory inventory, BlockPos pos, Snapshot snapshot) {

        super(SpaceAgeMenus.BEAM_APERTURE.get(), id);
        this.pos = pos;
        this.snapshot = snapshot;
        viewer = inventory.player;
    }

    @Override
    public void broadcastChanges() {

        super.broadcastChanges();
        if (++syncTicks < 20) return;
        syncTicks = 0;
        if (viewer instanceof ServerPlayer player && stillValid(player)
                && player.level().getBlockEntity(pos) instanceof BeamApertureBlockEntity aperture) {
            var next = aperture.snapshot();
            if (initialSync || !next.equals(snapshot)) {
                initialSync = false;
                snapshot = next;
                PacketDistributor.sendToPlayer(player, new AperturePayload(containerId, pos, snapshot));
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {

        return player.level().getBlockEntity(pos) instanceof BeamApertureBlockEntity
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {

        return ItemStack.EMPTY;
    }

    public record Snapshot(List<BlockData> blocks, int width, int height, int area, double quality,
                           Optional<BlockPos> defect, long energy, double received, double exported, double heat,
                           boolean overworld, boolean upward, BeamReception reception) {

        public static final Snapshot EMPTY = new Snapshot(List.of(), 0, 0, 0, 0, Optional.empty(), 0, 0, 0, 0, true, true, BeamReception.EMPTY);

        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockData.CODEC.listOf().fieldOf("blocks").forGetter(Snapshot::blocks),
                Codec.INT.fieldOf("width").forGetter(Snapshot::width),
                Codec.INT.fieldOf("height").forGetter(Snapshot::height),
                Codec.INT.fieldOf("area").forGetter(Snapshot::area),
                Codec.DOUBLE.fieldOf("quality").forGetter(Snapshot::quality),
                BlockPos.CODEC.optionalFieldOf("defect").forGetter(Snapshot::defect),
                Codec.LONG.fieldOf("energy").forGetter(Snapshot::energy),
                Codec.DOUBLE.fieldOf("received").forGetter(Snapshot::received),
                Codec.DOUBLE.fieldOf("exported").forGetter(Snapshot::exported),
                Codec.DOUBLE.fieldOf("heat").forGetter(Snapshot::heat),
                Codec.BOOL.fieldOf("overworld").forGetter(Snapshot::overworld),
                Codec.BOOL.fieldOf("upward").forGetter(Snapshot::upward),
                BeamReception.CODEC.fieldOf("reception").forGetter(Snapshot::reception)
        ).apply(instance, Snapshot::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);
    }
}
