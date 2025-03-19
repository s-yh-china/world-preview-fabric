package caeruleusTait.world.preview.backend.color;

import caeruleusTait.world.preview.WorldPreview;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

import static caeruleusTait.world.preview.WorldPreview.LOGGER;

public class ColormapReloadListener extends SimpleJsonResourceReloadListener<ColorMap.RawColorMap> {

    public ColormapReloadListener() {
        super(ColorMap.RawColorMap.CODEC, FileToIdConverter.json("colormap_preview"));
    }

    @Override
    protected void apply(Map<ResourceLocation, ColorMap.RawColorMap> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        final WorldPreview worldPreview = WorldPreview.get();
        final PreviewMappingData previewMappingData = worldPreview.biomeColorMap();
        previewMappingData.clearColorMappings();

        LOGGER.debug("Loading colormaps:");
        for (Map.Entry<ResourceLocation, ColorMap.RawColorMap> entry : object.entrySet()) {
            final ColorMap.RawColorMap value = entry.getValue();
            LOGGER.debug(" - {}: {} | {} entries", entry.getKey(), value.name(), value.data().size());
            previewMappingData.addColormap(new ColorMap(entry.getKey(), value));
        }
    }
}
