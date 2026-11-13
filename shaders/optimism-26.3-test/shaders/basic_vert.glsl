#version 330

//attributes
in vec3 vaPosition;
in vec2 vaUV0;
in ivec2 vaUV2;
in vec4 vaColor;
in vec3 vaNormal;


//uniforms
uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;
uniform vec3 chunkOffset;
uniform vec3 cameraPosition;
uniform mat4 gbufferModelViewInverse;
uniform mat3 normalMatrix;

out vec2 texCoord;
out vec2 fragLightmapCoords;
out float DistanceFromCamera;
out vec3 foliageColor;
out vec3 geonormal;

void main() {
    geonormal = vaNormal*normalMatrix;
    texCoord = vaUV0;
    foliageColor = vaColor.xyz;

    vec3 WorldSpaceCameraPosition = cameraPosition + (gbufferModelViewInverse * modelViewMatrix * vec4(vaPosition+chunkOffset,1)).xyz;

    DistanceFromCamera = distance(cameraPosition,WorldSpaceCameraPosition);


    gl_Position = projectionMatrix * modelViewMatrix * vec4(vaPosition+chunkOffset/*- (0.02f * DistanceFromCamera)*/,1);

    fragLightmapCoords = vaUV2 * (1.0/256.0) + (1.0/32.0);
}
