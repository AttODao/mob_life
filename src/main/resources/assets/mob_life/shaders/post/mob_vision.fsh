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
    vec4 ConfigOverrides;
    vec4 DynamicRedResponse;
    vec4 DynamicGreenResponse;
    vec4 DynamicBlueResponse;
    vec4 VisionBehavior;
};

out vec4 fragColor;

const vec3 LUMINANCE_RESPONSE = vec3(0.2126, 0.7152, 0.0722);

float peripheralAmount() {
    vec2 edgePosition = abs(texCoord * 2.0 - 1.0);
    float roundedEdgeDistance = pow(
        pow(edgePosition.x, 4.0) + pow(edgePosition.y, 4.0),
        0.25
    );
    return smoothstep(VisionBehavior.x, 1.0, roundedEdgeDistance);
}

vec3 samplePeripheralBlur(float blurAmount) {
    if (blurAmount < 0.001 || ConfigOverrides.w < 0.001) {
        return texture(InSampler, texCoord).rgb;
    }

    vec2 offset = ConfigOverrides.w * blurAmount / InSize;
    vec3 color = texture(InSampler, texCoord).rgb * 0.2;
    color += texture(
        InSampler,
        texCoord + vec2(offset.x, 0.0)
    ).rgb * 0.12;
    color += texture(
        InSampler,
        texCoord - vec2(offset.x, 0.0)
    ).rgb * 0.12;
    color += texture(
        InSampler,
        texCoord + vec2(0.0, offset.y)
    ).rgb * 0.12;
    color += texture(
        InSampler,
        texCoord - vec2(0.0, offset.y)
    ).rgb * 0.12;
    color += texture(InSampler, texCoord + offset).rgb * 0.08;
    color += texture(InSampler, texCoord - offset).rgb * 0.08;
    color += texture(
        InSampler,
        texCoord + vec2(offset.x, -offset.y)
    ).rgb * 0.08;
    color += texture(
        InSampler,
        texCoord + vec2(-offset.x, offset.y)
    ).rgb * 0.08;
    return color;
}

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

float linearDepth(float depth) {
    float nearPlane = DepthRange.x;
    float farPlane = DepthRange.y;
    return nearPlane * farPlane / max(
        farPlane - depth * (farPlane - nearPlane),
        0.00001
    );
}

float playerDistance(float depth) {
    float forwardDistance = linearDepth(depth);
    vec2 centeredTexCoord = texCoord * 2.0 - 1.0;
    float aspect = InSize.x / max(InSize.y, 1.0);
    float tanHalfFov = max(VisionBehavior.z, 0.001);
    vec2 rayOffset = centeredTexCoord * vec2(aspect * tanHalfFov, tanHalfFov);
    return forwardDistance * length(vec3(rayOffset, 1.0));
}

void main() {
    vec4 sourceColor = texture(InSampler, texCoord);
    if (DepthRange.z < 0.5) {
        float peripheral = peripheralAmount();
        sourceColor.rgb = samplePeripheralBlur(peripheral);
        vec3 shiftedColor = vec3(
            dot(
                sourceColor.rgb,
                mix(RedResponse.rgb, DynamicRedResponse.rgb, VisionBehavior.w)
            ),
            dot(
                sourceColor.rgb,
                mix(GreenResponse.rgb, DynamicGreenResponse.rgb, VisionBehavior.w)
            ),
            dot(
                sourceColor.rgb,
                mix(BlueResponse.rgb, DynamicBlueResponse.rgb, VisionBehavior.w)
            )
        );
        float luminance = dot(
            shiftedColor,
            LUMINANCE_RESPONSE
        );
        float interference = DepthRange.w;
        float retainedSaturation = ConfigOverrides.x
            * mix(1.0, 0.65, interference);
        vec3 baseVisionColor = mix(
            vec3(luminance),
            shiftedColor,
            retainedSaturation
        );
        float baseContrast = ConfigOverrides.y
            * mix(1.0, 0.85, interference);
        float baseBrightness = ConfigOverrides.z
            * mix(1.0, 0.82, interference);
        baseVisionColor = (baseVisionColor - 0.5) * baseContrast + 0.5;
        baseVisionColor = max(baseVisionColor, vec3(0.0));
        float transformedLuminance = max(
            dot(baseVisionColor, LUMINANCE_RESPONSE),
            0.001
        );
        float sourceLuminance = dot(sourceColor.rgb, LUMINANCE_RESPONSE);
        float lowLightBrightness = mix(
            VisionBehavior.y,
            1.0,
            Effects.y
        );
        baseVisionColor *= sourceLuminance
            * baseBrightness
            * lowLightBrightness
            / transformedLuminance;
        baseVisionColor *= mix(1.0, Effects.z, peripheral);
        fragColor = vec4(
            clamp(baseVisionColor, 0.0, 1.0),
            sourceColor.a
        );
        return;
    }

    float depth = texture(DepthSampler, texCoord).r;
    float fragmentDistance = playerDistance(depth);
    float blurAmount = fragmentDistance <= Parameters.x
        ? 0.0
        : smoothstep(Parameters.x, Parameters.y, fragmentDistance);
    float darkAmount = fragmentDistance <= Parameters.x
        ? 0.0
        : smoothstep(Parameters.x, Parameters.z, fragmentDistance);
    float fogAmount = fragmentDistance <= Parameters.x
        ? 0.0
        : smoothstep(Parameters.x, Parameters.w, fragmentDistance);
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
