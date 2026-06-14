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

const vec3 LUMINANCE_RESPONSE = vec3(0.2126, 0.7152, 0.0722);

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
            LUMINANCE_RESPONSE
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
        baseVisionColor = max(baseVisionColor, vec3(0.0));
        float transformedLuminance = max(
            dot(baseVisionColor, LUMINANCE_RESPONSE),
            0.001
        );
        float sourceLuminance = dot(sourceColor.rgb, LUMINANCE_RESPONSE);
        baseVisionColor *= sourceLuminance
            * baseBrightness
            / transformedLuminance;
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
    float extraInterference = clamp(DepthRange.w - 1.0, 0.0, 1.0);
    float skyBrightness = Effects.y;
    float distantBrightness = mix(0.30, 0.52, skyBrightness)
        * mix(1.0, 0.7, extraInterference);
    vec3 darkenedColor = sourceRgb * distantBrightness;
    vec3 distantColor = mix(sourceRgb, darkenedColor, darkAmount);
    vec3 hazeTint = mix(
        vec3(0.32, 0.34, 0.36),
        vec3(0.20, 0.21, 0.23),
        extraInterference
    );
    float hazeBrightness = mix(0.05, 0.72, skyBrightness);
    vec3 hazeColor = hazeTint * hazeBrightness;
    vec3 finalColor = mix(
        distantColor,
        hazeColor,
        fogAmount * Effects.w
    );
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), sourceColor.a);
}
