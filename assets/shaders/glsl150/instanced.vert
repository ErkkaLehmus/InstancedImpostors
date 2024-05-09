uniform vec3 u_dirLightColor;
uniform vec3 u_lightDir;
uniform vec3 u_ambientLight;
uniform mat3 u_normalMatrix;
uniform mat4 u_projViewTrans;

in vec3 a_position;
in vec4 a_color;
in vec3 a_normal;

#ifdef INSTANCED
    in vec3 i_worldTrans;
#else
    uniform mat4 u_worldTrans;
#endif

out vec3 v_lightDiffuse;
out vec3 v_ambientLight;
out vec4 v_color;
out vec3 v_normal;

void main () {
    v_color = a_color;
    //v_color.r = 0.95;
    vec3 normal;

    #ifdef INSTANCED
        //It took me a while to get the normals right,
        //but seems like it works simple as this
        //assuming that the 3D models are never rotated, only their position changes
        //so we can just use the normal values as they are
        normal = normalize(a_normal);
    #else
        normal = normalize(u_normalMatrix * a_normal);
    #endif

    //sunlight and ambient light
    float NdotL = clamp(dot(normal, -u_lightDir), 0.0, 1.0);
    vec3 value = u_dirLightColor * NdotL;
    v_lightDiffuse = value;

    v_ambientLight = u_ambientLight;

    #ifdef INSTANCED
        gl_Position = u_projViewTrans * vec4(a_position + i_worldTrans, 1.0);
    #else
        vec4 pos = u_worldTrans * vec4(a_position, 1.0);
        gl_Position = u_projViewTrans * pos;
    #endif
}
