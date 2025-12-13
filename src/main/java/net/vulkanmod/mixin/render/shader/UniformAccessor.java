package net.vulkanmod.mixin.render.shader;

import com.mojang.blaze3d.opengl.Uniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Mixin(Uniform.class)
public interface UniformAccessor {

    @Accessor("intValues")
    IntBuffer getIntValues();

    @Accessor("floatValues")
    FloatBuffer getFloatValues();
}
