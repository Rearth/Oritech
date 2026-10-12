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

// A fixed screen-space grid keeps the trailing specks crisp at any viewing distance.
// Cell variation breaks up the pattern without flickering or additional texture reads.
float scanDots() {
    vec2 cell = floor(gl_FragCoord.xy / 14.0);
    float variation = fract(sin(dot(cell, vec2(127.1, 311.7))) * 43758.5453);
    vec2 center = vec2(7.0) + vec2(variation - 0.5, fract(variation * 7.0) - 0.5) * 4.0;
    float distance = length(mod(gl_FragCoord.xy, 14.0) - center);
    return (1.0 - smoothstep(0.8, 1.8, distance)) * mix(0.55, 1.0, variation);
}

float scanTrail(float behind, float trailLength) {
    return smoothstep(0.3, 1.0, behind) * (1.0 - smoothstep(1.0, trailLength, behind));
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= 0.999999) discard;
    vec4 reconstructed = inverseViewProjection * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 offset = reconstructed.xyz / reconstructed.w - OriginAge.xyz;
    float age = OriginAge.w;
    float range = DirectionRange.z;
    float distance = length(offset);
    float dots = scanDots();
    // Slow, low-amplitude modulation gives the front a little texture without noisy edges.
    float variation = 1.0 + 0.15 * sin(offset.x * 0.7 + offset.z * 0.5 + age * 1.8)
    * sin(offset.y * 0.6 - offset.z * 0.3 - age);
    float intensity;
    if (DirectionRange.w > 0.5) {
        // A spherical shell sweeps vertically over walls as well as across floors.
        float radius = age / 4.0 * range;
        float edge = distance - radius;
        float width = 0.85 * variation + clamp(fwidth(distance), 0.0, 0.35);
        // Keep the bright core narrow, with a broad, low-intensity wake toward
        // the source and a crisp leading edge toward the unexplored area.
        float glowWidth = edge < 0.0 ? 8.0 * variation : width;
        float outerEdge = 1.0 - smoothstep(width * 0.8, width * 1.25, edge);
        intensity = (exp(-pow(edge / width, 2.0))
        + 0.28 * exp(-pow(edge / glowWidth, 2.0))) * outerEdge;
        // The dotted afterglow follows the shell by roughly a third of a second.
        intensity += dots * 0.32 * scanTrail(-edge, max(3.0, range / 4.0 * 0.35));
        intensity *= 1.0 - smoothstep(3.2, 4.0, age);
    } else {
        vec2 direction = DirectionRange.xy;
        float along = dot(offset.xz, direction);
        float across = abs(offset.x * direction.y - offset.z * direction.x);
        float width = max(0.08, fwidth(across));
        float line = 1.0 - smoothstep(0.2 * variation, 0.2 * variation + width, across);
        float glow = exp(-across * across / 0.8);
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
        float sweep = exp(-pow(edge / (2.2 * variation), 2.0));
        intensity = (line * 0.55 + glow * 0.15 + sweep * glow * 0.8);
        float trailLength = max(3.0, speed * 0.45);
        intensity += dots * 0.35 * exp(-across * across / 2.5) * scanTrail(-edge, trailLength);
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
