package net.vulkanmod.mixin.texture.update;

import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.block.BlockRenderDispatcher4;
import net.vulkanmod.render.texture.SpriteUpdateUtil;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher4.BlockRenderDispatcher5.class)
public class MSpriteContents {

    @Shadow int subFrame;
    @Shadow int frame;
    @Shadow @Final BlockRenderDispatcher4.class_5790 animationInfo;

    @Inject(method = "tickAndUpload", at = @At("HEAD"), cancellable = true)
    private void checkUpload(int i, int j, GpuTexture gpuTexture, CallbackInfo ci) {
        if (!SpriteUpdateUtil.doUploadFrame()) {
            // Update animations frames even if no upload is scheduled
            ++this.subFrame;
            BlockRenderDispatcher4.class_5791 frameInfo = this.animationInfo.field_28472.get(this.frame);
            if (this.subFrame >= frameInfo.comp_3446) {
                this.frame = (this.frame + 1) % this.animationInfo.field_28472.size();
                this.subFrame = 0;
            }

            ci.cancel();
        }
        else {
            SpriteUpdateUtil.addTransitionedLayout(VTextureSelector.getBoundTexture());
        }
    }
}
