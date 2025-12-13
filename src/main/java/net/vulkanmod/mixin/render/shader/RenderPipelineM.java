package net.vulkanmod.mixin.render.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.shader.ExtendedRenderPipeline;
import net.vulkanmod.render.engine.EGlProgram;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Supplier;

@Mixin(RenderPipeline.class)
public abstract class RenderPipelineM implements ExtendedRenderPipeline {

    @Shadow @Final private ResourceLocation location;
    @Shadow @Final private VertexFormat vertexFormat;
    @Shadow @Final private List<RenderPipeline.UniformDescription> uniforms;
    @Shadow @Final private List<String> samplers;

    @Shadow public abstract ResourceLocation getVertexShader();
    @Shadow public abstract ResourceLocation getFragmentShader();

    @Unique GraphicsPipeline pipeline;
    @Unique EGlProgram eGlProgram;

    @Unique
    public void setupUniformSuppliers(UBO ubo) {
        for (net.vulkanmod.vulkan.shader.layout.Uniform vUniform : ubo.getUniforms()) {
            Supplier<MappedBuffer> supplier = getUniformSupplier(vUniform.getName());
            vUniform.setSupplier(supplier);
        }
    }

    public Supplier<MappedBuffer> getUniformSupplier(String name) {
        var uniform = this.eGlProgram.getUniform(name);

        if (uniform == null) {
            Initializer.LOGGER.error(String.format("Error: field %s not present in uniform map", name));
            return null;
        }

        Supplier<MappedBuffer> supplier;
        ByteBuffer byteBuffer = switch (uniform.method_35662()) {
            case field_56741, field_56742 -> MemoryUtil.memByteBuffer(((UniformAccessor) uniform).getIntValues());
            case field_56743, field_56744, field_56745, field_56746, field_56747 -> MemoryUtil.memByteBuffer(((UniformAccessor) uniform).getFloatValues());
        };

        MappedBuffer mappedBuffer = MappedBuffer.createFromBuffer(byteBuffer);
        supplier = () -> mappedBuffer;

        return supplier;
    }

    @Override
    public void setPipeline(GraphicsPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void setProgram(EGlProgram program) {
        this.eGlProgram = program;
    }

    @Override
    public EGlProgram getProgram() {
        return this.eGlProgram;
    }

    @Override
    public Pipeline getPipeline() {
        return this.pipeline;
    }
}
