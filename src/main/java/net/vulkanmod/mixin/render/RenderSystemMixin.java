package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.class_10366;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.class_9799;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.FogParameters;
import net.vulkanmod.render.engine.VkGpuDevice;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.BiFunction;

import static com.mojang.blaze3d.systems.RenderSystem.*;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    @Shadow private static Matrix4f projectionMatrix;
    @Shadow private static Matrix4f savedProjectionMatrix;
    @Shadow @Final private static Matrix4fStack modelViewStack;
    @Shadow private static Matrix4f textureMatrix;

    @Shadow @Final private static float[] shaderColor;
    @Shadow @Final private static Vector3f[] shaderLightDirections;

    @Shadow private static @Nullable Thread renderThread;

    @Shadow
    public static void assertOnRenderThread() {
    }

    @Shadow private static class_10366 projectionType;
    @Shadow private static class_10366 savedProjectionType;
    @Shadow private static FogParameters shaderFog;

    @Shadow private static @Nullable GpuDevice DEVICE;
    @Shadow private static String apiDescription;
    @Shadow private static @Nullable GpuBuffer QUAD_VERTEX_BUFFER;
//    @Shadow @Final private static ArrayListDeque<RenderSystem.GpuAsyncTask> PENDING_FENCES;

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void initRenderer(long l, int i, boolean bl, BiFunction<ResourceLocation, ShaderType, String> biFunction, boolean bl2) {
        renderThread.setPriority(Thread.NORM_PRIORITY + 2);

        DEVICE = new VkGpuDevice(l, i, bl, biFunction, bl2);
//        apiDescription = getDevice().getImplementationInformation();

        VRenderSystem.initRenderer();

        try (class_9799 byteBufferBuilder = new class_9799(
                DefaultVertexFormat.field_1592.getVertexSize() * 4)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.field_1592);
            bufferBuilder.addVertex(1.0F, 1.0F, 0.0F);
            bufferBuilder.addVertex(0.0F, 1.0F, 0.0F);
            bufferBuilder.addVertex(0.0F, 0.0F, 0.0F);
            bufferBuilder.addVertex(1.0F, 0.0F, 0.0F);

            try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                QUAD_VERTEX_BUFFER = getDevice().createBuffer(() -> "Quad", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.vertexBuffer());
            }
        }
    }

//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void setupDefaultState(int x, int y, int width, int height) { }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void enableColorLogicOp() {
//        assertOnRenderThread();
//        VRenderSystem.enableColorLogicOp();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void disableColorLogicOp() {
//        assertOnRenderThread();
//        VRenderSystem.disableColorLogicOp();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite
//    public static void logicOp(GlStateManager.LogicOp op) {
//        assertOnRenderThread();
//        VRenderSystem.logicOp(op);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void activeTexture(int texture) {
//        GlTexture.activeTexture(texture);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static int maxSupportedTextureSize() {
//        return VRenderSystem.maxSupportedTextureSize();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void clear(int mask) {
//        VRenderSystem.clear(mask);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void clearColor(float r, float g, float b, float a) {
//        VRenderSystem.setClearColor(r, g, b, a);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void clearDepth(double d) {
//        VRenderSystem.clearDepth(d);
//    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enableScissor(int x, int y, int width, int height) {
        Renderer.setScissor(x, y, width, height);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableScissor() {
        Renderer.resetScissor();
    }

