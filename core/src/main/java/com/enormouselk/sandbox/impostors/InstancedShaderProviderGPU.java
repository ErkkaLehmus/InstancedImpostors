package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL32;
import com.badlogic.gdx.graphics.g3d.Environment;
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
import com.badlogic.gdx.utils.GdxRuntimeException;

public class InstancedShaderProviderGPU extends DefaultShaderProvider implements InstancedShaderProvider {

    DefaultShader.Config config;

    String vertexShader;
    String fragmentShader;

    InstancedShaderProviderGPU.InstancedShader instancedShader;
    InstancedShaderProviderGPU.ImpostorShader impostorShader;
    InstancedShaderProviderGPU.ImpostorShaderGPUheavy impostorShaderGPUheavy;


    public InstancedShaderProviderGPU(DefaultShader.Config config) {
        super(config);
        this.config = config;
        vertexShader = Gdx.files.internal("shaders/instanced.vert").readString();
        fragmentShader = Gdx.files.internal("shaders/instanced.frag").readString();
    }


    @Override
    public Shader createShader(Renderable renderable) {
        if (renderable.meshPart.mesh.isInstanced()) {
            return createInstancedShader(renderable);
        } else {

            return createPlainShader(renderable);
        }
    }


    @Override
    public void dispose() {
        super.dispose();

        ShaderProgram.prependVertexCode = "";
        ShaderProgram.prependFragmentCode = "";


        //if (snowShader != null) snowShader.dispose();
    }

    @Override
    public BaseShader createPlainShader(Renderable renderable) {

        //return new DefaultShader(renderable);

        return new BaseShader() {

            private final Matrix3 tmpM = new Matrix3();

            @Override
            public void begin(Camera camera, RenderContext context) {
                program.bind();
                program.setUniformMatrix("u_projViewTrans", camera.combined);
                context.setDepthTest(GL32.GL_LESS);
                //context.setDepthTest(GL30.GL_LEQUAL);

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
                ShaderProgram.prependVertexCode = "#version 300 es\n";
                ShaderProgram.prependFragmentCode = "#version 300 es\n";

                program = new ShaderProgram(vertexShader,fragmentShader);
                /*
                program = new ShaderProgram(Gdx.files.internal("shaders/instanced.vert"),
                        Gdx.files.internal("shaders/instanced.frag"));

                 */
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
                return !instance.meshPart.mesh.isInstanced();
            }

            @Override
            public void render(Renderable renderable) {

                tmpM.set(renderable.worldTransform);

                program.setUniformMatrix("u_worldTrans", renderable.worldTransform);
                program.setUniformMatrix("u_normalMatrix", tmpM.inv().transpose());
                super.render(renderable);
            }
        };

    }

    @Override
    public BaseShader createInstancedShader(Renderable renderable) {
        return new BaseShader() {

            @Override
            public void begin(Camera camera, RenderContext context) {
                program.bind();
                program.setUniformMatrix("u_projViewTrans", camera.combined);
                context.setDepthTest(GL32.GL_LESS);
                //context.setDepthTest(GL30.GL_LEQUAL);

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

                program = new ShaderProgram(vertexShader,fragmentShader);

                /*
                program = new ShaderProgram(Gdx.files.internal("shaders/instanced.vert"),
                    Gdx.files.internal("shaders/instanced.frag"));

                 */
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
                return instance.meshPart.mesh.isInstanced();
            }
        };
    }



