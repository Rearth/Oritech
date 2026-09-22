package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.GroundStationBlockEntity;
import java.util.*;

public final class MissionSavedData extends SavedData {
    public record Contact(UUID owner, MissionState.Telemetry telemetry) {
        public static final Codec<Contact> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(Contact::owner),
                MissionState.Telemetry.CODEC.fieldOf("telemetry").forGetter(Contact::telemetry)
        ).apply(i, Contact::new));
    }
    public static final Codec<MissionSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            MissionState.CODEC.listOf().fieldOf("craft").forGetter(d -> List.copyOf(d.craft.values())),
            Contact.CODEC.listOf().fieldOf("contacts").forGetter(d -> List.copyOf(d.contacts.values())),
            Codec.LONG.optionalFieldOf("extra_ticks", 0L).forGetter(d -> d.extraTicks)
    ).apply(i, MissionSavedData::new));
    private static final SavedDataType<MissionSavedData> TYPE = new SavedDataType<>(OritechSpaceAge.id("missions"), MissionSavedData::new, CODEC, null);
    public final Map<UUID, MissionState> craft = new LinkedHashMap<>();
    public final Map<UUID, Contact> contacts = new LinkedHashMap<>();
    public final Map<GlobalPos, GroundStationBlockEntity> stations = new HashMap<>();
    public List<SpaceCommunications.Node> networkNodes = List.of();
    public long extraTicks;
    public int debugSpeed = 1;
    MissionSavedData() { }
    private MissionSavedData(List<MissionState> missions, List<Contact> contacts, long extraTicks) {
        this.extraTicks = extraTicks;
        missions.forEach(m -> craft.put(m.rocket.getRocketId(), m));
        contacts.forEach(c -> this.contacts.put(c.telemetry.rocket().getRocketId(), c));
    }
    public void receive(MissionState state) { contacts.put(state.rocket.getRocketId(), new Contact(state.owner, state.earth)); }
    public boolean dismiss(UUID owner, UUID craftId) {
        var mission = craft.get(craftId);
        if (mission == null || !mission.owner.equals(owner) || !mission.canDismiss()) return false;
        craft.remove(craftId);
        contacts.remove(craftId);
        return true;
    }
    public static MissionSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
}
