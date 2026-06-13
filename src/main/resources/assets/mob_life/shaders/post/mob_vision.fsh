#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform VisionConfig {
    vec4 RedResponse;
    vec4 GreenResponse;
    vec4 BlueResponse;
    vec4 Settings;
};

out vec4 fragColor;

void main() {
    vec4 sourceColor = texture(InSampler, texCoord);
    vec3 shiftedColor = vec3(
        dot(sourceColor.rgb, RedResponse.rgb),
        dot(sourceColor.rgb, GreenResponse.rgb),
        dot(sourceColor.rgb, BlueResponse.rgb)
    );

    float luminance = dot(shiftedColor, vec3(0.2126, 0.7152, 0.0722));
    vec3 lowSaturationColor = mix(vec3(luminance), shiftedColor, Settings.x);
    lowSaturationColor = (lowSaturationColor - 0.5) * Settings.y + 0.5;
    lowSaturationColor *= Settings.z;

    fragColor = vec4(clamp(lowSaturationColor, 0.0, 1.0), sourceColor.a);
}
