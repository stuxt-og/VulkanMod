package net.vulkanmod.render.chunk.build.frapi.render;
/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.vulkanmod.render.chunk.build.frapi.helper.ColorHelper;
import net.vulkanmod.render.chunk.build.frapi.mesh.MutableQuadViewImpl;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Math;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.class_4696;
import net.minecraft.class_5819;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.ARGB;

public class SimpleBlockRenderContext extends AbstractRenderContext {
    public static final ThreadLocal<SimpleBlockRenderContext> POOL = ThreadLocal.withInitial(SimpleBlockRenderContext::new);

    private final class_5819 random = class_5819.method_43047();

    private MultiBufferSource vertexConsumers;
    private RenderType defaultRenderLayer;
    private float red;
    private float green;
    private float blue;
    private int light;

    @Nullable
    private RenderType lastRenderLayer;
    @Nullable
    private VertexConsumer lastVertexConsumer;

    @Override
    protected void bufferQuad(MutableQuadViewImpl quad) {
        final RenderMaterial mat = quad.material();
        final BlendMode blendMode = mat.blendMode();
        final RenderType renderLayer = blendMode == BlendMode.DEFAULT ? defaultRenderLayer : blendMode.blockRenderLayer;
        final VertexConsumer vertexConsumer;

        if (renderLayer == lastRenderLayer) {
            vertexConsumer = lastVertexConsumer;
        } else {
            lastVertexConsumer = vertexConsumer = vertexConsumers.getBuffer(renderLayer);
            lastRenderLayer = renderLayer;
        }

        tintQuad(quad);
        shadeQuad(quad, mat.emissive());
        bufferQuad(quad, vertexConsumer);
    }

    private void tintQuad(MutableQuadViewImpl quad) {
        if (quad.tintIndex() != -1) {
            final float red = this.red;
            final float green = this.green;
            final float blue = this.blue;

            for (int i = 0; i < 4; i++) {
                quad.color(i, ARGB.method_64602(quad.color(i), red, green, blue));
            }
        }
    }

    private void shadeQuad(MutableQuadViewImpl quad, boolean emissive) {
        if (emissive) {
            for (int i = 0; i < 4; i++) {
                quad.lightmap(i, LightTexture.FULL_BRIGHT);
            }
        } else {
            final int light = this.light;

            for (int i = 0; i < 4; i++) {
                quad.lightmap(i, ColorHelper.maxLight(quad.lightmap(i), light));
            }
        }
    }

    public void bufferModel(PoseStack.Pose entry, MultiBufferSource vertexConsumers, BlockStateModel model, float red, float green, float blue, int light, int overlay, BlockAndTintGetter blockView, BlockPos pos, BlockState state) {
        matrices = entry;
        this.overlay = overlay;

        this.vertexConsumers = vertexConsumers;
        this.defaultRenderLayer = class_4696.method_23683(state);
        this.red = Math.clamp(red, 0, 1);
        this.green = Math.clamp(green, 0, 1);
        this.blue = Math.clamp(blue, 0, 1);
        this.light = light;

        random.method_43052(42L);

        model.emitQuads(getEmitter(), blockView, pos, state, random, cullFace -> false);

        matrices = null;
        this.vertexConsumers = null;
        lastRenderLayer = null;
        lastVertexConsumer = null;
    }
}

