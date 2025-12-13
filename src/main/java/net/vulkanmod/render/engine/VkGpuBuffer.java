package net.vulkanmod.render.engine;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.MemoryType;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import org.jetbrains.annotations.Nullable;

import static org.lwjgl.vulkan.VK10.*;

@Environment(EnvType.CLIENT)
public class VkGpuBuffer extends GpuBuffer {
    protected boolean closed;
    protected boolean initialized = false;
    @Nullable protected final Supplier<String> label;

    Buffer buffer;

    protected VkGpuBuffer(VkDebugLabel glDebugLabel, @Nullable Supplier<String> supplier, BufferType bufferType, BufferUsage bufferUsage, int size) {
        super(bufferType, bufferUsage, size);
        this.label = supplier;

        int usage = switch (bufferType) {
            case VERTICES -> VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
            case INDICES -> VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
            case COPY_READ, PIXEL_UNPACK -> VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
            case COPY_WRITE, PIXEL_PACK -> VK_BUFFER_USAGE_TRANSFER_DST_BIT;

            default -> throw new IllegalStateException("Unsupported buffertype: " + bufferType);
        };

        MemoryType memoryType = switch (bufferUsage) {
            case DYNAMIC_WRITE, DYNAMIC_READ, DYNAMIC_COPY, STATIC_READ -> MemoryTypes.HOST_MEM;
            case STATIC_WRITE, STATIC_COPY -> MemoryTypes.GPU_MEM;

            default -> throw new IllegalStateException("Unsupported buffertype: " + bufferUsage);
        };

        this.buffer = new Buffer(usage, memoryType);

        if (bufferUsage.isReadable()) {
            this.buffer.createBuffer(this.size);
            this.initialized = true;
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;

            MemoryManager.getInstance().addToFreeable(this.buffer);
        }
    }

    public Buffer getBuffer() {
        return buffer;
    }

    @Environment(EnvType.CLIENT)
    public static class ReadView implements GpuBuffer.ReadView {
        private final int target;
        private final ByteBuffer data;

        protected ReadView(int i, ByteBuffer byteBuffer) {
            this.target = i;
            this.data = byteBuffer;
        }

        @Override
        public ByteBuffer data() {
            return this.data;
        }

        @Override
        public void close() {
//            GlStateManager._glUnmapBuffer(this.target);
        }
    }
}

