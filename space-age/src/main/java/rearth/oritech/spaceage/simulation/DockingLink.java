package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.HardwareRef;

import java.util.UUID;

/**
 * A service connection; neither craft's blocks or inventories change owners.
 */
public record DockingLink(UUID host, UUID visitor, HardwareRef hostPort, HardwareRef visitorPort) {

    public static final Codec<DockingLink> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.STRING_CODEC.fieldOf("host").forGetter(DockingLink::host),
            UUIDUtil.STRING_CODEC.fieldOf("visitor").forGetter(DockingLink::visitor),
            HardwareRef.CODEC.fieldOf("host_port").forGetter(DockingLink::hostPort),
            HardwareRef.CODEC.fieldOf("visitor_port").forGetter(DockingLink::visitorPort)
    ).apply(i, DockingLink::new));
}
