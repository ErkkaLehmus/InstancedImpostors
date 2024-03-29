precision mediump float;

in vec3 v_ambientLight;
in vec3 v_lightDiffuse;
in vec4 v_color;

out vec4 FragColor;

void main () {

    //vec4 baseColor = v_color;
    vec4 baseColor = vec4(pow(v_color.rgb,vec3(1.0/2.2)), v_color.a);
    FragColor = vec4((baseColor.rgb * (v_ambientLight + v_lightDiffuse)),1);
}
