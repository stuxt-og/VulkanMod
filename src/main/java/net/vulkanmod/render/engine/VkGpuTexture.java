package net.vulkanmod.render.engine;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.vulkan.VK10;

@Environment(EnvType.CLIENT)
public class VkGpuTexture extends GpuTexture {
    protected VkGlTexture glTexture;
    protected final int field_57882;
    private final Int2ReferenceMap<VkFbo> fboCache = new Int2ReferenceOpenHashMap<>();
    protected boolean field_57883;
    protected boolean field_57884 = true;

    protected VkGpuTexture(String string, TextureFormat textureFormat, int width, int height, int mipLevel, int id, VkGlTexture glTexture) {
        super(string, textureFormat, width, height, mipLevel);
        this.field_57882 = id;
        this.glTexture = glTexture;
    }

    @Override
    public void close() {
        if (!this.field_57883) {
            this.field_57883 = true;
            GlStateManager._deleteTexture(this.field_57882);

            for (VkFbo fbo : this.fboCache.values()) {
                fbo.close();
            }
        }
    }

    @Override
    public boolean isClosed() {
        return this.field_57883;
    }

//    public int getFbo(DirectStateAccess directStateAccess, @Nullable GpuTexture gpuTexture) {
//        int i = gpuTexture == null ? 0 : ((VkGpuTexture)gpuTexture).id;
//        return this.fboCache.computeIfAbsent(i, j -> {
//            int k = directStateAccess.createFrameBufferObject();
//            directStateAccess.bindFrameBufferTextures(k, this.id, i, 0, 0);
//            return k;
//        });
//    }

    public void method_68424() {
        if (this.field_57884) {
//            GlStateManager._texParameter(3553, 10242, GlConst.toGl(this.addressModeU));
//            GlStateManager._texParameter(3553, 10243, GlConst.toGl(this.addressModeV));
//            switch (this.minFilter) {
//                case NEAREST:
//                    GlStateManager._texParameter(3553, 10241, this.useMipmaps ? 9986 : 9728);
//                    break;
//                case LINEAR:
//                    GlStateManager._texParameter(3553, 10241, this.useMipmaps ? 9987 : 9729);
//            }
//
//            switch (this.magFilter) {
//                case NEAREST:
//                    GlStateManager._texParameter(3553, 10240, 9728);
//                    break;
//                case LINEAR:
//                    GlStateManager._texParameter(3553, 10240, 9729);
//            }

            byte samplerFlags;
            samplerFlags = magFilter == FilterMode.LINEAR ? SamplerManager.LINEAR_FILTERING_BIT : 0;

            samplerFlags |= switch (minFilter) {
                case LINEAR -> SamplerManager.USE_MIPMAPS_BIT | SamplerManager.MIPMAP_LINEAR_FILTERING_BIT;
                case NEAREST -> SamplerManager.USE_MIPMAPS_BIT;
                default -> 0;
            };

            glTexture.getVulkanImage().updateTextureSampler(this.getMipLevels(), samplerFlags);

            this.field_57884 = false;
        }
    }

    public int method_68427() {
        return this.field_57882;
    }

    @Override
    public void setAddressMode(AddressMode addressMode, AddressMode addressMode2) {
        super.setAddressMode(addressMode, addressMode2);
        this.field_57884 = true;
    }

    @Override
    public void setTextureFilter(FilterMode filterMode, FilterMode filterMode2, boolean bl) {
        super.setTextureFilter(filterMode, filterMode2, bl);
        this.field_57884 = true;
    }

    public VkFbo getFbo(@Nullable GpuTexture depthAttachment) {
        int depthAttachmentId = depthAttachment == null ? 0 : ((VkGpuTexture)depthAttachment).field_57882;
        return this.fboCache.computeIfAbsent(depthAttachmentId, j -> new VkFbo(this, (VkGpuTexture) depthAttachment));
    }

    public VulkanImage getVulkanImage() {
        return glTexture.getVulkanImage();
    }

    public static TextureFormat textureFormat(int format) {
        return switch (format) {
            case VK10.VK_FORMAT_R8G8B8A8_UNORM, VK10.VK_FORMAT_B8G8R8A8_UNORM -> TextureFormat.RGBA8;
            case VK10.VK_FORMAT_R8_UNORM -> TextureFormat.RED8;
            case VK10.VK_FORMAT_D32_SFLOAT -> TextureFormat.DEPTH32;
            default -> throw new IllegalStateException("Unexpected value: " + format);
        };
    }

    public static int vkFormat(TextureFormat textureFormat) {
        return switch (textureFormat) {
            case RGBA8 -> VK10.VK_FORMAT_R8G8B8A8_UNORM;
            case RED8 -> VK10.VK_FORMAT_R8_UNORM;
            case DEPTH32 -> VK10.VK_FORMAT_D32_SFLOAT;
        };
    }
}

