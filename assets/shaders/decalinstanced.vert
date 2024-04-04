uniform mat4 u_projViewTrans;
in vec3 a_position;
in vec2 a_texCoords0;
#ifdef CPU
in mat4 i_worldTrans;
in vec2 i_uvOffset;
#else
in vec3 i_worldTrans;
uniform mat4 u_impostorRotationMatrix;
uniform float u_moveY;
uniform vec2 u_uvOffset;
#endif

out vec2 TexCoords;
out vec2 uvOffset;

void main () {
    TexCoords = a_texCoords0;

    #ifdef CPU
    uvOffset = i_uvOffset;
    gl_Position = u_projViewTrans * i_worldTrans * vec4(a_position, 1.0);
    #else
    uvOffset = u_uvOffset;
    vec4 position_corrected = u_impostorRotationMatrix * vec4(a_position.x,a_position.y+u_moveY,a_position.z,0);
    gl_Position = u_projViewTrans * (position_corrected + vec4(i_worldTrans, 1.0));
    #endif
}
