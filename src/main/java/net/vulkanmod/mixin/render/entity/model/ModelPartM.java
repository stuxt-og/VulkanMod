package net.vulkanmod.mixin.render.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import net.vulkanmod.interfaces.ModelPartCubeMixed;
import net.vulkanmod.render.model.CubeModel;
import net.vulkanmod.render.vertex.format.I32_SNorm;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ModelPart.class)
public abstract class ModelPartM {
    @Shadow @Final private List<ModelPart.class_628> cubes;

    Vector3f normal = new Vector3f();

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void injCompile(PoseStack.Pose pose, VertexConsumer vertexConsumer, int light, int overlay, int color, CallbackInfo ci) {
        this.renderCubes(pose, vertexConsumer, light, overlay, color);
        ci.cancel();
    }

    @Unique
    public void renderCubes(PoseStack.Pose pose, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        Matrix4f matrix4f = pose.method_23761();
        Matrix3f matrix3f = pose.method_23762();

        ExtendedVertexBuilder vertexBuilder = ExtendedVertexBuilder.of(vertexConsumer);

        if (vertexBuilder != null && vertexBuilder.canUseFastVertex()) {
            color = ColorUtil.RGBA.fromArgb32(color);

            for (ModelPart.class_628 cube : this.cubes) {
                ModelPartCubeMixed cubeMixed = (ModelPartCubeMixed)(cube);
                CubeModel cubeModel = cubeMixed.getCubeModel();

                ModelPart.Polygon[] polygons = cubeModel.getPolygons();

                cubeModel.transformVertices(matrix4f);

                for (ModelPart.Polygon polygon : polygons) {
                    matrix3f.transform(this.normal.set(polygon.comp_3185()));
                    this.normal.normalize();

                    int packedNormal = I32_SNorm.packNormal(normal.x(), normal.y(), normal.z());

                    ModelPart.Vertex[] vertices = polygon.comp_3184();

                    for (ModelPart.Vertex vertex : vertices) {
                        Vector3f pos = vertex.comp_3186();
                        vertexBuilder.vertex(pos.x(), pos.y(), pos.z(), color, vertex.comp_3187(), vertex.comp_3188(), overlay, light, packedNormal);
                    }
                }
            }
        }
        else {
            for (ModelPart.class_628 cube : this.cubes) {
                ModelPartCubeMixed cubeMixed = (ModelPartCubeMixed)(cube);
                CubeModel cubeModel = cubeMixed.getCubeModel();

                ModelPart.Polygon[] polygons = cubeModel.getPolygons();

                cubeModel.transformVertices(matrix4f);

                for (ModelPart.Polygon polygon : polygons) {
                    matrix3f.transform(this.normal.set(polygon.comp_3185()));
                    this.normal.normalize();

                    ModelPart.Vertex[] vertices = polygon.comp_3184();

                    for (ModelPart.Vertex vertex : vertices) {
                        Vector3f pos = vertex.comp_3186();
                        vertexConsumer.method_23919(pos.x(), pos.y(), pos.z(), color, vertex.comp_3187(), vertex.comp_3188(), overlay, light,
                                              normal.x(), normal.y(), normal.z());
                    }
                }
            }
        }

    }
}
