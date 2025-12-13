package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.interfaces.shader.ExtendedRenderPipeline;
import net.vulkanmod.render.engine.VkCommandEncoder;
import net.vulkanmod.render.engine.VkGpuTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.OptionalDouble;
import java.util.OptionalInt;

@Mixin(RenderType.CompositeRenderType.class)
public abstract class CompositeRenderTypeM {

    @Shadow public abstract RenderPipeline getRenderPipeline();

    @Shadow @Final private RenderType.CompositeState state;

//    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V", shift = At.Shift.AFTER))
//    private void bindPipeline(MeshData par1, CallbackInfo ci, @Local RenderPipeline renderPipeline) {
//        try {
//            Pipeline pipeline = ExtendedRenderPipeline.of(renderPipeline).getPipeline();
//
//            Renderer renderer = Renderer.getInstance();
//            renderer.bindGraphicsPipeline((GraphicsPipeline) pipeline);
//            VTextureSelector.bindShaderTextures(pipeline);
//            renderer.uploadAndBindUBOs(pipeline);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//    }

    @Shadow @Final private RenderPipeline renderPipeline;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void draw(MeshData meshData) {
        RenderPipeline renderPipeline = this.getRenderPipeline();
        ((RenderType.CompositeRenderType)(Object)(this)).setupRenderState();

        try {
            RenderTarget renderTarget = ((CompositeStateAccessor)(Object)this.state).getOutputState().getRenderTarget();
            VkCommandEncoder commandEncoder = (VkCommandEncoder) RenderSystem.getDevice().createCommandEncoder();

            try (RenderPass renderPass = commandEncoder.createRenderPass(
                    renderTarget.getColorTexture(), OptionalInt.empty(), renderTarget.useDepth ? renderTarget.getDepthTexture() : null, OptionalDouble.empty()
                )) {
                renderPass.setPipeline(renderPipeline);
                if (RenderSystem.SCISSOR_STATE.isEnabled()) {
                    renderPass.enableScissor(RenderSystem.SCISSOR_STATE);
                }

                for (int i = 0; i < 12; i++) {
                    GpuTexture gpuTexture = RenderSystem.getShaderTexture(i);
                    if (gpuTexture != null) {
                        if (((VkGpuTexture)gpuTexture).getVulkanImage() == null)
                            throw new NullPointerException();

                        renderPass.bindSampler("Sampler" + i, gpuTexture);
                        VTextureSelector.bindTexture(i, ((VkGpuTexture)gpuTexture).getVulkanImage());
                    }
                }

                VRenderSystem.applyModelViewMatrix(RenderSystem.getModelViewMatrix());
                VRenderSystem.applyProjectionMatrix(RenderSystem.getProjectionMatrix());
                VRenderSystem.calculateMVP();

                commandEncoder.applyPipelineState(renderPipeline);

                Pipeline pipeline = ExtendedRenderPipeline.of(renderPipeline).getPipeline();
                VRenderSystem.setPrimitiveTopologyGL(GlConst.toGl(meshData.drawState().mode()));

                Renderer renderer = Renderer.getInstance();
                renderer.bindGraphicsPipeline((GraphicsPipeline) pipeline);
                VTextureSelector.bindShaderTextures(pipeline);
                renderer.uploadAndBindUBOs(pipeline);

                Renderer.getDrawer().draw(meshData.vertexBuffer(), meshData.indexBuffer(), meshData.drawState().mode(), meshData.drawState().format(), meshData.drawState().vertexCount());
            }
        } catch (Throwable var14) {
            if (meshData != null) {
                try {
                    meshData.close();
                } catch (Throwable var11) {
                    var14.addSuppressed(var11);
                }
            }

            throw var14;
        }

        if (meshData != null) {
            meshData.close();
        }

        ((RenderType.CompositeRenderType)(Object)(this)).clearRenderState();
    }

}
