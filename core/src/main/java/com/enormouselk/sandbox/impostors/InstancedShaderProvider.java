package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import javax.swing.text.AttributeSet;

public class InstancedShaderProvider extends DefaultShaderProvider {

    DefaultShader.Config config;

    String vertexShader;
    String fragmentShader;


    public InstancedShaderProvider(DefaultShader.Config config) {
        super(config);
        this.config = config;
        vertexShader = Gdx.files.internal("shaders/instanced.vert").readString();
        fragmentShader = Gdx.files.internal("shaders/instanced.frag").readString();
    }


    @Override
    protected Shader createShader(Renderable renderable) {

        if (renderable.meshPart.mesh.isInstanced()) {
            return createInstancedShader(renderable);
        } else {
            return createPlainShader(renderable);
        }
        //return super.createShader(renderable);
    }

    @Override
    public void dispose() {
        super.dispose();
        //if (snowShader != null) snowShader.dispose();
    }

    public BaseShader createPlainShader(Renderable renderable) {

        return new DefaultShader(renderable);
    }

    public BaseShader createInstancedShader(Renderable renderable) {
        return new BaseShader() {

            @Override
            public void begin(Camera camera, RenderContext context) {
                program.bind();
                program.setUniformMatrix("u_projViewTrans", camera.combined);
                context.setDepthTest(GL30.GL_LEQUAL);

                ColorAttribute ambientLight = (ColorAttribute) renderable.environment.get(ColorAttribute.AmbientLight);
                DirectionalLightsAttribute dirLights = (DirectionalLightsAttribute) renderable.environment.get(DirectionalLightsAttribute.Type);

                //if ambient light was not found, use hardcoded default
                //this is for the demo version only
                //would be better to handle with ifdef in the shader code
                if (ambientLight != null) {
                    float[] col = new float[]{ambientLight.color.r,ambientLight.color.g,ambientLight.color.b};
                    program.setUniform3fv("u_ambientLight", col, 0, 3);
                }
                else {
                    program.setUniform3fv("u_ambientLight", new float[]{0.95f, 0.75f, 0.77f}, 0, 3);
                }

                //same for the directional light

                if ((dirLights!= null) && (!dirLights.lights.isEmpty()))
                {
                    DirectionalLight lit = dirLights.lights.get(0);
                    float[] col = new float[]{lit.color.r,lit.color.g,lit.color.b};
                    float[] dir = new float[]{lit.direction.x,lit.direction.y,lit.direction.z};
                    program.setUniform3fv("u_dirLightColor", col, 0, 3);
                    program.setUniform3fv("u_lightDir",dir, 0, 3);
                }
                else
                {
                    float lightDirY = MathUtils.sinDeg(50);
                    float lightDirZ = MathUtils.cosDeg(50);

                    program.setUniform3fv("u_dirLightColor", new float[]{1f, 1f, 0.75f}, 0, 3);
                    program.setUniform3fv("u_lightDir", new float[]{0, lightDirY, lightDirZ}, 0, 3);
                }


            }

            @Override
            public void init() {
                ShaderProgram.prependVertexCode = "#version 300 es\n#define INSTANCED\n";
                ShaderProgram.prependFragmentCode = "#version 300 es\n";
                program = new ShaderProgram(Gdx.files.internal("shaders/instanced.vert"),
                    Gdx.files.internal("shaders/instanced.frag"));
                if (!program.isCompiled()) {
                    throw new GdxRuntimeException("Shader compile error: " + program.getLog());
                }
                init(program, renderable);
            }

            @Override
            public int compareTo(Shader other) {
                return 0;
            }

            @Override
            public boolean canRender(Renderable instance) {
                return true;
            }
        };
    }

    public BaseShader createDecalShader(Renderable renderable) {
        return new BaseShader() {

            @Override
            public void begin(Camera camera, RenderContext context) {
                program.bind();
                program.setUniformMatrix("u_projViewTrans", camera.combined);
                program.setUniformi("u_texture", 0);
                context.setDepthTest(GL30.GL_LEQUAL);
            }

            @Override
            public void init() {
                ShaderProgram.prependVertexCode = "#version 300 es\n";
                ShaderProgram.prependFragmentCode = "#version 300 es\n";
                program = new ShaderProgram(Gdx.files.internal("shaders/decalinstanced.vert"),
                    Gdx.files.internal("shaders/decalinstanced.frag"));
                if (!program.isCompiled()) {
                    throw new GdxRuntimeException("Shader compile error: " + program.getLog());
                }
                init(program, renderable);
            }

            @Override
            public int compareTo(Shader other) {
                return 0;
            }

            @Override
            public boolean canRender(Renderable instance) {
                return true;
            }
        };
    }


}
