package net.vulkanmod.mixin.util;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.class_276;
import net.minecraft.class_318;
import net.vulkanmod.render.engine.VkGpuTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.lwjgl.vulkan.VK10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Consumer;

@Mixin(class_318.class)
public class ScreenshotRecorderM {

    /**
     * @author
     */
    @Overwrite
    public static void takeScreenshot(class_276 renderTarget, Consumer<NativeImage> consumer) {
        int width = renderTarget.field_1482;
        int height = renderTarget.field_1481;
        GpuTexture gpuTexture = renderTarget.method_30277();
        if (gpuTexture == null) {
            throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
        } else {
            // Need to submit and wait cmds if screenshot was requested
            // before the end of the frame
            Renderer.getInstance().flushCmds();

            int pixelSize = TextureFormat.RGBA8.pixelSize();
            GpuBuffer gpuBuffer = RenderSystem.getDevice()
                                              .createBuffer(() -> "Screenshot buffer", BufferType.PIXEL_PACK, BufferUsage.STATIC_READ, width * height * pixelSize);
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {
                try (GpuBuffer.ReadView readView = commandEncoder.readBuffer(gpuBuffer)) {
                    NativeImage nativeImage = new NativeImage(width, height, false);

                    var colorAttachment = ((VkGpuTexture) Renderer.getInstance()
                                                                  .getMainPass()
                                                                  .getColorAttachment());
                    boolean isBgraFormat = (colorAttachment.getVulkanImage().format == VK10.VK_FORMAT_B8G8R8A8_UNORM);

                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int color = readView.data().getInt((x + y * width) * pixelSize);

                            if (isBgraFormat) {
                                color = ColorUtil.BGRAtoRGBA(color);
                            }

                            nativeImage.method_4305(x, y, color | 0xFF000000);
                        }
                    }

                    consumer.accept(nativeImage);
                }

                gpuBuffer.close();
            }, 0);
        }
    }

}
