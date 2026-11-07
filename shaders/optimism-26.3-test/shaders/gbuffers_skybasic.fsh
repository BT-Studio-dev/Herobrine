#version 330 compatibility

uniform float viewHeight;
uniform float viewWidth;
uniform mat4 gbufferModelView;
uniform mat4 gbufferProjectionInverse;
uniform vec3 fogColor;
uniform vec3 skyColor;
uniform float frameTimeCounter;
uniform int worldTime;

in vec4 starData; //rgb = star color, a = flag for weather or not this pixel is a star.

float fogify(float x, float w) {
	return w / (x * x + w);
}

vec3 calcSkyColor(vec3 pos) {
	float upDot = dot(pos, gbufferModelView[1].xyz); //not much, what's up with you?
	return mix(skyColor, fogColor, fogify(max(upDot, 0.0), 0.25));
}

vec3 screenToView(vec3 screenPos) {
	vec4 ndcPos = vec4(screenPos, 1.0) * 2.0 - 1.0;
	vec4 tmp = gbufferProjectionInverse * ndcPos;
	return tmp.xyz / tmp.w;
}
// Hash function to generate pseudo-random numbers
float hash(float n) {
    return fract(((sin(n) * 43758.5453123) + (cos(n* 6.543293) * 23145.5123457))/2);
}

vec4 draw_breath(vec4 o_c, vec3 normalizedPos, float aspekt_ratio)
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

/* DRAWBUFFERS:01 */
layout(location = 0) out vec4 color;
layout(location = 1) out vec4 outColor1; //colortex1 - specular

void main() {
	outColor1 = vec4(0.0, 0.0, 0.0, 1.0);
	if (starData.a > 0.5) {
		color = vec4(starData.rgb, 1.0);
	} else {
		vec3 pos = screenToView(vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), 1.0));
		vec3 albedo = calcSkyColor(normalize(pos));
		color = vec4(albedo, 1.0);
	}
	float min = 1000;
    if (min < 4)
    {
        min = 4;
    }
    float nightlevel = calculateNightLevel(worldTime);
    float lightlevel = 0;
    float darknesslevel = 0;

    darknesslevel = ((1-lightlevel)*nightlevel) + (lightlevel*lightlevel);


    min *= darknesslevel;
    if (min < 1)
    {
        min = 1;
    }
    if (color.a <= 0.1)
    {
        discard;
    }
    float avgcolor = (color.x + color.y + color.z) / 3.0;
    color = vec4((darknesslevel*vec3(avgcolor)) + (color.xyz*(1-darknesslevel)),color.w);
    if (nightlevel > 0)
    {
        if (mod(int(hash(int(frameTimeCounter*10))*50000), 500) == 0)
        {
        //output color
        color = vec4(0,0,0,color.w);
        return;
        }
    }


    //output color
    color = pow(vec4((color/min).xyz,color.w),vec4(1.4));


    vec3 normalizedPos = gl_FragCoord.xyz/vec3(viewWidth,viewHeight,1);
    float aspekt_ratio = viewHeight/viewWidth;
    float timesined = hash(int(frameTimeCounter*100))*darknesslevel;
    if (timesined > 0.97)
    {
        color = ((1-timesined)*color) + (timesined * draw_breath(color, normalizedPos, aspekt_ratio));
    }
}
