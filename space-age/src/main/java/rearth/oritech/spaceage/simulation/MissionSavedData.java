package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.GroundStationBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MissionSavedData extends SavedData {

    public static final Codec<MissionSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            MissionState.CODEC.listOf().fieldOf("craft").forGetter(d -> List.copyOf(d.craft.values())),
            Codec.LONG.optionalFieldOf("extra_ticks", 0L).forGetter(d -> d.extraTicks),
            DockingLink.CODEC.listOf().fieldOf("docking_links").forGetter(d -> d.dockingLinks)
    ).apply(i, MissionSavedData::new));
    private static final SavedDataType<MissionSavedData> TYPE = new SavedDataType<>(OritechSpaceAge.id("missions"), MissionSavedData::new, CODEC, null);
    public final List<DockingLink> dockingLinks = new ArrayList<>();
    public final Map<UUID, MissionState> craft = new LinkedHashMap<>();
    public final Map<GlobalPos, GroundStationBlockEntity> stations = new HashMap<>();
    public List<SpaceCommunications.Node> networkNodes = List.of();
    public long extraTicks;
    public int debugSpeed = 1;

    MissionSavedData() {
    }

    private MissionSavedData(List<MissionState> missions, long extraTicks, List<DockingLink> links) {

        dockingLinks.addAll(links);
        this.extraTicks = extraTicks;
        missions.forEach(m -> craft.put(m.rocket.getRocketId(), m));
    }

    public static MissionSavedData get(MinecraftServer server) {

        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean dismiss(UUID owner, UUID craftId) {

        var mission = craft.get(craftId);
        if (mission == null || !mission.owner.equals(owner) || !mission.canDismiss() || RocketDocking.linked(this, craftId))
            return false;
        craft.remove(craftId);
        return true;
    }
}
