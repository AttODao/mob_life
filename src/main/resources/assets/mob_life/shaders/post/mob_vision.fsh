#version 330

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;

layout(std140) uniform VisionConfig {
    vec4 RedResponse;
    vec4 GreenResponse;
    vec4 BlueResponse;
    vec4 Settings;
};

layout(std140) uniform DistanceBlur {
    vec4 Parameters;
    vec4 Effects;
    vec4 DepthRange;
};

out vec4 fragColor;

vec3 sampleDistanceBlur(float blurAmount) {
    if (blurAmount < 0.001) {
        return texture(InSampler, texCoord).rgb;
    }

    vec2 offset = Effects.x * blurAmount / InSize;
    vec3 color = texture(InSampler, texCoord).rgb * 0.1;

    color += texture(InSampler, texCoord + vec2( offset.x, 0.0)).rgb * 0.1;
    color += texture(InSampler, texCoord + vec2(-offset.x, 0.0)).rgb * 0.1;
    color += texture(InSampler, texCoord + vec2(0.0,  offset.y)).rgb * 0.1;
    color += texture(InSampler, texCoord + vec2(0.0, -offset.y)).rgb * 0.1;

    color += texture(InSampler, texCoord + vec2( offset.x,  offset.y)).rgb * 0.075;
    color += texture(InSampler, texCoord + vec2(-offset.x,  offset.y)).rgb * 0.075;
    color += texture(InSampler, texCoord + vec2( offset.x, -offset.y)).rgb * 0.075;
    color += texture(InSampler, texCoord + vec2(-offset.x, -offset.y)).rgb * 0.075;

    vec2 farOffset = offset * 2.0;
    color += texture(InSampler, texCoord + vec2( farOffset.x, 0.0)).rgb * 0.05;
    color += texture(InSampler, texCoord + vec2(-farOffset.x, 0.0)).rgb * 0.05;
    color += texture(InSampler, texCoord + vec2(0.0,  farOffset.y)).rgb * 0.05;
    color += texture(InSampler, texCoord + vec2(0.0, -farOffset.y)).rgb * 0.05;
    return color;
}

void main() {
    vec4 sourceColor = texture(InSampler, texCoord);
    if (DepthRange.z < 0.5) {
        vec3 shiftedColor = vec3(
            dot(sourceColor.rgb, RedResponse.rgb),
            dot(sourceColor.rgb, GreenResponse.rgb),
            dot(sourceColor.rgb, BlueResponse.rgb)
        );
        float luminance = dot(
            shiftedColor,
            vec3(0.2126, 0.7152, 0.0722)
        );
        float interference = DepthRange.w;
        float retainedSaturation = Settings.x
            * mix(1.0, 0.65, interference);
        vec3 baseVisionColor = mix(
            vec3(luminance),
            shiftedColor,
            retainedSaturation
        );
        float baseContrast = Settings.y
            * mix(1.0, 0.85, interference);
        float baseBrightness = Settings.z
            * mix(1.0, 0.82, interference);
        baseVisionColor = (baseVisionColor - 0.5) * baseContrast + 0.5;
        baseVisionColor *= baseBrightness;
        fragColor = vec4(
            clamp(baseVisionColor, 0.0, 1.0),
            sourceColor.a
        );
        return;
    }

    float depth = texture(DepthSampler, texCoord).r;
    float nearPlane = DepthRange.x;
    float farPlane = DepthRange.y;
    float linearDistance = nearPlane * farPlane / max(
        farPlane - depth * (farPlane - nearPlane),
        0.00001
    );
    float blurAmount = linearDistance <= Parameters.x
        ? 0.0
        : smoothstep(Parameters.x, Parameters.y, linearDistance);
    float darkAmount = linearDistance <= Parameters.x
        ? 0.0
        : smoothstep(Parameters.x, Parameters.z, linearDistance);
    float fogAmount = linearDistance <= Parameters.x
        ? 0.0
        : smoothstep(Parameters.x, Parameters.w, linearDistance);
    vec3 sourceRgb = sampleDistanceBlur(blurAmount);
    vec3 darkenedColor = (sourceRgb - 0.5) * Effects.y + 0.5;
    darkenedColor *= Effects.z;
    vec3 distantColor = mix(sourceRgb, darkenedColor, darkAmount);
    float extraInterference = clamp(DepthRange.w - 1.0, 0.0, 1.0);
    vec3 hazeColor = mix(
        vec3(0.32, 0.34, 0.36),
        vec3(0.20, 0.21, 0.23),
        extraInterference
    );
    vec3 finalColor = mix(
        distantColor,
        hazeColor,
        fogAmount * Effects.w
    );
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), sourceColor.a);
}