    @Override
    public BaseShader createDecalShader(Renderable renderable) {
        return new BaseShader() {



            @Override
            public void begin(Camera camera, RenderContext context) {
                program.bind();
                program.setUniformMatrix("u_projViewTrans", camera.combined);
                //program.setUniformMatrix("u_impostorRotationMatrix", renderable.worldTransform);

                MapChunk.DecalTransform decalTransform = (MapChunk.DecalTransform) renderable.userData;

                program.setUniformMatrix("u_impostorRotationMatrix", decalTransform.transform);
                program.setUniformf("u_moveY", decalTransform.moveY);
                program.setUniform2fv("u_uvOffset", decalTransform.uvOffset,0,2);
                program.setUniformi("u_texture", 0);
                context.setDepthTest(GL32.GL_LESS);
                //context.setDepthTest(GL30.GL_LEQUAL);
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


    public static class InstancedShader extends BaseShader
    {
        private Renderable renderable;

        public void setRenderable(Renderable renderable) {
            this.renderable = renderable;
        }

        @Override
        public void begin(Camera camera, RenderContext context) {
            program.bind();
            context.setDepthTest(GL32.GL_LEQUAL);

            program.setUniformMatrix("u_projViewTrans", camera.combined);

            //context.setDepthTest(GL30.GL_LEQUAL);


        }

        public void setEnvironment(Environment environment)
        {
            ColorAttribute ambientLight = (ColorAttribute) environment.get(ColorAttribute.AmbientLight);
            DirectionalLightsAttribute dirLights = (DirectionalLightsAttribute) environment.get(DirectionalLightsAttribute.Type);

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

            //program = new ShaderProgram(vertexShader,fragmentShader);


                program = new ShaderProgram(Gdx.files.internal("shaders/instanced.vert"),
                    Gdx.files.internal("shaders/instanced.frag"));


            if (!program.isCompiled()) {
                throw new GdxRuntimeException("Shader compile error: " + program.getLog());
            }
            init(program, renderable);

            ShaderProgram.prependVertexCode = "";
            ShaderProgram.prependFragmentCode = "";
        }

        @Override
        public int compareTo(Shader other) {
            return 0;
        }

        @Override
        public boolean canRender(Renderable instance) {
            return instance.meshPart.mesh.isInstanced();
        }
    }

    public static class ImpostorShader extends BaseShader
    {
        @Override
        public void begin(Camera camera, RenderContext context) {
            program.bind();
            program.setUniformMatrix("u_projViewTrans", camera.combined);
            context.setDepthTest(GL32.GL_LESS);
            //program.setUniformMatrix("u_impostorRotationMatrix", renderable.worldTransform);



            //context.setDepthTest(GL30.GL_LEQUAL);
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
            //init(program, renderable);

        }

        @Override
        public void render(Renderable renderable) {
            MapChunk.DecalTransform decalTransform = (MapChunk.DecalTransform) renderable.userData;

            program.setUniformMatrix("u_impostorRotationMatrix", decalTransform.transform);
            program.setUniformf("u_moveY", decalTransform.moveY);
            program.setUniform2fv("u_uvOffset", decalTransform.uvOffset,0,2);
            program.setUniformi("u_texture", 0);
            super.render(renderable);
        }

        @Override
        public int compareTo(Shader other) {
            return 0;
        }

        @Override
        public boolean canRender(Renderable instance) {
            return true;
        }
    }

    public static class ImpostorShaderGPUheavy extends BaseShader
    {
        @Override
        public void begin(Camera camera, RenderContext context) {
            program.bind();
            program.setUniformMatrix("u_projViewTrans", camera.combined);
            float[] camPos = new float[3];
            camPos[0] = camera.position.x;
            camPos[1] = camera.position.y;
            camPos[2] = camera.position.z;
            program.setUniform3fv("u_camPos", camPos,0,3);
            context.setDepthTest(GL32.GL_LESS);
            //program.setUniformMatrix("u_impostorRotationMatrix", renderable.worldTransform);



            //context.setDepthTest(GL30.GL_LEQUAL);
        }

        @Override
        public void init() {
            ShaderProgram.prependVertexCode = "#version 300 es\n";
            ShaderProgram.prependFragmentCode = "#version 300 es\n";


            program = new ShaderProgram(Gdx.files.internal("shaders/decalinstancedGPUheavy.vert"),
                    Gdx.files.internal("shaders/decalinstanced.frag"));

            if (!program.isCompiled()) {
                throw new GdxRuntimeException("Shader compile error: " + program.getLog());
            }
            //init(program, renderable);

        }


        public void render(Renderable renderable, LodModel.DecalData decalData) {

            /*
            MapChunk.DecalTransform decalTransform = (MapChunk.DecalTransform) renderable.userData;

            program.setUniformMatrix("u_impostorRotationMatrix", decalTransform.transform);
            program.setUniformf("u_moveY", decalTransform.moveY);
            program.setUniform2fv("u_uvOffset", decalTransform.uvOffset,0,2);

             */

            //MapChunk.DecalTransform decalTransform = (MapChunk.DecalTransform) renderable.userData;
            //program.setUniformf("u_moveY", decalTransform.moveY);
            program.setUniformf("u_moveY", 12f);

            program.setUniformi("u_texture", 0);
            program.setUniform2fv("u_uvStepSize",decalData.uvStepSize,0,2);
            program.setUniform2fv("u_uvSteps",decalData.uvSteps,0,2);
            program.setUniform2fv("u_uvSize",decalData.uvSize,0,2);
            super.render(renderable);
        }

        @Override
        public int compareTo(Shader other) {
            return 0;
        }

        @Override
        public boolean canRender(Renderable instance) {
            return true;
        }
    }

}
