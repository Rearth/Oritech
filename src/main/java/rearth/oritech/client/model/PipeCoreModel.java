package rearth.oritech.client.model;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.math.Quadrant;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import rearth.oritech.Oritech;
import rearth.oritech.block.logistics.PipeCore.PipeKind;
import rearth.oritech.block.logistics.PipeCoreBlockEntity;

import java.util.List;
import java.util.Map;

public final class PipeCoreModel implements DynamicBlockStateModel {

    private static final int CACHED_QUAD_LIMIT = 16_384;
    private static final ModelState IDENTITY = new ModelState() {};
    private static final Direction[] DIRECTIONS = Direction.values();

    // Chunk rebuilds only need the atlas material from the resource reload.
    private static final ModelBaker.Interner NO_INTERNING = new ModelBaker.Interner() {
        @Override
        public Vector3fc vector(Vector3fc vector) {
            return vector;
        }

        @Override
        public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo material) {
            return material;
        }
    };

    private final Material.Baked texture;
    private final BakedQuad.MaterialInfo material;
    private final LoadingCache<Map<PipeKind, List<AABB>>, SimpleModelWrapper> bakedLayouts;

    private PipeCoreModel(Material.Baked texture) {
        this.texture = texture;
        material = BakedQuad.MaterialInfo.of(texture, texture.sprite().contents().transparency(), CuboidFace.NO_TINT, true, 0, false);

        // Each resource reload creates a new model and cache for the current atlas.
        bakedLayouts = CacheBuilder.newBuilder()
            .maximumWeight(CACHED_QUAD_LIMIT)
            .weigher((Map<PipeKind, List<AABB>> layout, SimpleModelWrapper model) -> model.quads().getAll().size())
            .build(CacheLoader.from(this::bakeLayout));
    }

    public static void register(RegisterBlockStateModels event) {
        event.registerModel(Oritech.id("pipe_core"), Unbaked.CODEC);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
        var layout = level.getModelData(pos).get(PipeCoreBlockEntity.MODEL_LAYOUT);
        if (layout != null) output.add(bakedLayouts.getUnchecked(layout));
    }

    private SimpleModelWrapper bakeLayout(Map<PipeKind, List<AABB>> layout) {
        var quads = new QuadCollection.Builder();
        layout.forEach((kind, boxes) -> {
            var faceData = new ExtraFaceData(color(kind), 0, false);
            for (var box : boxes) {
                var from = new Vector3f((float) (box.minX * 16), (float) (box.minY * 16), (float) (box.minZ * 16));
                var to = new Vector3f((float) (box.maxX * 16), (float) (box.maxY * 16), (float) (box.maxZ * 16));
                for (var face : DIRECTIONS) {
                    var uv = FaceBakery.defaultFaceUV(from, to, face);
                    // Outside elbows extend past the block bounds. Keep their UVs in the sprite.
                    uv = new CuboidFace.UVs(Mth.clamp(uv.minU(), 0, 16), Mth.clamp(uv.minV(), 0, 16),
                        Mth.clamp(uv.maxU(), 0, 16), Mth.clamp(uv.maxV(), 0, 16));
                    quads.addUnculledFace(FaceBakery.bakeQuad(NO_INTERNING, from, to, uv, Quadrant.R0, material, face, IDENTITY, null, faceData));
                }
            }
        });
        return new SimpleModelWrapper(quads.build(), false, texture);
    }

    private static int color(PipeKind kind) {
        return switch (kind) {
            case ITEM -> 0xFF64BA66;
            case FLUID -> 0xFF589AD9;
            case ENERGY -> 0xFFE79844;
        };
    }

    @Override
    public Material.Baked particleMaterial() {
        return texture;
    }

    @Override
    public int materialFlags() {
        return material.flags();
    }

    public record Unbaked(Material texture) implements CustomUnbakedBlockStateModel {
        private static final MapCodec<Unbaked> CODEC = Material.CODEC.fieldOf("texture").xmap(Unbaked::new, Unbaked::texture);

        @Override
        public MapCodec<Unbaked> codec() {
            return CODEC;
        }

        @Override
        public PipeCoreModel bake(ModelBaker baker) {
            return new PipeCoreModel(baker.materials().get(texture, () -> "oritech:pipe_core"));
        }

        @Override
        public void resolveDependencies(Resolver resolver) {}
    }
}
