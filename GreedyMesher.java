package net.vulkanmod.render.chunk.mesh;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.vulkanmod.render.vertex.TerrainRenderType;

import javax.swing.text.html.BlockView;
import java.util.*;

import static net.minecraft.world.level.block.RenderShape.MODEL;

public class GreedyMesher {
    public static final GreedyMesher INSTANCE = new GreedyMesher();
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Direction[][] AXIS_DIRECTIONS = {
            {Direction.WEST, Direction.EAST},   // Axis 0: X
            {Direction.DOWN, Direction.UP},      // Axis 1: Y
            {Direction.NORTH, Direction.SOUTH}  // Axis 2: Z
    };

    private GreedyMesher() {} // Singleton

    /**
     * Перевіряє, чи можна greedy-мерджити блок (для 1.21.5+ з новими блоками).
     * Додано перевірки для нових Spring to Life блоків (Bush, Dry Grass тощо).
     */
    public boolean canMerge(BlockState state, BakedModel model, BlockView world, BlockPos pos) {
        if (state.getRenderShape() != MODEL) {
            return false;
        }
        if (model == null || model.isVanillaAdapter()) {
            return false;
        }
        // Швидкий чек на повний куб (працює для 95% ванільних/модових)
        if (!Block.isShapeFullBlock(state.getVisualShape(null, pos, CollisionContext.empty()))) {
            return false;
        }

        // Виключаємо offset, TE, затемнення
        if (!state.getOffset(null, pos).equals(Vec3.ZERO) ||
                state.hasBlockEntity() || state.getLightBlock(null, pos) < 15) {
            return false;
        }

        List<BakedQuad> quads = model.getQuads(state, null, RANDOM);
        return quads.size() == 6 && !state.emissiveRendering(null, pos);
    }

    /**
     * Емітить greedy quads для секції (16x16x16).
     * Викликається з BlockRenderer.renderBlock для кубічних блоків.
     */
    public void emitGreedyQuads(BlockAndTintGetter region, BlockPos sectionOrigin,
                                VertexConsumer consumer, TerrainRenderType renderType,
                                BlockRenderDispatcher blockRenderManager) {

        for (int axis = 0; axis < 3; axis++) {
            for (int side = 0; side < 2; side++) {
                Direction face = AXIS_DIRECTIONS[axis][side];
                Direction opposite = face.getOpposite();

                int dx = opposite.getStepX();
                int dy = opposite.getStepY();
                int dz = opposite.getStepZ();

                boolean[][] mask = new boolean[16][16];

                // 1. Заповнюємо маску видимості
                for (int u = 0; u < 16; u++) {
                    for (int v = 0; v < 16; v++) {
                        BlockPos pos = getFacePos(sectionOrigin, axis, side, u, v);
                        BlockState state = region.getBlockState(pos);
                        BlockPos neighborPos = pos.offset(dx, dy, dz);
                        BlockState neighbor = region.getBlockState(neighborPos);

                        mask[u][v] = state.isSolidRender(null, pos) &&
                                !neighbor.isSolidRender(null, pos);
                    }
                }

                // 2. Greedy meshing — ВИПРАВЛЕНИЙ ЦИКЛ
                int u = 0;
                while (u < 16) {
                    int v = 0;
                    while (v < 16) {
                        if (!mask[u][v]) {
                            v++;
                            continue;
                        }

                        BlockPos basePos = getFacePos(sectionOrigin, axis, side, u, v);
                        BlockState baseState = region.getBlockState(basePos);

                        // Розширюємо по ширині (u)
                        int width = 1;
                        while (u + width < 16 && mask[u + width][v] &&
                                region.getBlockState(getFacePos(sectionOrigin, axis, side, u + width, v)).equals(baseState))
                            width++;

                        // Розширюємо по висоті (v)
                        int height = 1;
                        while (v + height < 16) {
                            boolean canExpand = true;
                            for (int x = 0; x < width; x++) {
                                if (!mask[u + x][v + height] ||
                                        !region.getBlockState(getFacePos(sectionOrigin, axis, side, u + x, v + height)).equals(baseState)) {
                                    canExpand = false;
                                    break;
                                }
                            }
                            if (!canExpand) break;
                            height++;
                        }

                        // Еміт великий квад
                        emitMergedQuad(consumer, baseState, face, axis, side, u, v, width, height, blockRenderManager);

                        // Очищаємо оброблену область
                        for (int x = 0; x < width; x++) {
                            for (int y = 0; y < height; y++) {
                                mask[u + x][v + y] = false;
                            }
                        }

                        v += height;  // Переходимо нижче
                    }
                    u++;  // Наступний стовпець
                }
            }
        }
    }

