package net.vulkanmod.render.engine;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.*;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.interfaces.shader.ExtendedRenderPipeline;
import net.vulkanmod.render.shader.ShaderLoadUtil;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.converter.GlslConverter;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VK10;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@SuppressWarnings("NullableProblems")
public class VkGpuDevice implements GpuDevice {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final VkCommandEncoder encoder;
    private final VkDebugLabel debugLabels;
    private final int maxSupportedTextureSize;
    private final BiFunction<ResourceLocation, ShaderType, String> defaultShaderSource;
    private final Map<RenderPipeline, GlRenderPipeline> pipelineCache = new IdentityHashMap<>();
    private final Map<ShaderCompilationKey, GlShaderModule> shaderCache = new HashMap<>();
    private final Set<String> enabledExtensions = new HashSet<>();

    private final Map<ShaderCompilationKey, String> shaderSrcCache = new HashMap<>();

    public VkGpuDevice(long l, int i, boolean bl, BiFunction<ResourceLocation, ShaderType, String> shaderSource, boolean bl2) {
        this.debugLabels = VkDebugLabel.create(bl2, this.enabledExtensions);
        this.maxSupportedTextureSize = 8192;
        this.defaultShaderSource = shaderSource;

        this.encoder = new VkCommandEncoder(this);
    }

    public VkDebugLabel debugLabels() {
        return this.debugLabels;
    }

    @Override
    public CommandEncoder createCommandEncoder() {
        return this.encoder;
    }

    @Override
    public GpuTexture createTexture(@Nullable Supplier<String> supplier, TextureFormat textureFormat, int i, int j, int k) {
        return this.createTexture(this.debugLabels.exists() && supplier != null ? supplier.get() : null, textureFormat, i, j, k);
    }

    @Override
    public GpuTexture createTexture(@Nullable String string, TextureFormat textureFormat, int width, int height, int mipLevels) {
        if (mipLevels < 1) {
            throw new IllegalArgumentException("mipLevels must be at least 1");
        } else {
            int id = VkGlTexture.genTextureId();
            if (string == null) {
                string = String.valueOf(id);
            }

            int format = VkGpuTexture.vkFormat(textureFormat);
            boolean depthFormat = VulkanImage.isDepthFormat(format);
            int attachmentUsage = depthFormat ? VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT : VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

            VulkanImage texture = VulkanImage.builder(width, height)
                                             .setName(string)
                                             .setFormat(format)
                                             .setMipLevels(mipLevels)
                                             .addUsage(attachmentUsage)
                                             .createVulkanImage();

            VkGlTexture vGlTexture = VkGlTexture.getTexture(id);
            vGlTexture.setVulkanImage(texture);
            VkGlTexture.bindTexture(id);

            VkGpuTexture glTexture = new VkGpuTexture(string, textureFormat, width, height, mipLevels, id, vGlTexture);
            this.debugLabels.applyLabel(glTexture);
            return glTexture;
        }
    }

