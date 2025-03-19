package caeruleusTait.world.preview.backend.color;

import caeruleusTait.world.preview.WorldPreview;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

import static caeruleusTait.world.preview.WorldPreview.LOGGER;

public class HeightmapPresetReloadListener extends SimpleJsonResourceReloadListener<PreviewData.HeightmapPresetData> {

    public HeightmapPresetReloadListener() {
        super(PreviewData.HeightmapPresetData.CODEC, FileToIdConverter.json("heightmap_preview_presets"));
    }

    @Override
    protected void apply(Map<ResourceLocation, PreviewData.HeightmapPresetData> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        final WorldPreview worldPreview = WorldPreview.get();
        final PreviewMappingData previewMappingData = worldPreview.biomeColorMap();
        previewMappingData.clearHeightmapPresets();

        LOGGER.debug("Loading heightmap presets:");
        for (Map.Entry<ResourceLocation, PreviewData.HeightmapPresetData> entry : object.entrySet()) {
            final PreviewData.HeightmapPresetData value = entry.getValue();
            LOGGER.debug(" - {}: {} | {} to {}", entry.getKey(), value.name(), value.minY(), value.maxY());
            previewMappingData.addHeightmapPreset(value);
        }
    }
}
