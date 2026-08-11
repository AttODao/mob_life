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
const vec3 INSTINCT_GLOBAL_TINT = vec3(1.42, 0.93, 0.48);
const vec3 INSTINCT_EDGE_TINT = vec3(1.35, 0.68, 0.14);

float peripheralAmount() {
    vec2 edgePosition = abs(texCoord * 2.0 - 1.0);
    float roundedEdgeDistance = pow(
        pow(edgePosition.x, 4.0) + pow(edgePosition.y, 4.0),
        0.25
    );
    return smoothstep(VisionBehavior.x, 1.0, roundedEdgeDistance);
}

float instinctFrameAmount() {
    vec2 edgePosition = abs(texCoord * 2.0 - 1.0);
    float roundedEdgeDistance = pow(
        pow(edgePosition.x, 4.0) + pow(edgePosition.y, 4.0),
        0.25
    );
    return smoothstep(0.50, 1.0, roundedEdgeDistance);
}

vec3 tintPreservingLuminance(vec3 color, vec3 tint) {
    float luminance = dot(color, LUMINANCE_RESPONSE);
    vec3 tintedColor = color * tint;
    float tintedLuminance = max(dot(tintedColor, LUMINANCE_RESPONSE), 0.001);
    return tintedColor * luminance / tintedLuminance;
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

float lowLightSensitivity() {
    return smoothstep(1.30, 1.55, VisionBehavior.y);
}

float localDarkness(float luminance) {
    return 1.0 - smoothstep(0.04, 0.45, luminance);
}

float adaptedDarkness(float luminance) {
    float skyDarkness = 1.0 - clamp(Effects.y, 0.0, 1.0);
    return localDarkness(luminance) * mix(lowLightSensitivity(), 1.0, skyDarkness);
}

float brightLightSignal(vec3 color) {
    return smoothstep(0.18, 0.82, dot(color, LUMINANCE_RESPONSE));
}

void main() {
    vec4 sourceColor = texture(InSampler, texCoord);
    float instinctStrength = clamp(DynamicGreenResponse.w, 0.0, 1.0);
    if (DepthRange.z < 0.5) {
        float peripheral = peripheralAmount();
        sourceColor.rgb = samplePeripheralBlur(
            peripheral * (1.0 - 0.15 * instinctStrength)
        );
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
        float darkness = adaptedDarkness(sourceLuminance);
        float lowLightBrightness = mix(1.0, VisionBehavior.y, darkness);
        float lightResponse = brightLightSignal(sourceColor.rgb);
        float sensitivityBoost = 1.0
            + lowLightSensitivity() * darkness * (0.18 + 0.32 * lightResponse);
        baseVisionColor *= sourceLuminance
            * baseBrightness
            * lowLightBrightness
            * sensitivityBoost
            / transformedLuminance;
        float instinctFrame = instinctFrameAmount() * instinctStrength;
        vec3 globalInstinctColor = tintPreservingLuminance(baseVisionColor, INSTINCT_GLOBAL_TINT);
        baseVisionColor = mix(baseVisionColor, globalInstinctColor, instinctStrength * 0.40);
        vec3 edgeInstinctColor = tintPreservingLuminance(baseVisionColor, INSTINCT_EDGE_TINT);
        baseVisionColor = mix(baseVisionColor, edgeInstinctColor, instinctFrame * 0.72);
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
    vec3 sharpSourceRgb = texture(InSampler, texCoord).rgb;
    float hasWorldDepth = 1.0 - step(0.99999, depth);
    vec3 sourceRgb = sampleDistanceBlur(blurAmount * (1.0 - 0.15 * instinctStrength));
    float blurredLuminance = dot(sourceRgb, LUMINANCE_RESPONSE);
    float lightSensitivity = lowLightSensitivity()
        * hasWorldDepth
        * localDarkness(blurredLuminance);
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
    float sharpLuminance = dot(sharpSourceRgb, LUMINANCE_RESPONSE);
    vec2 pixel = 1.0 / InSize;
    float neighborLuminance0 = dot(
        texture(InSampler, texCoord + vec2( pixel.x, 0.0)).rgb,
        LUMINANCE_RESPONSE
    );
    float neighborLuminance1 = dot(
        texture(InSampler, texCoord + vec2(-pixel.x, 0.0)).rgb,
        LUMINANCE_RESPONSE
    );
    float neighborLuminance2 = dot(
        texture(InSampler, texCoord + vec2(0.0,  pixel.y)).rgb,
        LUMINANCE_RESPONSE
    );
    float neighborLuminance3 = dot(
        texture(InSampler, texCoord + vec2(0.0, -pixel.y)).rgb,
        LUMINANCE_RESPONSE
    );
    float neighborMax = max(
        max(neighborLuminance0, neighborLuminance1),
        max(neighborLuminance2, neighborLuminance3)
    );
    float localPeak = smoothstep(
        0.015,
        0.12,
        sharpLuminance - neighborMax
    );
    float brightCore = smoothstep(0.50, 0.82, sharpLuminance);
    float pointContrast = smoothstep(
        0.08,
        0.28,
        sharpLuminance - blurredLuminance
    );
    float sourceLight = brightCore * pointContrast * localPeak;
    float distanceEffect = max(darkAmount, fogAmount);
    float distantLightVisibility = clamp(
        lightSensitivity * sourceLight * distanceEffect,
        0.0,
        0.9
    );
    vec3 visibleLightColor = sharpSourceRgb * (1.25 + 0.45 * lightSensitivity);
    finalColor = mix(
        finalColor,
        max(finalColor, visibleLightColor),
        distantLightVisibility
    );
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), sourceColor.a);
}