//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void disableDepthTest() {
//        assertOnRenderThread();
//        //GlStateManager._disableDepthTest();
//        VRenderSystem.disableDepthTest();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void enableDepthTest() {
//        assertOnRenderThreadOrInit();
//        VRenderSystem.enableDepthTest();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void depthFunc(int i) {
//        assertOnRenderThread();
//        VRenderSystem.depthFunc(i);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void depthMask(boolean b) {
//        assertOnRenderThread();
//        VRenderSystem.depthMask(b);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
//        VRenderSystem.colorMask(red, green, blue, alpha);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void blendEquation(int i) {
//        assertOnRenderThread();
//        //TODO
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void enableBlend() {
//        VRenderSystem.enableBlend();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void disableBlend() {
//        VRenderSystem.disableBlend();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void blendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor) {
//        VRenderSystem.blendFunc(sourceFactor, destFactor);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void blendFunc(int srcFactor, int dstFactor) {
//        VRenderSystem.blendFunc(srcFactor, dstFactor);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void blendFuncSeparate(GlStateManager.SourceFactor p_69417_, GlStateManager.DestFactor p_69418_, GlStateManager.SourceFactor p_69419_, GlStateManager.DestFactor p_69420_) {
//        VRenderSystem.blendFuncSeparate(p_69417_, p_69418_, p_69419_, p_69420_);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
//        VRenderSystem.blendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void enableCull() {
//        assertOnRenderThread();
//        VRenderSystem.enableCull();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void disableCull() {
//        assertOnRenderThread();
//        VRenderSystem.disableCull();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void polygonMode(final int face, final int mode) {
//        assertOnRenderThread();
//        VRenderSystem.setPolygonModeGL(mode);
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void enablePolygonOffset() {
//        assertOnRenderThread();
//        VRenderSystem.enablePolygonOffset();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void disablePolygonOffset() {
//        assertOnRenderThread();
//        VRenderSystem.disablePolygonOffset();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void polygonOffset(float factor, float units) {
//        assertOnRenderThread();
//        VRenderSystem.polygonOffset(factor, units);
//    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setShaderLights(Vector3f dir0, Vector3f dir1) {
        shaderLightDirections[0] = dir0;
        shaderLightDirections[1] = dir1;

        VRenderSystem.lightDirection0.buffer.putFloat(0, dir0.x());
        VRenderSystem.lightDirection0.buffer.putFloat(4, dir0.y());
        VRenderSystem.lightDirection0.buffer.putFloat(8, dir0.z());

        VRenderSystem.lightDirection1.buffer.putFloat(0, dir1.x());
        VRenderSystem.lightDirection1.buffer.putFloat(4, dir1.y());
        VRenderSystem.lightDirection1.buffer.putFloat(8, dir1.z());
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setShaderColor(float r, float g, float b, float a) {
        shaderColor[0] = r;
        shaderColor[1] = g;
        shaderColor[2] = b;
        shaderColor[3] = a;

        VRenderSystem.setShaderColor(r, g, b, a);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setShaderFog(FogParameters fogParameters) {
        assertOnRenderThread();
        shaderFog = fogParameters;

        VRenderSystem.setShaderFogColor(fogParameters.comp_3012(), fogParameters.comp_3013(), fogParameters.comp_3014(), fogParameters.comp_3015());
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setProjectionMatrix(Matrix4f projectionMatrix, class_10366 projectionType) {
        Matrix4f matrix4f = new Matrix4f(projectionMatrix);
        RenderSystemMixin.projectionMatrix = matrix4f;
        RenderSystemMixin.projectionType = projectionType;

        VRenderSystem.applyProjectionMatrix(matrix4f);
        VRenderSystem.calculateMVP();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setTextureMatrix(Matrix4f matrix4f) {
        assertOnRenderThread();
        textureMatrix.set(matrix4f);
        VRenderSystem.setTextureMatrix(matrix4f);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void resetTextureMatrix() {
        assertOnRenderThread();
        textureMatrix.identity();
        VRenderSystem.setTextureMatrix(textureMatrix);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void restoreProjectionMatrix() {
        projectionMatrix = savedProjectionMatrix;
        projectionType = savedProjectionType;

        VRenderSystem.applyProjectionMatrix(projectionMatrix);
        VRenderSystem.calculateMVP();
    }

//    /**
//     * @author
//     */
//    @Overwrite(remap = false)
//    public static void texParameter(int target, int pname, int param) {
//        GlTexture.texParameteri(target, pname, param);
//    }
}
