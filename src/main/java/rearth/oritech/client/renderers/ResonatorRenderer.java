package rearth.oritech.client.renderers;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import rearth.oritech.Oritech;
import rearth.oritech.client.init.OritechClientConfig;

import java.util.OptionalInt;

/**
 * Projects the scan onto visible world surfaces without sampling or loading blocks.
 */
public final class ResonatorRenderer {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Oritech.id("pipeline/resonator"))
            .withVertexShader(Oritech.id("core/resonator"))
            .withFragmentShader(Oritech.id("core/resonator"))
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("ScanData", UniformType.UNIFORM_BUFFER)
            .withSampler("DepthSampler")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();
    private static Scan pulse;
    private static Scan guidance;
    // LevelRenderer resets its render state before AfterLevel. Retain this frame's
    // immutable extraction snapshot until the post-world pass consumes it.
    private static FrameScans frameScans;
    private static MappableRingBuffer uniforms;

    private record Scan(ClientLevel level, Vec3 origin, Vec3 direction, int color, float range, float horizontalRange,
                        long startedAt) {
    }

    private record FrameScans(Scan pulse, Scan guidance, long now) {
    }

    public static void start(Vec3 origin, Vec3 direction, int color, float range, float horizontalRange, boolean searching) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var scan = new Scan(level, origin, direction, color, range, horizontalRange, System.nanoTime());
        if (searching) pulse = scan;
        else guidance = scan;
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }

    public static void onExtractRenderState(ExtractLevelRenderStateEvent event) {
        var client = Minecraft.getInstance();
        long now = System.nanoTime();
        if (!active(pulse, client.level, now, 4)) pulse = null;
        double guidanceDuration = guidance == null ? 8 : Math.max(8, Math.max(3, (guidance.range + 3) / 16.0) + 4);
        if (!active(guidance, client.level, now, guidanceDuration)) guidance = null;
        frameScans = null;
        if (OritechClientConfig.visualEffects.get() != OritechClientConfig.VisualEffects.NONE && (pulse != null || guidance != null)) {
            frameScans = new FrameScans(pulse, guidance, now);
        }
    }

    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        pulse = null;
        guidance = null;
        frameScans = null;
        closeUniforms();
    }

    private static void closeUniforms() {
        if (uniforms != null) {
            uniforms.close();
            uniforms = null;
        }
    }

    public static void onAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        var scans = frameScans;
        frameScans = null;
        if (scans == null) {
            closeUniforms();
            return;
        }
        if (uniforms == null)
            uniforms = new MappableRingBuffer(() -> "Oritech resonator", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, 128);
        if (scans.pulse != null) draw(event, scans.pulse, scans.now, true);
        if (scans.guidance != null) draw(event, scans.guidance, scans.now, false);
    }

    private static boolean active(Scan scan, ClientLevel level, long now, double seconds) {
        return scan != null && scan.level == level && now - scan.startedAt < seconds * 1_000_000_000L;
    }

    private static void draw(RenderLevelStageEvent.AfterLevel event, Scan scan, long now, boolean searching) {
        var target = Minecraft.getInstance().getMainRenderTarget();
        if (target.getDepthTextureView() == null) return;
        var camera = event.getLevelRenderState().cameraRenderState;
        var relativeOrigin = scan.origin.subtract(camera.pos);
        var horizontalDirection = new Vec3(scan.direction.x, 0, scan.direction.z).normalize();
        // Vertical targets have no horizontal bearing; keep the ribbon math defined.
        if (horizontalDirection.lengthSqr() < 0.001) horizontalDirection = new Vec3(1, 0, 0);
        float age = (now - scan.startedAt) / 1_000_000_000f;
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        try (var mapped = encoder.mapBuffer(uniforms.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(mapped.data())
                    .putMat4f(new Matrix4f(event.getModelViewMatrix()).invert())
                    .putVec4(new Vector4f((float) relativeOrigin.x, (float) relativeOrigin.y, (float) relativeOrigin.z, age))
                    .putVec4(new Vector4f((float) horizontalDirection.x, (float) horizontalDirection.z, scan.range, searching ? 1 : 0))
                    .putVec4(new Vector4f(((scan.color >> 16) & 255) / 255f, ((scan.color >> 8) & 255) / 255f, (scan.color & 255) / 255f, scan.horizontalRange))
                    .putVec4(new Vector4f((float) scan.direction.x, (float) scan.direction.y, (float) scan.direction.z, 0));
        }
        // Depth is sampled, never attached to this pass: no read/write feedback or depth changes.
        try (var pass = encoder.createRenderPass(() -> "Oritech resonator", target.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ScanData", uniforms.currentBuffer());
            pass.bindTexture("DepthSampler", target.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.draw(0, 3);
        }
        uniforms.rotate();
    }
}
