#version 150

uniform sampler2D DiffuseSampler;
uniform float Opacity;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 outline = texture(DiffuseSampler, texCoord);
    float brightness = max(max(outline.r, outline.g), outline.b);
    vec3 color = outline.rgb / max(brightness, 0.001);
    fragColor = vec4(color, clamp(outline.a, 0.0, 1.0) * Opacity);
}
