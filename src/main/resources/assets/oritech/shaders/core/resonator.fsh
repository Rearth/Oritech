#version 330
uniform sampler2D DepthSampler;
layout(std140) uniform ScanData {
    mat4 InverseView;
    vec4 OriginAge;
    vec4 DirectionRange;
    vec4 ScanColor; // RGB color, W horizontal distance to the guidance endpoint.
    vec4 TargetDirection; // XYZ normalized direction toward the actual resource.
};
in vec2 texCoord;
flat in mat4 inverseViewProjection;
out vec4 fragColor;

const float CELL_SIZE = 0.125;

// Whole cells form sparse square flecks on block faces, fixed in place as waves pass.
float scanDots(vec3 cell) {
    float variation = fract(sin(dot(cell, vec3(127.1, 311.7, 74.7))) * 43758.5453);
    return step(0.82, variation);
}

float scanTrail(float behind, float trailLength) {
    return step(1.0, behind) * (1.0 - step(trailLength, behind))
    * mix(1.0, 0.4, step(trailLength * 0.5, behind));
}

// A bright front followed by two dimmer bands, without a blurred halo.
float scanBands(float edge, float width) {
    if (edge > 0.0 || edge < -width * 4.0) return 0.0;
    if (edge > -width) return 1.0;
    if (edge > -width * 2.0) return 0.3;
    return 0.1;
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= 0.999999) discard;
    vec4 reconstructed = inverseViewProjection * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 offset = reconstructed.xyz / reconstructed.w - OriginAge.xyz;
    // Snap to a world-axis grid anchored at the scan origin, never to the camera.
    vec3 cell = floor(offset / CELL_SIZE);
    offset = (cell + 0.5) * CELL_SIZE;
    float age = OriginAge.w;
    float range = DirectionRange.z;
    float distance = length(offset);
    float dots = scanDots(cell);
    float intensity;
    if (DirectionRange.w > 0.5) {
        // A spherical shell sweeps vertically over walls as well as across floors.
        float radius = age / 4.0 * range;
        // Fixed two-block patches lead or lag by up to 0.75 blocks in five steps.
        // Share the offset across the front, bands and wake so they stay together.
        vec3 patchCell = floor(offset / 2.0);
        float variation = fract(sin(dot(patchCell, vec3(127.1, 311.7, 74.7))) * 43758.5453);
        float terrace = (floor(variation * 5.0) - 2.0) * 0.375;
        float edge = distance - radius - terrace;
        intensity = scanBands(edge, 0.75);
        // The dotted afterglow follows the shell by roughly a third of a second.
        intensity += dots * 0.32 * scanTrail(-edge, max(3.0, range / 4.0 * 0.35));
        intensity *= 1.0 - smoothstep(3.2, 4.0, age);
    } else {
        vec2 direction = DirectionRange.xy;
        float along = dot(offset.xz, direction);
        float across = abs(offset.x * direction.y - offset.z * direction.x);
        float line = 1.0 - step(0.25, across);
        float ribbon = 1.0 - step(0.75, across);
        // Keep the readable travel speed, but emit a new front every 1.5 seconds
        // instead of waiting for the previous front to reach the end of the range.
        float sweepDuration = max(3.0, (range + 3.0) / 16.0);
        float speed = (range + 3.0) / sweepDuration;
        float radius = age * speed;
        float spacing = speed * 1.5;
        // Project onto the actual target bearing. Wave planes travel toward the
        // resource, moving down walls for targets below and up for targets above.
        float progress = dot(offset, TargetDirection.xyz);
        float shell = clamp(floor((radius - progress) / spacing + 0.5), 0.0, floor(age / 1.5));
        radius -= shell * spacing;
        float edge = progress - radius;
        float sweep = scanBands(edge, 1.0);
        intensity = line * 0.4 + ribbon * sweep * 0.8;
        float trailLength = max(3.0, speed * 0.45);
        float wake = 1.0 - step(1.25, across);
        intensity += dots * 0.35 * wake * scanTrail(-edge, trailLength);
        // End above/below the resource's XZ position, independently of its depth.
        // The ribbon's endpoint stays horizontal while its waves use the 3D bearing.
        float horizontalRange = ScanColor.w;
        float endFade = min(2.0, horizontalRange);
        intensity *= smoothstep(-3.5, -3.0, along)
        * (1.0 - smoothstep(horizontalRange - endFade, max(0.001, horizontalRange), along));
        // Give several pings time to reach even the far end before fading out.
        float duration = max(8.0, sweepDuration + 4.0);
        intensity *= 1.0 - smoothstep(duration - 2.0, duration, age);
    }
    intensity *= smoothstep(0.0, 0.12, age);
    if (intensity < 0.005) discard;
    // Dark scan colors keep a charcoal highlight instead of washing out to white.
    float brightness = max(ScanColor.r, max(ScanColor.g, ScanColor.b));
    fragColor = vec4(mix(ScanColor.rgb, vec3(brightness), clamp(intensity - 0.7, 0.0, 0.6)), min(intensity, 0.85));
}
