package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Separates missed aperture area, aperture throughput, and buffer rejection in one beam leg. */
public record BeamReception(double incident, double collected, double capacityLoss, double storageLoss, double accepted, double radius) {

    public static final BeamReception EMPTY = new BeamReception(0, 0, 0, 0, 0, 0);
    public static final Codec<BeamReception> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("incident").forGetter(BeamReception::incident),
            Codec.DOUBLE.fieldOf("collected").forGetter(BeamReception::collected),
            Codec.DOUBLE.fieldOf("capacity_loss").forGetter(BeamReception::capacityLoss),
            Codec.DOUBLE.fieldOf("storage_loss").forGetter(BeamReception::storageLoss),
            Codec.DOUBLE.fieldOf("accepted").forGetter(BeamReception::accepted),
            Codec.DOUBLE.fieldOf("radius").forGetter(BeamReception::radius)
    ).apply(i, BeamReception::new));

    public static BeamReception limited(double incident, double collected, double capacity, double stored, double radius) {

        var captured = Math.max(0, Math.min(incident, collected));
        var admitted = Math.max(0, Math.min(captured, capacity));
        var accepted = Math.max(0, Math.min(admitted, stored));
        return new BeamReception(incident, captured, captured - admitted, admitted - accepted, accepted, radius);
    }

    public double missed() {

        return Math.max(0, incident - collected);
    }

    public double coverage() {

        return incident > 0 ? collected / incident : 0;
    }

    public BeamReception plus(BeamReception other) {

        return new BeamReception(incident + other.incident, collected + other.collected, capacityLoss + other.capacityLoss,
                storageLoss + other.storageLoss, accepted + other.accepted, Math.max(radius, other.radius));
    }

    public BeamReception scaled(double scale) {

        return new BeamReception(incident * scale, collected * scale, capacityLoss * scale, storageLoss * scale, accepted * scale, radius);
    }
}
