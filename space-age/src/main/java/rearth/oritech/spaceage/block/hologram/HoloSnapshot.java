package rearth.oritech.spaceage.block.hologram;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import rearth.oritech.spaceage.simulation.FleetCategories;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.List;
import java.util.UUID;

/** Compact inspection data: no inventories, rocket structures, or new forecasts. */
public record HoloSnapshot(HoloSettings settings, HoloSurface surface, List<SpaceObjectData> objects,
                           List<Craft> craft, FleetCategories.Saved categories, long tick, int speed,
                           int facing, String status) {

    public static final HoloSnapshot EMPTY = new HoloSnapshot(HoloSettings.DEFAULT, HoloSurface.EMPTY,
            List.of(), List.of(), FleetCategories.Saved.EMPTY, 0, 1, 0, "no_station");
    public static final Codec<HoloSnapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
            HoloSettings.CODEC.fieldOf("settings").forGetter(HoloSnapshot::settings),
            HoloSurface.CODEC.fieldOf("surface").forGetter(HoloSnapshot::surface),
            SpaceObjectData.CODEC.listOf().fieldOf("objects").forGetter(HoloSnapshot::objects),
            Craft.CODEC.listOf().fieldOf("craft").forGetter(HoloSnapshot::craft),
            FleetCategories.Saved.CODEC.fieldOf("categories").forGetter(HoloSnapshot::categories),
            Codec.LONG.fieldOf("tick").forGetter(HoloSnapshot::tick),
            Codec.INT.fieldOf("speed").forGetter(HoloSnapshot::speed),
            Codec.INT.fieldOf("facing").forGetter(HoloSnapshot::facing),
            Codec.STRING.fieldOf("status").forGetter(HoloSnapshot::status)
    ).apply(i, HoloSnapshot::new));

    public record Point(double x, double y) {

        public static final Codec<Point> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Point::x), Codec.DOUBLE.fieldOf("y").forGetter(Point::y)
        ).apply(i, Point::new));
    }

    public record Craft(UUID id, String name, String category, double x, double y, double vx, double vy,
                        UUID target, boolean connected, String status, List<Point> path) {

        public static final Codec<Craft> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(Craft::id),
                Codec.STRING.fieldOf("name").forGetter(Craft::name),
                Codec.STRING.fieldOf("category").forGetter(Craft::category),
                Codec.DOUBLE.fieldOf("x").forGetter(Craft::x), Codec.DOUBLE.fieldOf("y").forGetter(Craft::y),
                Codec.DOUBLE.fieldOf("vx").forGetter(Craft::vx), Codec.DOUBLE.fieldOf("vy").forGetter(Craft::vy),
                UUIDUtil.STRING_CODEC.fieldOf("target").forGetter(Craft::target),
                Codec.BOOL.fieldOf("connected").forGetter(Craft::connected),
                Codec.STRING.fieldOf("status").forGetter(Craft::status),
                Point.CODEC.listOf().fieldOf("path").forGetter(Craft::path)
        ).apply(i, Craft::new));
    }
}
