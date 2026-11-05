package rearth.oritech.spaceage.client.rocket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.spaceage.OritechSpaceAge;

/** Smoke expands and catches world light; hot embers shrink and remain emissive. */
final class ExhaustParticle extends SingleQuadParticle {

    enum Kind { SMOKE, EMBER, ION }

    private final Kind kind;
    private final float size;

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

        return Layer.TRANSLUCENT;
    }
}
