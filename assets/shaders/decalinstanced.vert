uniform mat4 u_projViewTrans;
in vec3 a_position;
in vec2 a_texCoords0;
in mat4 i_worldTrans;
in vec2 i_uvOffset;

out vec2 TexCoords;
out vec2 uvOffset;

void main () {
    TexCoords = a_texCoords0;
    uvOffset = i_uvOffset;
    gl_Position = u_projViewTrans * i_worldTrans * vec4(a_position, 1.0);
}
