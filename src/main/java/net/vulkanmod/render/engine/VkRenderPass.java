package net.vulkanmod.render.engine;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.vulkanmod.interfaces.shader.ExtendedRenderPipeline;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class VkRenderPass implements RenderPass {
    protected static final int MAX_VERTEX_BUFFERS = 1;
    public static final boolean VALIDATION = SharedConstants.IS_RUNNING_IN_IDE;
    private final VkCommandEncoder encoder;
    private final boolean hasDepthTexture;
    private boolean closed;
    @Nullable
    protected RenderPipeline pipeline;
    protected final GpuBuffer[] vertexBuffers = new GpuBuffer[1];
    @Nullable
    protected GpuBuffer indexBuffer;
    protected VertexFormat.IndexType indexType = VertexFormat.IndexType.INT;
    protected final ScissorState scissorState = new ScissorState();
    protected final HashMap<String, Object> uniforms = new HashMap<>();
    protected final HashMap<String, GpuTexture> samplers = new HashMap<>();
    protected final Set<String> dirtyUniforms = new HashSet<>();
    protected final Set<String> dirtySamplers = new HashSet<>();

    public VkRenderPass(VkCommandEncoder glCommandEncoder, boolean bl) {
        this.encoder = glCommandEncoder;
        this.hasDepthTexture = bl;
    }

    public boolean hasDepthTexture() {
        return this.hasDepthTexture;
    }

    @Override
    public void setPipeline(RenderPipeline renderPipeline) {
        if (this.pipeline == null || this.pipeline != renderPipeline) {
            this.dirtyUniforms.addAll(this.uniforms.keySet());
            this.dirtySamplers.addAll(this.samplers.keySet());
        }

//        this.pipeline = this.encoder.getDevice().getOrCompilePipeline(renderPipeline);


        this.pipeline = renderPipeline;

        if (ExtendedRenderPipeline.of(renderPipeline).getPipeline() == null) {
            this.encoder.getDevice().compilePipeline(renderPipeline);
        }
    }

    @Override
    public void bindSampler(String string, GpuTexture gpuTexture) {
        this.samplers.put(string, gpuTexture);
        this.dirtySamplers.add(string);
    }

    @Override
    public void setUniform(String string, int... is) {
        this.uniforms.put(string, is);
        this.dirtyUniforms.add(string);
    }

    @Override
    public void setUniform(String string, float... fs) {
        this.uniforms.put(string, fs);
        this.dirtyUniforms.add(string);
    }

    @Override
    public void setUniform(String string, Matrix4f matrix4f) {
        this.uniforms.put(string, matrix4f.get(new float[16]));
        this.dirtyUniforms.add(string);
    }

    @Override
    public void enableScissor(ScissorState scissorState) {
        this.scissorState.copyFrom(scissorState);
    }

    @Override
    public void enableScissor(int i, int j, int k, int l) {
        this.scissorState.enable(i, j, k, l);
    }

    @Override
    public void disableScissor() {
        this.scissorState.disable();
    }

    @Override
    public void setVertexBuffer(int i, GpuBuffer gpuBuffer) {
        if (i >= 0 && i < 1) {
            this.vertexBuffers[i] = gpuBuffer;
        } else {
            throw new IllegalArgumentException("Vertex buffer slot is out of range: " + i);
        }
    }

    @Override
    public void setIndexBuffer(@Nullable GpuBuffer gpuBuffer, VertexFormat.IndexType indexType) {
        this.indexBuffer = gpuBuffer;
        this.indexType = indexType;
    }

    @Override
    public void drawIndexed(int i, int j) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.encoder.executeDraw(this, i, j, this.indexType);
        }
    }

    @Override
    public void drawMultipleIndexed(Collection<RenderPass.Draw> collection, @Nullable GpuBuffer gpuBuffer, @Nullable VertexFormat.IndexType indexType) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.encoder.executeDrawMultiple(this, collection, gpuBuffer, indexType);
        }
    }

    @Override
    public void draw(int i, int j) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.encoder.executeDraw(this, i, j, null);
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.encoder.finishRenderPass();
        }
    }

    public @Nullable RenderPipeline getPipeline() {
        return pipeline;
    }
}

