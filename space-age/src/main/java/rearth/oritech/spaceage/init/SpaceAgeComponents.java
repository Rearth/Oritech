package rearth.oritech.spaceage.init;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import java.util.UUID;
import java.util.function.Supplier;

public final class SpaceAgeComponents {
    public record MissionCard(UUID system, String name, SpaceSimulation.FlightPlan plan) {
        public static final Codec<MissionCard> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("system").forGetter(MissionCard::system),
                Codec.STRING.fieldOf("name").forGetter(MissionCard::name),
                SpaceSimulation.FlightPlan.CODEC.fieldOf("plan").forGetter(MissionCard::plan)
        ).apply(i, MissionCard::new));
    }
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, OritechSpaceAge.MOD_ID);
    public static final Supplier<DataComponentType<MissionCard>> MISSION = COMPONENTS.registerComponentType("mission", b ->
            b.persistent(MissionCard.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(MissionCard.CODEC)));
    private SpaceAgeComponents() { }
}
