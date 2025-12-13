package net.vulkanmod.mixin.vertex;

import net.minecraft.class_1058;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.class_4723;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO move
@Mixin(class_4723.class)
public class SpriteCoordinateExpanderM implements ExtendedVertexBuilder {
    @Shadow @Final private class_1058 sprite;

    @Unique
    private ExtendedVertexBuilder extDelegate;
    @Unique
    private boolean canUseFastVertex = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void getExtBuilder(VertexConsumer vertexConsumer, class_1058 textureAtlasSprite, CallbackInfo ci) {
        if (vertexConsumer instanceof ExtendedVertexBuilder) {
            this.extDelegate = (ExtendedVertexBuilder) vertexConsumer;
            this.canUseFastVertex = true;
        }
    }

    @Override
    public boolean canUseFastVertex() {
        return this.canUseFastVertex;
    }

    @Override
    public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal) {
        this.extDelegate.vertex(x, y, z, packedColor, this.sprite.method_4580(u), this.sprite.method_4570(v), overlay, light, packedNormal);
    }
}
