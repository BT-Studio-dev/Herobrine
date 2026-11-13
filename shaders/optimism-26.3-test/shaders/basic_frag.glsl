#version 330

/* DRAWBUFFERS:0 */
layout(location = 0) out vec4 outColor0; //colortex0 - outcolor

uniform sampler2D gtexture;
uniform sampler2D lightmap;

in vec2 texCoord;
in float DistanceFromCamera;
in vec3 foliageColor;
in vec2 fragLightmapCoords;

uniform mat4 gbufferModelViewInverse;
uniform float viewWidth;
uniform float viewHeight;
uniform float frameTimeCounter;
uniform int worldTime;
uniform vec3 shadowLightPosition;



// Hash function to generate pseudo-random numbers
float hash(float n) {
    return fract(((sin(n) * 43758.5453123) + (cos(n* 6.543293) * 23145.5123457))/2);
}

vec4 draw_eye(vec4 o_c, vec3 normalizedPos, float aspekt_ratio)
{
    float random_x = hash(int(frameTimeCounter*10)) *96;
    float random_y = fract(hash(int(frameTimeCounter*10)) *12762.23412) * 96;

    const vec4 eye_color = vec4(1,0,0,1);
    if (
        (normalizedPos.x >= 1.0/96.0*(42+random_x) && normalizedPos.x < 1.0/96.0*(42 + random_x + 2)) &&
        (normalizedPos.y >= 1.0/(96.0*aspekt_ratio)*((47 + random_y -30)) &&
        normalizedPos.y < 1.0/(96.0*aspekt_ratio)*((48.0 + random_y -30))))
    {
        o_c = (eye_color*eye_color.w) + (o_c*(1-eye_color.w));
    }
    else if (
        (normalizedPos.x >= 1.0/96.0*(46 + random_x) && normalizedPos.x < 1.0/96.0*(46 + random_x + 2)) &&
        (normalizedPos.y >= 1.0/(96.0*aspekt_ratio)*((47 + random_y-30)) &&
        normalizedPos.y < 1.0/(96.0*aspekt_ratio)*((48.0 + random_y-30))))
    {
        o_c = (eye_color*eye_color.w) + (o_c*(1-eye_color.w));
    }
    return o_c;
}




float calculateNightLevel(int time) {
    // Define the start and end points of the transition
    const int startTransition = 13000;
    const int peakTime = 18000;
    const int endTransition = 24000;

    // Initialize light level to 0
    float level = 0.0;

    // Calculate light level based on world time
    if (time >= startTransition && time <= peakTime) {
        level = smoothstep(float(startTransition), float(peakTime), float(time));
    } else if (time > peakTime && time <= endTransition) {
        level = 1.0 - smoothstep(float(peakTime), float(endTransition), float(time));
    }

    return level;
}



void main() {

    vec3 shadowlightdirection = normalize(mat3(gbufferModelViewInverse) * shadowLightPosition);


    vec3 normalizedPos = gl_FragCoord.xyz/vec3(viewWidth,viewHeight,1);
    float aspekt_ratio = viewHeight/viewWidth;
    vec4 lightcolor = pow(texture(lightmap,fragLightmapCoords),vec4(2.2));
    float min = pow(DistanceFromCamera,2.2);
    if (min < 4)
    {
        min = 4;
    }
    float nightlevel = calculateNightLevel(worldTime);
    float darknesslevel = 0;


    darknesslevel = (1-(lightcolor.x+lightcolor.y+lightcolor.z)/3.0)*nightlevel;

    vec4 color = texture(gtexture,texCoord)*vec4(foliageColor,1);

    min *= darknesslevel;
    if (min < 1)
    {
        min = 1;
    }
    if (color.a <= 0.1)
    {
        discard;
    }



    if (mod(int(hash(int(frameTimeCounter*5))*5), 5000) < nightlevel/2.0)
    {
        color = pow(vec4((color/min).xyz*(lightcolor.xyz/8),color.w),vec4(1.4));
    }
    else
    {
        color = color*lightcolor;
    }


    float avgcolor = (color.x + color.y + color.z) / 3.0;
    color = vec4((darknesslevel*vec3(avgcolor)) + (color.xyz*(1-darknesslevel)),color.w);


    //output color
    outColor0 = vec4((color/min).xyz,color.w);


    float timesined = hash(int(frameTimeCounter*25))*darknesslevel;
    if (timesined > 0.9)
    {
        outColor0 = ((1-timesined)*outColor0) + (timesined * draw_eye(outColor0, normalizedPos, aspekt_ratio));
    }

    outColor0 = ((1-darknesslevel)*outColor0) + (darknesslevel * pow(outColor0,vec4(2.2)));
}
