package rearth.oritech.spaceage.client.rocket;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.spaceage.OritechSpaceAge;

/** Smoke expands and catches world light; hot embers shrink and remain emissive. */
public final class ExhaustParticle extends SingleQuadParticle {

    // Faint foreground puffs must not occlude the smoke behind them.
    private static final Layer EXHAUST_LAYER = new Layer(true, TextureAtlas.LOCATION_PARTICLES,
            RenderPipelines.TRANSLUCENT_PARTICLE.toBuilder()
                    .withLocation(OritechSpaceAge.id("pipeline/exhaust_particle"))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .build());

    enum Kind { SMOKE, EMBER, ION }

    private final Kind kind;
    private final float size;

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {

        event.registerPipeline(EXHAUST_LAYER.pipeline());
    }

    ExhaustParticle(ClientLevel level, Vec3 position, Vec3 velocity, float size, Kind kind) {

        super(level, position.x, position.y, position.z, sprite(kind));
        this.kind = kind;
        this.size = size;
        quadSize = size;
        xd = velocity.x;
        yd = velocity.y;
        zd = velocity.z;
        lifetime = kind == Kind.SMOKE ? 30 + random.nextInt(25) : 10 + random.nextInt(9);
        friction = kind == Kind.SMOKE ? .93f : .96f;
        gravity = kind == Kind.SMOKE ? -.025f : .12f;
        hasPhysics = true;
        roll = random.nextFloat() * (float) Math.PI * 2;
        oRoll = roll;
        if (kind == Kind.ION) setColor(.25f, .65f, 1);
    }

    private static TextureAtlasSprite sprite(Kind kind) {

        var name = switch (kind) {
            case SMOKE -> "smoke";
            case EMBER -> "spark";
            case ION -> "ion_spark";
        };
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES)
                .getSprite(OritechSpaceAge.id("effect/thruster_" + name));
    }

    @Override
    public void tick() {

        super.tick();
        var progress = Math.min(1, age / (float) lifetime);
        alpha = (kind == Kind.SMOKE ? .65f : 1) * (1 - progress) * Math.min(1, age / 3f);
    }

    @Override
    public float getQuadSize(float partialTick) {

        var progress = Math.min(1, (age + partialTick) / lifetime);
        return size * (kind == Kind.SMOKE ? 1 + 2.5f * progress : 1 - .7f * progress);
    }

    @Override
    protected int getLightCoords(float partialTick) {

        return kind == Kind.SMOKE ? super.getLightCoords(partialTick) : RenderHelpers.FULL_BRIGHT;
    }

    @Override
    protected Layer getLayer() {

        return EXHAUST_LAYER;
    }
}
