package caeruleusTait.world.preview.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class WorldPreviewClient implements ClientModInitializer {

    public static final ShaderProgram HSV_SHADER = new ShaderProgram(
            ResourceLocation.tryBuild("world_preview", "core/hsv"),
            DefaultVertexFormat.POSITION_COLOR,
            ShaderDefines.EMPTY
    );

    @Override
    public void onInitializeClient() {
        CoreShaders.getProgramsToPreload().add(HSV_SHADER);
    }

    public static void renderTexture(AbstractTexture texture, double xMin, double yMin, double xMax, double yMax) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        RenderSystem.setShaderTexture(0, texture.getId());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        bufferBuilder.addVertex((float) xMin, (float) yMax, 0.0F).setUv(0.0F, 1.0F);
        bufferBuilder.addVertex((float) xMax, (float) yMax, 0.0F).setUv(1.0F, 1.0F);
        bufferBuilder.addVertex((float) xMax, (float) yMin, 0.0F).setUv(1.0F, 0.0F);
        bufferBuilder.addVertex((float) xMin, (float) yMin, 0.0F).setUv(0.0F, 0.0F);
        try (MeshData data = bufferBuilder.buildOrThrow()) {
            BufferUploader.drawWithShader(data);
        }
    }

    public static String toTitleCase(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        return Arrays
                .stream(input.split(" "))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));
    }
}