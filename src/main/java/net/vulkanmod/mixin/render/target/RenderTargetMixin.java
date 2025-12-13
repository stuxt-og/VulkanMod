package net.vulkanmod.mixin.render.target;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.class_10799;
import net.minecraft.class_276;
import net.vulkanmod.render.engine.VkFbo;
import net.vulkanmod.render.engine.VkGpuTexture;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.OptionalInt;

@Mixin(class_276.class)
public abstract class RenderTargetMixin {

    @Shadow public int viewWidth;
    @Shadow public int viewHeight;
    @Shadow public int width;
    @Shadow public int height;

//    @Shadow protected int depthBufferId;
//    @Shadow protected int colorTextureId;
//    @Shadow public int frameBufferId;
//
//    @Shadow @Final private float[] clearChannels;
    @Shadow @Final public boolean useDepth;

    @Shadow @Nullable protected GpuTexture colorTexture;
    @Shadow @Nullable protected GpuTexture depthTexture;
    boolean needClear = false;
    boolean bound = false;

//    /**
//     * @author
//     */
//    @Overwrite
//    public void clear() {
//        RenderSystem.assertOnRenderThreadOrInit();
//
//        if(!Renderer.isRecording())
//            return;
//
//        // If the framebuffer is not bound postpone clear
//        GlFramebuffer glFramebuffer = GlFramebuffer.getFramebuffer(this.frameBufferId);
//        if (!bound || GlFramebuffer.getBoundFramebuffer() != glFramebuffer) {
//            needClear = true;
//            return;
//        }
//
//        GlStateManager._clearColor(this.clearChannels[0], this.clearChannels[1], this.clearChannels[2], this.clearChannels[3]);
//        int i = 16384;
//        if (this.useDepth) {
//            GlStateManager._clearDepth(1.0);
//            i |= 256;
//        }
//
//        GlStateManager._clear(i);
//        needClear = false;
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite
//    public void bindRead() {
//        RenderSystem.assertOnRenderThread();
//
//        applyClear();
//
//        GlTexture.bindTexture(this.colorTextureId);
//
//        try (MemoryStack stack = MemoryStack.stackPush()) {
//            GlTexture.getBoundTexture().getVulkanImage()
//                    .readOnlyLayout(stack, Renderer.getCommandBuffer());
//        }
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite
//    public void unbindRead() {
//        RenderSystem.assertOnRenderThreadOrInit();
//        GlTexture.bindTexture(0);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite
//    public void bindWrite(boolean bl) {
//        RenderSystem.assertOnRenderThreadOrInit();
//
//        GlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
//        if (bl) {
//            GlStateManager._viewport(0, 0, this.viewWidth, this.viewHeight);
//        }
//
//        this.bound = true;
//        if (needClear)
//            clear();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite
//    public void unbindWrite() {
//        if (!RenderSystem.isOnRenderThread()) {
//            RenderSystem.recordRenderCall(() -> {
//                GlStateManager._glBindFramebuffer(36160, 0);
//                this.bound = false;
//            });
//        } else {
//            GlStateManager._glBindFramebuffer(36160, 0);
//            this.bound = false;
//        }
//    }
//
//    @Inject(method = "blitToScreen", at = @At("HEAD"), cancellable = true)
//    private void blitToScreen(CallbackInfo ci) {
//        // If the target needs clear it means it has not been used, thus we can skip blit
//        if (!this.needClear) {
//            Framebuffer framebuffer = GlFramebuffer.getFramebuffer(this.frameBufferId).getFramebuffer();
//            VTextureSelector.bindTexture(0, framebuffer.getColorAttachment());
//
//            DrawUtil.blitToScreen();
//        }
//
//        ci.cancel();
//    }
//
    /**
     * @author
     * @reason
     */
    @Overwrite
    public void blitAndBlendToTexture(GpuTexture gpuTexture) {
        RenderSystem.assertOnRenderThread();

        VkFbo fbo = ((VkGpuTexture) this.colorTexture).getFbo(this.depthTexture);
        if (fbo.needsClear()) {
            return;
        }

        RenderSystem.class_5590 autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer gpuBuffer = autoStorageIndexBuffer.method_68274(6);
        GpuBuffer gpuBuffer2 = RenderSystem.getQuadVertexBuffer();

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(gpuTexture, OptionalInt.empty())) {
            renderPass.setPipeline(class_10799.field_56840);
            renderPass.setVertexBuffer(0, gpuBuffer2);
            renderPass.setIndexBuffer(gpuBuffer, autoStorageIndexBuffer.method_31924());
            renderPass.bindSampler("InSampler", this.colorTexture);
            renderPass.drawIndexed(0, 6);
        }
    }
//
//    @Inject(method = "getColorTextureId", at = @At("HEAD"))
//    private void injClear(CallbackInfoReturnable<Integer> cir) {
//        applyClear();
//    }
//
//    @Unique
//    private void applyClear() {
//        if (this.needClear) {
//            GlFramebuffer currentFramebuffer = GlFramebuffer.getBoundFramebuffer();
//
//            this.bindWrite(false);
//
//            if (currentFramebuffer != null) {
//                GlFramebuffer.beginRendering(currentFramebuffer);
//            }
//        }
//    }
}
