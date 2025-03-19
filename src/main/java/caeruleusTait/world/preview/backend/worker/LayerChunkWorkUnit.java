package caeruleusTait.world.preview.backend.worker;

import caeruleusTait.world.preview.backend.color.PreviewData;
import caeruleusTait.world.preview.backend.sampler.ChunkSampler;
import caeruleusTait.world.preview.backend.storage.PreviewStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public class LayerChunkWorkUnit extends WorkUnit {
    private final ChunkSampler sampler;

    public LayerChunkWorkUnit(ChunkSampler sampler, ChunkPos pos, SampleUtils sampleUtils, PreviewData previewData, int y) {
        super(sampleUtils, pos, previewData, y);
        this.sampler = sampler;
    }

    @Override
    protected List<WorkResult> doWork() {
        if (sampleUtils.hasRawNoiseInfo()) {
            return doRawNoiseWork();
        } else {
            return doNormalWork();
        }
    }

    private List<WorkResult> doNormalWork() {
        WorkResult res = new WorkResult(this, QuartPos.fromBlock(y), primarySection, new ArrayList<>(16), List.of());
        for (BlockPos p : sampler.blocksForChunk(chunkPos, y)) {
            final var sample = sampleUtils.doSample(p);
            sampler.expandRaw(p, biomeIdFrom(sample.biome()), res);
        }
        return List.of(res);
    }

    private List<WorkResult> doRawNoiseWork() {
        WorkResult res             = new WorkResult(this, QuartPos.fromBlock(y), primarySection, new ArrayList<>(16), List.of());
        WorkResult temperature     = new WorkResult(this, QuartPos.fromBlock(y), storage.section4(chunkPos, y, PreviewStorage.FLAG_NOISE_TEMPERATURE),     new ArrayList<>(16), List.of());
        WorkResult humidity        = new WorkResult(this, QuartPos.fromBlock(y), storage.section4(chunkPos, y, PreviewStorage.FLAG_NOISE_HUMIDITY),        new ArrayList<>(16), List.of());
        WorkResult continentalness = new WorkResult(this, QuartPos.fromBlock(y), storage.section4(chunkPos, y, PreviewStorage.FLAG_NOISE_CONTINENTALNESS), new ArrayList<>(16), List.of());
        WorkResult erosion         = new WorkResult(this, QuartPos.fromBlock(y), storage.section4(chunkPos, y, PreviewStorage.FLAG_NOISE_EROSION),         new ArrayList<>(16), List.of());
        WorkResult depth           = new WorkResult(this, QuartPos.fromBlock(y), storage.section4(chunkPos, y, PreviewStorage.FLAG_NOISE_DEPTH),           new ArrayList<>(16), List.of());
        WorkResult weirdness       = new WorkResult(this, QuartPos.fromBlock(y), storage.section4(chunkPos, y, PreviewStorage.FLAG_NOISE_WEIRDNESS),       new ArrayList<>(16), List.of());
        for (BlockPos p : sampler.blocksForChunk(chunkPos, y)) {
            final var sample = sampleUtils.doSample(p);
            sampler.expandRaw(p, biomeIdFrom(sample.biome()), res);
            sampler.expandRaw(p, sample.noiseResult()[0], temperature);
            sampler.expandRaw(p, sample.noiseResult()[1], humidity);
            sampler.expandRaw(p, sample.noiseResult()[2], continentalness);
            sampler.expandRaw(p, sample.noiseResult()[3], erosion);
            sampler.expandRaw(p, sample.noiseResult()[4], depth);
            sampler.expandRaw(p, sample.noiseResult()[5], weirdness);
        }
        return List.of(res, temperature, humidity, continentalness, erosion, depth, weirdness);
    }

    @Override
    public long flags() {
        return PreviewStorage.FLAG_BIOME;
    }
}