    public VkGpuTexture gpuTextureFromVulkanImage(VulkanImage image) {
        int id = VkGlTexture.genTextureId();
        VkGlTexture glTexture = VkGlTexture.getTexture(id);
        glTexture.setVulkanImage(image);
        TextureFormat textureFormat = VkGpuTexture.textureFormat(image.format);
        VkGpuTexture gpuTexture = new VkGpuTexture(image.name, textureFormat, image.width, image.height, image.mipLevels, id, glTexture);
        this.debugLabels.applyLabel(gpuTexture);
        return gpuTexture;
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> supplier, BufferType bufferType, BufferUsage bufferUsage, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than zero");
        } else {
            return new VkGpuBuffer(this.debugLabels, supplier, bufferType, bufferUsage, size);
        }
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> supplier, BufferType bufferType, BufferUsage bufferUsage, ByteBuffer byteBuffer) {
        if (!byteBuffer.hasRemaining()) {
            throw new IllegalArgumentException("Buffer source must not be empty");
        } else {
            VkGpuBuffer glBuffer = new VkGpuBuffer(this.debugLabels, supplier, bufferType, bufferUsage, byteBuffer.remaining());
            this.encoder.writeToBuffer(glBuffer, byteBuffer, 0);
            return glBuffer;
        }
    }

    @Override
    public String getImplementationInformation() {
        return GLFW.glfwGetCurrentContext() == 0L
                ? "NO CONTEXT"
                : GlStateManager._getString(7937) + " GL version " + GlStateManager._getString(7938) + ", " + GlStateManager._getString(7936);
    }

    @Override
    public List<String> getLastDebugMessages() {
        return Collections.emptyList();
    }

    @Override
    public boolean isDebuggingEnabled() {
        return false;
    }

    @Override
    public String getRenderer() {
        return "VulkanMod";
    }

    @Override
    public String getVendor() {
        return Vulkan.getDevice().vendorIdString;
    }

    @Override
    public String getBackendName() {
        return "Vulkan";
    }

    @Override
    public String getVersion() {
        return Vulkan.getDevice().vkVersion;
    }

    private static int getMaxSupportedTextureSize() {
        int i = GlStateManager._getInteger(3379);

        for (int j = Math.max(32768, i); j >= 1024; j >>= 1) {
            GlStateManager._texImage2D(32868, 0, 6408, j, j, 0, 6408, 5121, null);
            int k = GlStateManager._getTexLevelParameter(32868, 0, 4096);
            if (k != 0) {
                return j;
            }
        }

        int jx = Math.max(i, 1024);
        LOGGER.info("Failed to determine maximum texture size by probing, trying GL_MAX_TEXTURE_SIZE = {}", jx);
        return jx;
    }

    @Override
    public int getMaxTextureSize() {
        return this.maxSupportedTextureSize;
    }

    @Override
    public void clearPipelineCache() {
        for (GlRenderPipeline glRenderPipeline : this.pipelineCache.values()) {
            if (glRenderPipeline.program() != GlProgram.INVALID_PROGRAM) {
                glRenderPipeline.program().close();
            }
        }

        this.pipelineCache.clear();

        for (GlShaderModule glShaderModule : this.shaderCache.values()) {
            if (glShaderModule != GlShaderModule.INVALID_SHADER) {
                glShaderModule.close();
            }
        }

        this.shaderCache.clear();
    }

    @Override
    public List<String> getEnabledExtensions() {
        return new ArrayList(this.enabledExtensions);
    }

    @Override
    public void close() {
        this.clearPipelineCache();
    }

    protected GlShaderModule getOrCompileShader(
            ResourceLocation resourceLocation, ShaderType shaderType, ShaderDefines shaderDefines, BiFunction<ResourceLocation, ShaderType, String> biFunction
    ) {
        ShaderCompilationKey shaderCompilationKey = new ShaderCompilationKey(resourceLocation, shaderType, shaderDefines);
        return this.shaderCache.computeIfAbsent(shaderCompilationKey, shaderCompilationKey2 -> this.compileShader(shaderCompilationKey, biFunction));
    }

    protected String getCachedShaderSrc(ResourceLocation resourceLocation, ShaderType shaderType, ShaderDefines shaderDefines, BiFunction<ResourceLocation, ShaderType, String> shaderSourceGetter) {
        ShaderCompilationKey shaderCompilationKey = new ShaderCompilationKey(resourceLocation, shaderType, shaderDefines);

        return this.shaderSrcCache.computeIfAbsent(shaderCompilationKey, compilationKey -> {
            if (resourceLocation.getPath().contains("post")) {
                String src = ShaderLoadUtil.getShaderSource(resourceLocation, shaderType);

                if (src != null) {
                    return src;
                }
            }

            return shaderSourceGetter.apply(compilationKey.id, compilationKey.type);
        });
    }

    public CompiledRenderPipeline precompilePipeline(RenderPipeline renderPipeline, @Nullable BiFunction<ResourceLocation, ShaderType, String> shaderSourceGetter) {
//        BiFunction<ResourceLocation, ShaderType, String> biFunction2 = shaderSourceGetter == null ? this.defaultShaderSource : shaderSourceGetter;
//        return this.pipelineCache.computeIfAbsent(renderPipeline, renderPipeline2 -> this.compilePipeline(renderPipeline, biFunction2));

//        ExtendedRenderPipeline.of(renderPipeline).compile(shaderSourceGetter);
        shaderSourceGetter = shaderSourceGetter == null ? this.defaultShaderSource : shaderSourceGetter;
        compilePipeline(renderPipeline, shaderSourceGetter);

        return new VkRenderPipeline(renderPipeline);
    }

    public void compilePipeline(RenderPipeline renderPipeline) {
//        BiFunction<ResourceLocation, ShaderType, String> biFunction2 = biFunction == null ? this.defaultShaderSource : biFunction;
//        return this.pipelineCache.computeIfAbsent(renderPipeline, renderPipeline2 -> this.compilePipeline(renderPipeline, biFunction2));

//        ExtendedRenderPipeline.of(renderPipeline).compile(this.defaultShaderSource);
        this.compilePipeline(renderPipeline, this.defaultShaderSource);
    }

    private GlShaderModule compileShader(ShaderCompilationKey shaderCompilationKey, BiFunction<ResourceLocation, ShaderType, String> biFunction) {
        String string = biFunction.apply(shaderCompilationKey.id, shaderCompilationKey.type);
        if (string == null) {
            LOGGER.error("Couldn't find source for {} shader ({})", shaderCompilationKey.type, shaderCompilationKey.id);
            return GlShaderModule.INVALID_SHADER;
        } else {
            String string2 = GlslPreprocessor.injectDefines(string, shaderCompilationKey.defines);
            int i = GlStateManager.glCreateShader(GlConst.toGl(shaderCompilationKey.type));
            GlStateManager.glShaderSource(i, string2);
            GlStateManager.glCompileShader(i);
            if (GlStateManager.glGetShaderi(i, 35713) == 0) {
                String string3 = StringUtils.trim(GlStateManager.glGetShaderInfoLog(i, 32768));
                LOGGER.error("Couldn't compile {} shader ({}): {}", shaderCompilationKey.type.getName(), shaderCompilationKey.id, string3);
                return GlShaderModule.INVALID_SHADER;
            } else {
                GlShaderModule glShaderModule = new GlShaderModule(i, shaderCompilationKey.id, shaderCompilationKey.type);
                this.debugLabels.applyLabel(glShaderModule);
                return glShaderModule;
            }
        }
    }

    private void compilePipeline(RenderPipeline renderPipeline, BiFunction<ResourceLocation, ShaderType, String> shaderSrcGetter) {
        String locationPath = renderPipeline.getLocation().getPath();

        String configName;
        if (locationPath.contains("core")) {
            configName = locationPath.split("/")[1];
        } else {
            configName = locationPath;
        }

        Pipeline.Builder builder = new Pipeline.Builder(renderPipeline.getVertexFormat(), configName);
        GraphicsPipeline pipeline;

        EGlProgram eGlProgram = new EGlProgram(1, configName);
        eGlProgram.setupUniforms(renderPipeline.getUniforms(), renderPipeline.getSamplers());

        ExtendedRenderPipeline extPipeline = ExtendedRenderPipeline.of(renderPipeline);
        extPipeline.setProgram(eGlProgram);

        JsonObject config = ShaderLoadUtil.getJsonConfig("core", configName);

        if (config == null && !configName.startsWith("rendertype")) {
            config = ShaderLoadUtil.getJsonConfig("core", "rendertype_%s".formatted(configName));
        }

        ResourceLocation vertexShaderLocation = renderPipeline.getVertexShader();
        ResourceLocation fragmentShaderLocation = renderPipeline.getVertexShader();
        if (config == null && vertexShaderLocation.getPath().equals(fragmentShaderLocation.getPath())) {
            locationPath = vertexShaderLocation.getPath();
            configName = locationPath.split("/")[1];
            config = ShaderLoadUtil.getJsonConfig("core", configName);

            if (config == null) {
                if (configName.startsWith("rendertype_")) {
                    configName = configName.substring("rendertype_".length());
                    config = ShaderLoadUtil.getJsonConfig("core", configName);
                }
            }
        }

        if (config == null) {
//            throw new RuntimeException("config %s does not exist.".formatted(configName));

            GlslConverter converter = new GlslConverter();

//            ShaderDefines shaderDefines = shaderProgramConfig.defines().withOverrides(shaderProgram.defines());
            String vshSrc = this.getCachedShaderSrc(renderPipeline.getVertexShader(), ShaderType.VERTEX, null, shaderSrcGetter);
            String fshSrc = this.getCachedShaderSrc(renderPipeline.getFragmentShader(), ShaderType.FRAGMENT, null, shaderSrcGetter);

            converter.process(renderPipeline.getVertexFormat(), vshSrc, fshSrc);

            UBO ubo = converter.createUBO();
            List<UBO> ubos = ubo != null ? Collections.singletonList(ubo) : Collections.emptyList();

            builder.setUniforms(ubos, converter.getSamplerList());
            builder.compileShaders(configName, converter.getVshConverted(), converter.getFshConverted());

            pipeline = builder.createGraphicsPipeline();
//            shaderMixed.setUniformsUpdate();

            if (ubo != null) {
                extPipeline.setupUniformSuppliers(ubo);
            }
        }
        else {
            builder.setUniformSupplierGetter(info -> extPipeline.getUniformSupplier(info.name));
            builder.parseBindings(config);

            ShaderLoadUtil.loadShader(builder, configName, renderPipeline.getVertexShader().getPath(), SPIRVUtils.ShaderKind.VERTEX_SHADER);
            ShaderLoadUtil.loadShader(builder, configName, renderPipeline.getFragmentShader().getPath(), SPIRVUtils.ShaderKind.FRAGMENT_SHADER);

            pipeline = builder.createGraphicsPipeline();
        }

        extPipeline.setPipeline(pipeline);
    }

    @Environment(EnvType.CLIENT)
    static record ShaderCompilationKey(ResourceLocation id, ShaderType type, ShaderDefines defines) {

        public String toString() {
            String string = this.id + " (" + this.type + ")";
            return !this.defines.isEmpty() ? string + " with " + this.defines : string;
        }
    }

    private static class VkRenderPipeline implements CompiledRenderPipeline {
        final RenderPipeline renderPipeline;

        public VkRenderPipeline(RenderPipeline renderPipeline) {
            this.renderPipeline = renderPipeline;
        }

        @Override
        public boolean containsUniform(String string) {
            return ExtendedRenderPipeline.of(renderPipeline).getProgram().getUniform(string) != null;
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }
}
