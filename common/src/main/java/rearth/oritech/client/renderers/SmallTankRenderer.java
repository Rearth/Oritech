package rearth.oritech.client.renderers;

import dev.architectury.hooks.fluid.FluidStackHooks;
import io.wispforest.owo.ui.core.Color;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;
import rearth.oritech.block.entity.storage.SmallTankEntity;

public class SmallTankRenderer implements BlockEntityRenderer<SmallTankEntity> {
    
    @Override
    public void render(SmallTankEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        
        var storage = entity.fluidStorage;
        if (storage.getAmount() <= 0 || storage.getFluid().equals(Fluids.EMPTY)) return;
        
        var fluid = storage.getFluid();
        var fill = storage.getAmount() / (float) storage.getCapacity();
        
        var sprite = FluidStackHooks.getStillTexture(fluid);
        var spriteColor = FluidStackHooks.getColor(fluid);
        var consumer = vertexConsumers.getBuffer(RenderLayer.getTranslucent());
        
        var parsedColor = Color.ofArgb(spriteColor);
        var opaqueColor = new Color(parsedColor.red(), parsedColor.green(), parsedColor.blue(), 1f);
        spriteColor = opaqueColor.argb();
        
        matrices.push();
        matrices.translate(0.126, 0.126, 0.126);
        matrices.scale(0.745f, 0.745f * fill, 0.745f);
        
        var entry = matrices.peek();
        var modelMatrix = entry.getPositionMatrix();
        
        // Draw the cube using quads
        for (Direction direction : Direction.values()) {
            if (direction.equals(Direction.DOWN)) continue; // skip bottom, as it's never visible
            drawQuad(direction, consumer, modelMatrix, entry, sprite, spriteColor, light, overlay);
        }
        
        matrices.pop();
        
    }
    
    public static void drawQuad(Direction direction, VertexConsumer consumer, Matrix4f modelMatrix, MatrixStack.Entry normalMatrix, Sprite sprite, int color, int light, int overlay) {
        // Define the vertices of the quad based on the direction it's facing
        
        var normal = direction.getUnitVector();
        
        var positions = getQuadVerticesByDirection(direction);
        
        for (int i = positions.length - 1; i >= 0; i--) {
            
            var pos = positions[i];
            var u = sprite.getFrameU(getFrameU()[i]);
            var v = sprite.getFrameV(getFrameV()[i]);
            
            consumer.vertex(modelMatrix, pos[0], pos[1], pos[2])
              .color(color)
              .texture(u, v)
              .light(light)
              .overlay(overlay)
              .normal(normalMatrix, normal.x, normal.y, normal.z);
        }
        
    }
    
    private static float[] getFrameU() {
        return new float[]{0, 1, 1, 0};
    }
    
    private static float[] getFrameV() {
        return new float[]{0, 0, 1, 1};
    }
    
    private static float[][] getQuadVerticesByDirection(Direction direction) {
        // Define the vertices for each face of the cube
        return switch (direction) {
            case UP -> new float[][]{
              {0, 1, 0}, // Top-left
              {1, 1, 0}, // Top-right
              {1, 1, 1}, // Bottom-right
              {0, 1, 1}  // Bottom-left
            };
            case DOWN -> new float[][]{
              {0, 0, 1}, // Top-left
              {1, 0, 1}, // Top-right
              {1, 0, 0}, // Bottom-right
              {0, 0, 0}  // Bottom-left
            };
            case NORTH -> new float[][]{
              {1, 1, 0}, // Top-left
              {0, 1, 0}, // Top-right
              {0, 0, 0}, // Bottom-right
              {1, 0, 0}  // Bottom-left
            };
            case SOUTH -> new float[][]{
              {0, 1, 1}, // Top-left
              {1, 1, 1}, // Top-right
              {1, 0, 1}, // Bottom-right
              {0, 0, 1}  // Bottom-left
            };
            case WEST -> new float[][]{
              {0, 1, 0}, // Top-left
              {0, 1, 1}, // Top-right
              {0, 0, 1}, // Bottom-right
              {0, 0, 0}  // Bottom-left
            };
            case EAST -> new float[][]{
              {1, 1, 1}, // Top-left
              {1, 1, 0}, // Top-right
              {1, 0, 0}, // Bottom-right
              {1, 0, 1}  // Bottom-left
            };
        };
    }
}
