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

const mat3 birdMatrix = mat3(1.0,0.0,0.0,0.0,0.0,1.0,0.0,-1.0,0.0);

const float PI = 3.1415927;
const float HALF_PI = 1.570796;
const float MINIMUM_ANGLE_RAD = 0.52;
float tmpFloat;
float tmpStepX;
float tmpStepY;
float angleY;
float angleX;
float moveY;

mat3 calcLookAtMatrix(vec3 origin, vec3 target) {
    vec3 rr = vec3(0.0, 1.0, 0.0);
    vec3 ww = normalize(target - origin);
    vec3 uu = normalize(cross(ww, rr));
    vec3 vv = normalize(cross(uu, ww));

    return mat3(uu, vv, ww);
}


vec2 getUVoffset()
{
    float dist = distance(u_camPos.xz,i_worldTrans.xz);
    angleX = atan(u_camPos.y - i_worldTrans.y,dist);
    //float angleX = atan(rotationMatrix[1][2],rotationMatrix[2][2])-HALF_PI;
    //float angleX = atan(-rotationMatrix[0][2],sqrt( (rotationMatrix[1][2] * rotationMatrix[1][2]) + (rotationMatrix[2][2] * rotationMatrix[2][2]) ))+HALF_PI;
    angleY = acos(dot(normalize(vec2(i_worldTrans.x,u_camPos.x)),normalize(vec2(i_worldTrans.z,u_camPos.z))));

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

    uvOffset = getUVoffset();

    mat3 impostorRotationMatrix;

    moveY = sin(angleX + HALF_PI) * u_moveY;

    if (tmpStepX >= u_uvSteps.x - 3.0)
    {
        //the decal is seen  from above, no need to rotate around y axis, we simply tilt the decal -90 degrees around X axis
        impostorRotationMatrix = birdMatrix;
        //moveY = 1.5 * moveY;
    }
    else
        //rotate the decal to look at the camera
        impostorRotationMatrix = calcLookAtMatrix(i_worldTrans,u_camPos);

    vec4 position_corrected = vec4( impostorRotationMatrix * a_position,0.0);

    position_corrected.y = position_corrected.y + moveY;
    gl_Position = u_projViewTrans * (position_corrected + vec4(i_worldTrans, 1.0));
}
