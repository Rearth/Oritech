#version 330
#moj_import <minecraft:projection.glsl>

layout(std140) uniform ScanData {
    mat4 InverseView;
    vec4 OriginAge;
    vec4 DirectionRange;
    vec4 ScanColor;
    vec4 TargetDirection;
};
out vec2 texCoord;
flat out mat4 inverseViewProjection;

void main() {
    vec2 uv = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
    texCoord = uv;
    // Use the actual projection, including view bobbing and screen distortion.
    inverseViewProjection = InverseView * inverse(ProjMat);
}
