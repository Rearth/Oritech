package rearth.oritech.spaceage.simulation.rocket;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum SpacecraftModel implements StringRepresentable {
    AUTO("auto"),
    ORIGINAL("original"),
    ALTERNATE("alternate");

    public static final Codec<SpacecraftModel> CODEC = StringRepresentable.fromEnum(SpacecraftModel::values);

    private final String name;

    SpacecraftModel(String name) {

        this.name = name;
    }

    @Override
    public String getSerializedName() {

        return name;
    }

    public SpacecraftModel resolve(boolean solar) {

        return this == AUTO ? solar ? ORIGINAL : ALTERNATE : this;
    }
}
