package net.vulkanmod.render.chunk.build.frapi;

import java.util.HashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.FabricBlockModelRenderer;
import net.fabricmc.fabric.api.renderer.v1.render.RenderLayerHelper;
import net.fabricmc.fabric.mixin.client.indigo.renderer.BlockRenderManagerAccessor;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.vulkanmod.render.chunk.build.frapi.material.MaterialFinderImpl;
import net.vulkanmod.render.chunk.build.frapi.mesh.MutableMeshImpl;
import net.vulkanmod.render.chunk.build.frapi.render.BlockRenderContext;
import net.vulkanmod.render.chunk.build.frapi.render.SimpleBlockRenderContext;

/**
 * The Fabric default renderer implementation. Supports all
 * features defined in the API except shaders and offers no special materials.
 */
public class VulkanModRenderer implements Renderer {
	public static final VulkanModRenderer INSTANCE = new VulkanModRenderer();

	public static final RenderMaterial STANDARD_MATERIAL = INSTANCE.materialFinder().find();

	static {
		INSTANCE.registerMaterial(RenderMaterial.STANDARD_ID, STANDARD_MATERIAL);
	}

	private final HashMap<ResourceLocation, RenderMaterial> materialMap = new HashMap<>();

	private VulkanModRenderer() {}

	@Override
	public MutableMesh mutableMesh() {
		return new MutableMeshImpl();
	}

	@Override
	public MaterialFinder materialFinder() {
		return new MaterialFinderImpl();
	}

	@Override
	public RenderMaterial materialById(ResourceLocation id) {
		return materialMap.get(id);
	}

	@Override
	public boolean registerMaterial(ResourceLocation id, RenderMaterial material) {
		if (materialMap.containsKey(id)) return false;

		// cast to prevent acceptance of impostor implementations
		materialMap.put(id, material);
		return true;
	}

	@Override
	public void render(ModelBlockRenderer modelBlockRenderer, BlockAndTintGetter blockAndTintGetter,
					   BlockStateModel blockStateModel, BlockState blockState, BlockPos blockPos, PoseStack poseStack,
					   MultiBufferSource multiBufferSource, boolean cull, long seed, int overlay) {
		BlockRenderContext.POOL.get().render(blockAndTintGetter, blockStateModel, blockState, blockPos, poseStack, multiBufferSource, cull, seed, overlay);
	}

	@Override
	public void render(PoseStack.Pose pose, MultiBufferSource multiBufferSource, BlockStateModel blockStateModel,
					   float v, float v1, float v2, int i, int i1, BlockAndTintGetter blockAndTintGetter,
					   BlockPos blockPos, BlockState blockState) {
		SimpleBlockRenderContext.POOL.get().bufferModel(pose, multiBufferSource, blockStateModel, v, v1, v2, i, i1, blockAndTintGetter, blockPos, blockState);
	}

	@Override
	public void renderBlockAsEntity(BlockRenderDispatcher blockRenderDispatcher, BlockState blockState,
									PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay,
									BlockAndTintGetter blockAndTintGetter, BlockPos pos) {
		RenderShape blockRenderType = blockState.getRenderShape();

		if (blockRenderType != RenderShape.INVISIBLE) {
			BlockStateModel model = blockRenderDispatcher.getBlockModel(blockState);
			int tint = ((BlockRenderManagerAccessor) blockRenderDispatcher).getBlockColors().getColor(blockState, null, null, 0);
			float red = (tint >> 16 & 255) / 255.0F;
			float green = (tint >> 8 & 255) / 255.0F;
			float blue = (tint & 255) / 255.0F;
			FabricBlockModelRenderer.render(poseStack.last(), layer -> multiBufferSource.getBuffer(RenderLayerHelper.getEntityBlockLayer(layer)), model, red, green, blue, light, overlay, blockAndTintGetter, pos, blockState);
			((BlockRenderManagerAccessor) blockRenderDispatcher).getBlockEntityModelsGetter().get().renderByBlock(blockState.getBlock(), ItemDisplayContext.NONE, poseStack, multiBufferSource, light, overlay);
		}
	}

	@Override
	public QuadEmitter getLayerRenderStateEmitter(ItemStackRenderState.LayerRenderState layerRenderState) {
		return null;
	}
}