    private void emitMergedQuad(VertexConsumer consumer, BlockState state,
                                Direction face, int axis, int side, int u, int v, int w, int h,
                                BlockRenderDispatcher blockRenderManager) {
        BakedModel model = blockRenderManager.getBlockModel(state);
        List<BakedQuad> quads = model.getQuads(state, face, RandomSource.create());

        if (quads.isEmpty()) return; // Безпека

        BakedQuad template = quads.getFirst(); // Беремо перший quad з потрібної сторони

        // Дані з шаблону
        int[] vertexData = template.getVertices();

        float[] pos = new float[3];
        float[] uv = new float[2];
        int light = 0;
        float nx = face.getStepX(), ny = face.getStepY(), nz = face.getStepZ();

        float x0, y0, z0, x1, y1, z1;

        if (axis == 0) { // X
            x0 = x1 = side == 0 ? 0.0f : 16.0f;
            y0 = u;      y1 = u + w;
            z0 = v;      z1 = v + h;
        } else if (axis == 1) { // Y
            y0 = y1 = side == 0 ? 0.0f : 16.0f;
            x0 = u;      x1 = u + w;
            z0 = v;      z1 = v + h;
        } else { // Z
            z0 = z1 = side == 0 ? 0.0f : 16.0f;
            x0 = u;      x1 = u + w;
            y0 = v;      y1 = v + h;
        }

        // Позиції вершин (в порядку як у vanilla: 0,1,2,3)
        float[][] vertices = {
                {x0, y0, z0}, // 0
                {x0, y1, z0}, // 1
                {x1, y1, z1}, // 2
                {x1, y0, z1}  // 3
        };

        // UV з шаблону (беремо з першої вершини і масштабимо)
        float uMin = Float.intBitsToFloat(vertexData[4]);
        float vMin = Float.intBitsToFloat(vertexData[5]);
        float uMax = Float.intBitsToFloat(vertexData[4 + 7 * 8]); // 4-та вершина
        float vMax = Float.intBitsToFloat(vertexData[5 + 7 * 8]);

        float[][] uvs = {
                {uMin, vMin},
                {uMin, vMax},
                {uMax, vMax},
                {uMax, vMin}
        };

        // Колір (ABGR → RGBA)
        int tintIndex = template.getTintIndex();
        int tint = tintIndex == -1 ? 0xFFFFFFFF : Minecraft.getInstance().getBlockColors().getColor(state, null, null, tintIndex);

        // Lightmap (беремо з шаблону, припускаємо, що AO і light однакові по кваду)
        light = vertexData[6]; // lightmap у першої вершини

        // Еміт 4 вершини
        for (int i = 0; i < 4; i++) {
            pos[0] = vertices[i][0];
            pos[1] = vertices[i][1];
            pos[2] = vertices[i][2];

            uv[0] = uvs[i][0];
            uv[1] = uvs[i][1];

            // Колір з tint (якщо є)
            int r = (tint >> 16 & 255);
            int g = (tint >> 8  & 255);
            int b = (tint       & 255);
            int a = (tint >> 24 & 255);
            if (a == 0) a = 255;

            consumer.addVertex(pos[0], pos[1], pos[2])
                    .setColor(r, g, b, a)
                    .setUv(uv[0], uv[1])
                    .setLight(light)
                    .setOverlay(10) // 1 аргумент
                    .setNormal(nx, ny, nz);
        }
    }

    private static BlockPos getFacePos(BlockPos origin, int axis, int side, int u, int v) {
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();

        if (axis == 0) { // X-вісь
            x += side == 0 ? 0 : 15;
            y += u;
            z += v;
        } else if (axis == 1) { // Y-вісь
            y += side == 0 ? 0 : 15;
            x += u;
            z += v;
        } else { // Z-вісь
            z += side == 0 ? 0 : 15;
            x += u;
            y += v;
        }
        return new BlockPos(x, y, z);
    }

    // Helper: отримати позицію блоку для фейсу
    private BlockPos getPosForFace(BlockPos origin, int axis, int dir, int u, int v, Direction direction) {
        // Логіка для осей: dir=0 — негативний, dir=1 — позитивний
        int dx = (axis == 0) ? (dir == 0 ? 0 : 15) : ((axis == 1) ? u : v);
        int dy = (axis == 1) ? (dir == 0 ? 0 : 15) : ((axis == 0) ? u : v);
        int dz = (axis == 2) ? (dir == 0 ? 0 : 15) : ((axis == 0) ? v : u);
        return origin.offset(dx, dy, dz);
    }
}