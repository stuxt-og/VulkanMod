package net.vulkanmod.mixin.compatibility;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.render.engine.*;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.shader.converter.UniformParser;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.vulkan.VK10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

@Mixin(PostPass.class)
public abstract class PostPassM {
    @Shadow @Final private String name;

    @Shadow @Final private List<PostPass.Input> inputs;
    @Shadow @Final private ResourceLocation outputTargetId;
    @Shadow @Final private List<PostChainConfig.Uniform> uniforms;

//    @Shadow protected abstract void restoreDefaultUniforms();

    @Shadow @Final private RenderPipeline pipeline;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void addToFrame(
            FrameGraphBuilder frameGraphBuilder, Map<ResourceLocation, ResourceHandle<RenderTarget>> map, Matrix4f matrix4f, @Nullable Consumer<RenderPass> consumer
    ) {
        FramePass framePass = frameGraphBuilder.addPass(this.name);

        for (PostPass.Input input : this.inputs) {
            input.addToPass(framePass, map);
        }

        ResourceHandle<RenderTarget> resourceHandle = map.computeIfPresent(
                this.outputTargetId, (resourceLocation, resourceHandlex) -> framePass.readsAndWrites(resourceHandlex)
        );
        if (resourceHandle == null) {
            throw new IllegalStateException("Missing handle for target " + this.outputTargetId);
        } else {
            framePass.executes(
                    () -> {
                        RenderTarget renderTarget = resourceHandle.get();
                        RenderSystem.backupProjectionMatrix();
                        RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.ORTHOGRAPHIC);
                        VkGpuBuffer quadVertexBuffer = (VkGpuBuffer) RenderSystem.getQuadVertexBuffer();
                        RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                        VkGpuBuffer indexBuffer = (VkGpuBuffer) autoStorageIndexBuffer.getBuffer(6);

                        Renderer.getInstance().endRenderPass();

                        for (PostPass.Input inputx : this.inputs) {
                            if (inputx instanceof PostPass.TargetInput) {
                                var targetId = ((PostPass.TargetInput) inputx).targetId();
                                var inTarget = map.get(targetId).get();

                                if (inTarget instanceof RenderTarget) {
                                    VkGpuTexture colorTexture = (VkGpuTexture) inTarget.getColorTexture();
                                    colorTexture.getVulkanImage().readOnlyLayout();

                                    VkGpuTexture depthTexture = (VkGpuTexture) inTarget.getDepthTexture();
                                    if (depthTexture != null) {
                                        depthTexture.getVulkanImage().readOnlyLayout();
                                    }
                                }
                            }

//                            inputx.bindTo(this.shader, map);
                        }

                        try (RenderPass renderPass = RenderSystem.getDevice()
                                                                 .createCommandEncoder()
                                                                 .createRenderPass(
                                                                         renderTarget.getColorTexture(), OptionalInt.empty(), renderTarget.useDepth ? renderTarget.getDepthTexture() : null, OptionalDouble.empty()
                                                                 )) {
                            renderPass.setPipeline(this.pipeline);
                            renderPass.setUniform("OutSize", (float)renderTarget.width, (float)renderTarget.height);
                            renderPass.setVertexBuffer(0, quadVertexBuffer);
                            renderPass.setIndexBuffer(indexBuffer, autoStorageIndexBuffer.type());

                            for (var inputx : this.inputs) {
                                inputx.bindTo(renderPass, map);
                            }

                            if (consumer != null) {
                                consumer.accept(renderPass);
                            }

                            for (PostChainConfig.Uniform uniform : this.uniforms) {
                                uniform.setOnRenderPass(renderPass);
                            }

//                            Renderer.setInvertedViewport(0, 0, renderTarget.width, renderTarget.height);
//                            Renderer.resetScissor();
//                            VRenderSystem.disableCull();
//
                            VkGpuDevice gpuDevice = (VkGpuDevice) RenderSystem.getDevice();
                            VkCommandEncoder commandEncoder = (VkCommandEncoder) gpuDevice.createCommandEncoder();
//                            commandEncoder.trySetup((VkRenderPass) renderPass);
//
//                            {
//                                ExtendedRenderPipeline extRenderPipeline = ExtendedRenderPipeline.of(this.pipeline);
//                                Pipeline pipeline = extRenderPipeline.getPipeline();
//
//                                Renderer renderer = Renderer.getInstance();
//                                renderer.bindGraphicsPipeline((GraphicsPipeline) pipeline);
//                                VTextureSelector.bindShaderTextures(pipeline);
//                                renderer.uploadAndBindUBOs(pipeline);
//                            }

//                            renderPass.drawIndexed(0, 6);

                            commandEncoder.setupUniforms((VkRenderPass) renderPass);
                            commandEncoder.bindPipeline(((VkRenderPass) renderPass).getPipeline());

                            int indexType = switch (autoStorageIndexBuffer.type()) {
                                case SHORT -> VK10.VK_INDEX_TYPE_UINT16;
                                case INT -> VK10.VK_INDEX_TYPE_UINT32;
                            };

                            Renderer.getDrawer().drawIndexed(quadVertexBuffer.getBuffer(), indexBuffer.getBuffer(), 6, indexType);
                        }

                        RenderSystem.restoreProjectionMatrix();

//                        Renderer.resetViewport();
//                        VRenderSystem.enableCull();

                        for (PostPass.Input input2 : this.inputs) {
                            input2.cleanup(map);
                        }
                    }
            );
        }
    }

}
