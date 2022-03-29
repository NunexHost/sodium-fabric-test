package net.caffeinemc.sodium.render.chunk.region;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.sodium.render.arena.AsyncBufferArena;
import net.caffeinemc.sodium.render.arena.BufferArena;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexFormats;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.Validate;

import java.util.Set;

public class RenderRegion {
    public static final int REGION_WIDTH = 8;
    public static final int REGION_HEIGHT = 4;
    public static final int REGION_LENGTH = 8;

    private static final int REGION_WIDTH_M = RenderRegion.REGION_WIDTH - 1;
    private static final int REGION_HEIGHT_M = RenderRegion.REGION_HEIGHT - 1;
    private static final int REGION_LENGTH_M = RenderRegion.REGION_LENGTH - 1;

    private static final int REGION_WIDTH_SH = Integer.bitCount(REGION_WIDTH_M);
    private static final int REGION_HEIGHT_SH = Integer.bitCount(REGION_HEIGHT_M);
    private static final int REGION_LENGTH_SH = Integer.bitCount(REGION_LENGTH_M);

    public static final int REGION_SIZE = REGION_WIDTH * REGION_HEIGHT * REGION_LENGTH;

    private static final int REGION_EXCESS = 8;

    static {
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_WIDTH));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_HEIGHT));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_LENGTH));
    }

    private final RenderRegionManager manager;

    private final Set<RenderSection> chunks = new ObjectOpenHashSet<>();
    private Resources resources;

    private final int x, y, z;

    private Frustum.Visibility visibility;

    public RenderRegion(RenderRegionManager manager, int x, int y, int z) {
        this.manager = manager;

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static RenderRegion createRegionForChunk(RenderRegionManager manager, int x, int y, int z) {
        return new RenderRegion(manager, x >> REGION_WIDTH_SH, y >> REGION_HEIGHT_SH, z >> REGION_LENGTH_SH);
    }

    public Resources getResources() {
        return this.resources;
    }

    public void deleteResources() {
        if (this.resources != null) {
            this.resources.delete();
            this.resources = null;
        }
    }

    public static long getRegionKeyForChunk(int x, int y, int z) {
        return ChunkSectionPos.asLong(x >> REGION_WIDTH_SH, y >> REGION_HEIGHT_SH, z >> REGION_LENGTH_SH);
    }

    public int getOriginX() {
        return this.x << REGION_WIDTH_SH << 4;
    }

    public int getOriginY() {
        return this.y << REGION_HEIGHT_SH << 4;
    }

    public int getOriginZ() {
        return this.z << REGION_LENGTH_SH << 4;
    }

    public Resources getOrCreateArenas() {
        Resources arenas = this.resources;

        if (arenas == null) {
            this.resources = (arenas = this.manager.createRegionArenas());
        }

        return arenas;
    }

    public void addChunk(RenderSection chunk) {
        if (!this.chunks.add(chunk)) {
            throw new IllegalStateException("Chunk " + chunk + " is already a member of region " + this);
        }
    }

    public void removeChunk(RenderSection chunk) {
        if (!this.chunks.remove(chunk)) {
            throw new IllegalStateException("Chunk " + chunk + " is not a member of region " + this);
        }
    }

    public boolean isEmpty() {
        return this.chunks.isEmpty();
    }

    public int getChunkCount() {
        return this.chunks.size();
    }

    public void updateVisibility(Frustum frustum) {
        int x = this.getOriginX();
        int y = this.getOriginY();
        int z = this.getOriginZ();

        // HACK: Regions need to be slightly larger than their real volume
        // Otherwise, the first node in the iteration graph might be incorrectly culled when the camera
        // is at the extreme end of a region
        this.visibility = frustum.testBox(x - REGION_EXCESS, y - REGION_EXCESS, z - REGION_EXCESS,
                x + (REGION_WIDTH << 4) + REGION_EXCESS, y + (REGION_HEIGHT << 4) + REGION_EXCESS, z + (REGION_LENGTH << 4) + REGION_EXCESS);
    }

    public Frustum.Visibility getVisibility() {
        return this.visibility;
    }

    public static int getChunkIndex(int x, int y, int z) {
        return (x * RenderRegion.REGION_LENGTH * RenderRegion.REGION_HEIGHT) + (y * RenderRegion.REGION_LENGTH) + z;
    }

    public static class Resources {
        public final BufferArena vertexBuffers;

        public Resources(RenderDevice device, TerrainVertexType vertexType) {
            this.vertexBuffers = new AsyncBufferArena(device, REGION_SIZE * 756, vertexType.getBufferVertexFormat().stride());
        }

        public void delete() {
            this.vertexBuffers.delete();
        }

        public boolean isEmpty() {
            return this.vertexBuffers.isEmpty();
        }

        public long getDeviceUsedMemory() {
            return this.vertexBuffers.getDeviceUsedMemory();
        }

        public long getDeviceAllocatedMemory() {
            return this.vertexBuffers.getDeviceAllocatedMemory();
        }
    }
}