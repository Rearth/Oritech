package rearth.oritech.client.renderers.models;

import com.google.gson.JsonObject;
import earth.terrarium.athena.api.client.models.AthenaBlockModel;
import earth.terrarium.athena.api.client.models.AthenaModelAttributes;
import earth.terrarium.athena.api.client.models.AthenaQuad;
import earth.terrarium.athena.api.client.utils.AppearanceAndTintGetter;
import earth.terrarium.athena.api.client.utils.CtmState;
import earth.terrarium.athena.api.client.utils.CtmUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.util.ColorableMachine.ColorVariant;
import rearth.oritech.util.PaintableBlock;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Athena's five-tile CTM layout with a separate material set for each paint color. */
public class ColoredConnectedBlockModel implements AthenaBlockModel {
    private final Int2ObjectMap<Material> materials = new Int2ObjectArrayMap<>();
    private final Map<ColorVariant, int[]> palettes = new EnumMap<>(ColorVariant.class);
    private final AthenaModelAttributes attributes;

    public ColoredConnectedBlockModel(JsonObject json) {
        attributes = AthenaModelAttributes.fromJson(json);
        var textures = json.getAsJsonObject("ctm_textures");
        var names = new String[]{"particle", "empty", "center", "vertical", "horizontal"};
        for (var color : ColorVariant.values()) {
            var palette = new int[names.length];
            for (int index = 0; index < names.length; index++) {
                var texture = Identifier.parse(textures.get(names[index]).getAsString());
                if (color != ColorVariant.ORANGE) {
                    var path = texture.getPath();
                    texture = Identifier.fromNamespaceAndPath(texture.getNamespace(),
                            "block/colored/" + path.substring("block/".length()) + "_" + color.getSerializedName());
                }
                palette[index] = materials.size();
                materials.put(palette[index], new Material(texture));
            }
            palettes.put(color, palette);
        }
    }

    @Override
    public List<AthenaQuad> getQuads(AppearanceAndTintGetter level, BlockState state, BlockPos pos, Direction direction) {
        if (CtmUtils.checkRelative(level, state, pos, direction)) return List.of();
        var connections = CtmState.from(level, state, pos, direction,
                CtmUtils.check(level, state, pos, direction, (self, other) ->
                        other.is(self.getBlock()) && other.getValue(PaintableBlock.COLOR) == self.getValue(PaintableBlock.COLOR)));
        return quads(connections, state.getValue(PaintableBlock.COLOR));
    }

    private List<AthenaQuad> quads(CtmState state, ColorVariant color) {
        var palette = palettes.get(color);
        if (state.allTrue()) return List.of(AthenaQuad.square(palette[1], 0));
        return List.of(
                new AthenaQuad(palette[CtmUtils.getTexture(state.up(), state.left(), state.upLeft())], 0, 0.5f, 1, 0.5f, Rotation.NONE, 0),
                new AthenaQuad(palette[CtmUtils.getTexture(state.up(), state.right(), state.upRight())], 0.5f, 1, 1, 0.5f, Rotation.NONE, 0),
                new AthenaQuad(palette[CtmUtils.getTexture(state.down(), state.left(), state.downLeft())], 0, 0.5f, 0.5f, 0, Rotation.NONE, 0),
                new AthenaQuad(palette[CtmUtils.getTexture(state.down(), state.right(), state.downRight())], 0.5f, 1, 0.5f, 0, Rotation.NONE, 0));
    }

    @Override
    public Map<Direction, List<AthenaQuad>> getDefaultQuads(Direction direction) {
        return direction == null ? Map.of() : Map.of(direction, quads(CtmState.ALL_FALSE, ColorVariant.ORANGE));
    }

    @Override
    public Int2ObjectMap<Material.Baked> getTextures(Function<Material, Material.Baked> getter) {
        var resolved = new Int2ObjectArrayMap<Material.Baked>();
        materials.forEach((index, material) -> resolved.put(index, getter.apply(material)));
        return resolved;
    }

    @Override
    public AthenaModelAttributes getAttributes() {
        return attributes;
    }
}

