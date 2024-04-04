package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;

public interface InstancedShaderProvider extends ShaderProvider {
    Shader createShader(Renderable renderable);

    void dispose();

    BaseShader createPlainShader(Renderable renderable);

    BaseShader createInstancedShader(Renderable renderable);

    BaseShader createDecalShader(Renderable renderable);
}
