uniform mat4 u_projViewTrans;
uniform vec3 u_camPos;

uniform vec2 u_uvStepSize;
uniform vec2 u_uvSteps;
uniform vec2 u_uvSize;
uniform float u_moveY;

in vec3 a_position;
in vec2 a_texCoords0;
in vec3 i_worldTrans;

out vec2 TexCoords;
out vec2 uvOffset;

float PI = 3.1415927;
float HALF_PI = 1.570796;
float MINIMUM_ANGLE_RAD = 0.52;
float tmpFloat;
float tmpStepX;
float moveY;

mat3 calcLookAtMatrix(vec3 origin, vec3 target) {
    vec3 rr = vec3(0.0, 1.0, 0.0);
    vec3 ww = normalize(target - origin);
    vec3 uu = normalize(cross(ww, rr));
    vec3 vv = normalize(cross(uu, ww));
    return mat3(uu, vv, ww);
}


vec2 getUVoffset(mat3 rotationMatrix)
{
    float dist = distance(u_camPos.xz,i_worldTrans.xz);
    float angleX = atan(u_camPos.y - i_worldTrans.y,dist);
    //float angleX = atan(rotationMatrix[1][2],rotationMatrix[2][2])-HALF_PI;
    //float angleX = atan(-rotationMatrix[0][2],sqrt( (rotationMatrix[1][2] * rotationMatrix[1][2]) + (rotationMatrix[2][2] * rotationMatrix[2][2]) ))+HALF_PI;
    float angleY = acos(dot(normalize(vec2(i_worldTrans.x,u_camPos.x)),normalize(vec2(i_worldTrans.z,u_camPos.z))));

    float tmpStepX;
    float tmpStepY;
    float tmpFloat1;
    float tmpFloat2;

    if (angleX > HALF_PI)
      angleX = HALF_PI - (angleX - HALF_PI);
    if (angleX < MINIMUM_ANGLE_RAD) {
        tmpFloat1 = 0.0;
        tmpStepX = 0.0;
    }
    else
    {
        tmpStepX = float(round((angleX - MINIMUM_ANGLE_RAD) / u_uvStepSize.x));
        if (tmpStepX >= u_uvSteps.x) tmpStepX = u_uvSteps.x-1.0;
        tmpFloat1 =  tmpStepX * u_uvSize.x;
    }

    tmpStepY = float(round((angleY) / u_uvStepSize.y));
    if (tmpStepY >= u_uvSteps.y) tmpStepY = u_uvSteps.y-1.0;
    tmpFloat2 =  tmpStepY * u_uvSize.y;

    return vec2(tmpFloat1,tmpFloat2);
}

void main () {
    moveY = u_moveY;
    TexCoords = a_texCoords0;
    mat3 impostorRotationMatrix = calcLookAtMatrix(i_worldTrans,u_camPos);
    uvOffset = getUVoffset(impostorRotationMatrix);
    vec4 position_corrected = vec4( impostorRotationMatrix * vec3(a_position.x,a_position.y+moveY,a_position.z),0.0);
    gl_Position = u_projViewTrans * (position_corrected + vec4(i_worldTrans, 1.0));
}
