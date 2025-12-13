package net.vulkanmod.mixin.texture.update;

import net.minecraft.class_1060;
import net.minecraft.class_1061;
import net.vulkanmod.render.texture.SpriteUpdateUtil;
import net.vulkanmod.vulkan.Renderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(class_1060.class)
public abstract class MTextureManager {

    @Shadow @Final private Set<class_1061> tickableTextures;

    /**
     * @author
     */
    @Overwrite
    public void tick() {
        if (Renderer.skipRendering)
            return;

        //Debug D
        for (class_1061 tickable : this.tickableTextures) {
            tickable.method_4622();
        }

        SpriteUpdateUtil.transitionLayouts();
    }
}
