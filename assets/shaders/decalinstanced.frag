precision mediump float;

uniform sampler2D u_texture;
in vec2 TexCoords;
in vec2 uvOffset;
out vec4 FragColor;


void main () {
    vec4 color = texture(u_texture, TexCoords+uvOffset);
    if (color.a < 0.95) discard;
    FragColor = color;
}
