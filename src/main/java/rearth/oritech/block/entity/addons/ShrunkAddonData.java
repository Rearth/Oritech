package rearth.oritech.block.entity.addons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.util.MachineAddonController.BaseAddonData;

public record ShrunkAddonData(BaseAddonData data, boolean fluid, int quarryCount, int hunterCount, int yieldCount,
                              boolean cropFilter, boolean silk) {

    public static final Codec<ShrunkAddonData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BaseAddonData.CODEC.fieldOf("data").forGetter(ShrunkAddonData::data),
            Codec.BOOL.fieldOf("fluid").forGetter(ShrunkAddonData::fluid),
            Codec.INT.fieldOf("quarry_count").forGetter(ShrunkAddonData::quarryCount),
            Codec.INT.optionalFieldOf("hunter_count", 0).forGetter(ShrunkAddonData::hunterCount),
            Codec.INT.fieldOf("yield_count").forGetter(ShrunkAddonData::yieldCount),
            Codec.BOOL.fieldOf("crop_filter").forGetter(ShrunkAddonData::cropFilter),
            Codec.BOOL.fieldOf("silk").forGetter(ShrunkAddonData::silk)
    ).apply(instance, ShrunkAddonData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShrunkAddonData> STREAM_CODEC = NetworkManager.getAutoCodec(ShrunkAddonData.class);

    @Override
    public @NotNull String toString() {
        return "ShrunkAddonData{" +
                "data=" + data +
                ", fluid=" + fluid +
                ", quarryCount=" + quarryCount +
                ", hunterCount=" + hunterCount +
                ", yieldCount=" + yieldCount +
                ", cropFilter=" + cropFilter +
                ", silk=" + silk +
                '}';
    }
}
