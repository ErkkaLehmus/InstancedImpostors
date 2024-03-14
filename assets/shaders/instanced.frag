precision mediump float;

in vec3 v_ambientLight;
in vec3 v_lightDiffuse;
in vec4 v_color;

out vec4 FragColor;

void main () {

    vec4 baseColor = v_color;
    FragColor = vec4((baseColor.rgb * (v_ambientLight + v_lightDiffuse)),1);
}
