package caeruleusTait.world.preview.backend.worker;

import caeruleusTait.world.preview.WorldPreview;
import caeruleusTait.world.preview.WorldPreviewConfig;
import caeruleusTait.world.preview.backend.storage.PreviewLevel;
import caeruleusTait.world.preview.backend.stubs.DummyMinecraftServer;
import caeruleusTait.world.preview.backend.stubs.DummyServerLevelData;
import caeruleusTait.world.preview.backend.stubs.EmptyAquifer;
import caeruleusTait.world.preview.mixin.MinecraftServerAccessor;
import caeruleusTait.world.preview.mixin.NoiseBasedChunkGeneratorAccessor;
import caeruleusTait.world.preview.mixin.NoiseChunkAccessor;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Lifecycle;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.PathAllowList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static caeruleusTait.world.preview.WorldPreview.LOGGER;
import static net.minecraft.core.registries.Registries.LEVEL_STEM;

public class SampleUtils implements AutoCloseable {
    private final Path tempDir;
    private final DataFixer dataFixer;
    private final LevelStorageSource.LevelStorageAccess levelStorageAccess;
    private final LevelHeightAccessor levelHeightAccessor;
    private final CloseableResourceManager resourceManager;
    private final BiomeSource biomeSource;
    private final RandomState randomState;
    private final ChunkGenerator chunkGenerator;
    private final RegistryAccess registryAccess;
    private final ChunkGeneratorStructureState chunkGeneratorStructureState;
    private final StructureCheck structureCheck;
    private final StructureManager structureManager;
    private final StructureTemplateManager structureTemplateManager;
    private final PreviewLevel previewLevel;
    private final Registry<Structure> structureRegistry;
    private final ResourceKey<Level> dimension;
    private final NoiseGeneratorSettings noiseGeneratorSettings;
    private final MinecraftServer minecraftServer;
    private final ServerLevel serverLevel;
    private final WorldPreviewConfig cfg;

    /**
     * Create SampleUtils with a <b>real</b> Minecraft server
     */
    public SampleUtils(
            @NotNull MinecraftServer server,
            BiomeSource biomeSource,
            ChunkGenerator chunkGenerator,
            WorldOptions worldOptions,
            LevelStem levelStem,
            LevelHeightAccessor levelHeightAccessor
    ) throws IOException {
        this.cfg = WorldPreview.get().cfg();
        this.tempDir = null;
        this.minecraftServer = server;
        this.dataFixer = minecraftServer.getFixerUpper();
        this.levelStorageAccess = ((MinecraftServerAccessor) minecraftServer).getStorageSource();

        this.levelHeightAccessor = levelHeightAccessor;
        this.resourceManager = (CloseableResourceManager) minecraftServer.getResourceManager();
        this.biomeSource = biomeSource;
        this.chunkGenerator = chunkGenerator;
        this.registryAccess = minecraftServer.registryAccess();
        this.structureRegistry = this.registryAccess.lookupOrThrow(Registries.STRUCTURE);
        this.structureTemplateManager = minecraftServer.getStructureManager();
        this.previewLevel = new PreviewLevel(this.registryAccess, this.levelHeightAccessor);

        ResourceKey<LevelStem> levelStemResourceKey = this.registryAccess.lookupOrThrow(LEVEL_STEM)
                .getResourceKey(levelStem)
                .orElseThrow();
        dimension = Registries.levelStemToLevel(levelStemResourceKey);

        if (chunkGenerator instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator) {
            randomState = RandomState.create(
                    noiseBasedChunkGenerator.generatorSettings().value(),
                    registryAccess.lookupOrThrow(Registries.NOISE),
                    worldOptions.seed()
            );
        } else {
            randomState = RandomState.create(
                    NoiseGeneratorSettings.dummy(),
                    registryAccess.lookupOrThrow(Registries.NOISE),
                    worldOptions.seed()
            );
        }

        this.structureCheck = new StructureCheck(
                null,
                // Should never be required because `tryLoadFromStorage` must not be called
                this.registryAccess,
                this.structureTemplateManager,
                dimension,
                this.chunkGenerator,
                this.randomState,
                this.levelHeightAccessor,
                chunkGenerator.getBiomeSource(),
                worldOptions.seed(),
                dataFixer
        );
        this.structureManager = new StructureManager(previewLevel, worldOptions, structureCheck);
        this.chunkGeneratorStructureState = this.chunkGenerator.createState(
                this.registryAccess.lookupOrThrow(Registries.STRUCTURE_SET),
                this.randomState,
                worldOptions.seed()
        );

        if (chunkGenerator instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator) {
            noiseGeneratorSettings = noiseBasedChunkGenerator.generatorSettings().value();
        } else {
            noiseGeneratorSettings = null;
        }
        serverLevel = null;
    }

    /**
     * Create SampleUtils <b>and</b> a fake Minecraft server
     */
    public SampleUtils(
            BiomeSource biomeSource,
            ChunkGenerator chunkGenerator,
            LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess,
            WorldOptions worldOptions,
            LevelStem levelStem,
            LevelHeightAccessor levelHeightAccessor,
            WorldDataConfiguration worldDataConfiguration,
            Proxy proxy,
            @Nullable Path tempDataPackDir
    ) throws IOException, RuntimeException {
        this.cfg = WorldPreview.get().cfg();
        try {
            tempDir = Files.createTempDirectory("world_preview");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.dataFixer = DataFixers.getDataFixer();
        LevelStorageSource levelStorageSource = new LevelStorageSource(
                tempDir,
                tempDir.resolve("backups"),
                new DirectoryValidator(new PathAllowList(List.of())),
                dataFixer
        );
        this.levelStorageAccess = levelStorageSource.createAccess("world_preview");
        this.levelHeightAccessor = levelHeightAccessor;

        Path dataPackDir = levelStorageAccess.getLevelPath(LevelResource.DATAPACK_DIR);
        FileUtil.createDirectoriesSafe(dataPackDir);
        if (tempDataPackDir != null) {
            try (Stream<Path> stream = Files.walk(tempDataPackDir)) {
                stream.filter(x -> !x.equals(tempDataPackDir)).forEach(x -> {
                    try {
                        Util.copyBetweenDirs(tempDataPackDir, dataPackDir, x);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        this.biomeSource = biomeSource;
        this.chunkGenerator = chunkGenerator;
        this.registryAccess = layeredRegistryAccess.compositeAccess();
        this.structureRegistry = this.registryAccess.lookupOrThrow(Registries.STRUCTURE);
        this.previewLevel = new PreviewLevel(this.registryAccess, this.levelHeightAccessor);

        PackRepository packRepository = ServerPacksSource.createPackRepository(levelStorageAccess);
        resourceManager = (new WorldLoader.PackConfig(
                packRepository,
                worldDataConfiguration,
                false,
                false
        )).createResourceManager().getSecond();

        HolderGetter<Block> holderGetter = this.registryAccess.lookupOrThrow(Registries.BLOCK)
                .filterFeatures(worldDataConfiguration.enabledFeatures());
        this.structureTemplateManager = new StructureTemplateManager(
                resourceManager,
                levelStorageAccess,
                dataFixer,
                holderGetter
        );

        ResourceKey<LevelStem> levelStemResourceKey = this.registryAccess.lookupOrThrow(LEVEL_STEM)
                .getResourceKey(levelStem)
                .orElseThrow();
        dimension = Registries.levelStemToLevel(levelStemResourceKey);

        // Some mods listen on the <init> of MinecraftServer
        final int functionCompilationLevel = 0;
        final Executor executor = Executors.newSingleThreadExecutor();
        final LevelSettings levelSettings = new LevelSettings("temp", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(worldDataConfiguration.enabledFeatures()), worldDataConfiguration);
        List<Registry.PendingTags<?>> list = TagLoader.loadTagsForExistingRegistries(resourceManager, layeredRegistryAccess.getLayer(RegistryLayer.STATIC));
        final PrimaryLevelData primaryLevelData = new PrimaryLevelData(levelSettings, worldOptions, PrimaryLevelData.SpecialWorldProperty.NONE, Lifecycle.stable());
        final var future = ReloadableServerResources.loadResources(resourceManager, layeredRegistryAccess, list, worldDataConfiguration.enabledFeatures(), Commands.CommandSelection.DEDICATED, functionCompilationLevel, executor, executor);
        final ReloadableServerResources reloadableServerResources;
        try {
            reloadableServerResources = future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        reloadableServerResources.updateStaticRegistryTags();
        WorldStem worldStem = new WorldStem(resourceManager, reloadableServerResources, layeredRegistryAccess, primaryLevelData);

        final ChunkProgressListener chunkProgressListener = new ChunkProgressListener() {
            @Override
            public void updateSpawnPos(ChunkPos center) {

            }

            @Override
            public void onStatusChange(ChunkPos chunkPosition, @Nullable ChunkStatus newStatus) {

            }

            @Override
            public void start() {

            }

            @Override
            public void stop() {

            }
        };

        minecraftServer = new DummyMinecraftServer(
                new Thread(() -> {}), // Dummy thread is required for the spark mod
                levelStorageAccess,
                packRepository,
                worldStem,
                proxy,
                dataFixer,
                new Services(null, null, null, null),
                i -> chunkProgressListener
        );

        // All this stuff, just so we can give Forge a fake minecraft server...
        WorldPreview.get().loaderSpecificSetup(minecraftServer);

        // Use this (or add an option) to do things "properly"
        // ((DummyMinecraftServer) minecraftServer).createLevels();

        // Now "create" a world, to trigger mixins hooking the ServerLevel constructor
        new ServerLevel(
                minecraftServer,
                Executors.newSingleThreadExecutor(),
                levelStorageAccess,
                new DerivedLevelData(
                        worldStem.worldData(),
                        worldStem.worldData().overworldData()
                ),
                dimension,
                levelStem,
                new ChunkProgressListener() {
                    @Override
                    public void updateSpawnPos(ChunkPos center) {}

                    @Override
                    public void onStatusChange(ChunkPos chunkPosition, @Nullable ChunkStatus newStatus) {}

                    @Override
                    public void start() {}

                    @Override
                    public void stop() {}
                },
                false, // debug
                BiomeManager.obfuscateSeed(worldOptions.seed()),
                List.of(),
                false, // tickTime
                null
        );

        // Noise / Heightmap stuff -- and random state
        if (chunkGenerator instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator) {
            noiseGeneratorSettings = noiseBasedChunkGenerator.generatorSettings().value();
            randomState = RandomState.create(
                    noiseBasedChunkGenerator.generatorSettings().value(),
                    registryAccess.lookupOrThrow(Registries.NOISE),
                    worldOptions.seed()
            );
        } else {
            noiseGeneratorSettings = null;
            randomState = RandomState.create(
                    NoiseGeneratorSettings.dummy(),
                    registryAccess.lookupOrThrow(Registries.NOISE),
                    worldOptions.seed()
            );
        }

        // This needs to happen *after* creating the dummy Minecraft server
        this.structureCheck = new StructureCheck(
                null, // Should never be required because `tryLoadFromStorage` must not be called
                this.registryAccess,
                this.structureTemplateManager,
                dimension,
                this.chunkGenerator,
                this.randomState,
                this.levelHeightAccessor,
                chunkGenerator.getBiomeSource(),
                worldOptions.seed(),
                dataFixer
        );
        this.structureManager = new StructureManager(this.previewLevel, worldOptions, this.structureCheck);

        this.chunkGeneratorStructureState = this.chunkGenerator.createState(
                this.registryAccess.lookupOrThrow(Registries.STRUCTURE_SET),
                this.randomState,
                worldOptions.seed()
        );

        // Initialize early
        chunkGeneratorStructureState.ensureStructuresGenerated();

        // Create fake , to trigger mixins for some mods...
        serverLevel = new ServerLevel(
                minecraftServer,
                Executors.newSingleThreadExecutor(),
                levelStorageAccess,
                new DummyServerLevelData(),
                dimension,
                levelStem,
                chunkProgressListener,
                false, // is Debug
                BiomeManager.obfuscateSeed(worldOptions.seed()),
                List.of(),
                false,
                null
            );
    }

    public @Nullable ServerPlayer getPlayers(UUID playerId) {
        if (minecraftServer instanceof DummyMinecraftServer) {
            return null;
        }
        return minecraftServer.getPlayerList().getPlayer(playerId);
    }

    public record BiomeResult(ResourceKey<Biome> biome, short[] noiseResult) {}

    private static short doubleToShort(double val, double factor) {
        return (short) Math.min(Short.MAX_VALUE, Math.max(Short.MIN_VALUE, (long) (val * factor * (double)Short.MAX_VALUE)));
    }

    public boolean hasRawNoiseInfo() {
        return cfg.storeNoiseSamples && biomeSource instanceof MultiNoiseBiomeSource;
    }

    public BiomeResult doSample(BlockPos pos) {
        final Climate.Sampler sampler = randomState.sampler();
        if (hasRawNoiseInfo()) {
            final var singlePointContext = new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ());
            final double temperature = sampler.temperature().compute(singlePointContext);
            final double humidity = sampler.humidity().compute(singlePointContext);
            final double continentalness = sampler.continentalness().compute(singlePointContext);
            final double erosion = sampler.erosion().compute(singlePointContext);
            final double depth = sampler.depth().compute(singlePointContext);
            final double weirdness = sampler.weirdness().compute(singlePointContext);

            final short[] noiseData = new short[] {
                    doubleToShort(temperature, 1),
                    doubleToShort(humidity, 1),
                    doubleToShort(continentalness, 0.5),
                    doubleToShort(erosion, 1),
                    doubleToShort(depth, 0.5),
                    doubleToShort(weirdness, 0.75),
            };

            final var targetPoint = Climate.target((float) temperature, (float) humidity, (float) continentalness, (float) erosion, (float) depth, (float) weirdness);
            final MultiNoiseBiomeSource noiseBiomeSource = (MultiNoiseBiomeSource) biomeSource;
            final Holder<Biome> biome = noiseBiomeSource.getNoiseBiome(targetPoint);
            return new BiomeResult(biome.unwrapKey().orElseThrow(), noiseData);
        } else {
            return new BiomeResult(
                    biomeSource.getNoiseBiome(
                            QuartPos.fromBlock(pos.getX()),
                            QuartPos.fromBlock(pos.getY()),
                            QuartPos.fromBlock(pos.getZ()),
                            randomState.sampler()
                    ).unwrapKey().orElseThrow(),
                    null
            );
        }
    }

    /*
    public ResourceKey<Biome> doSample(BlockPos pos) {
        return biomeSource.getNoiseBiome(
                QuartPos.fromBlock(pos.getX()),
                QuartPos.fromBlock(pos.getY()),
                QuartPos.fromBlock(pos.getZ()),
                randomState.sampler()
        ).unwrapKey().orElseThrow();
    }
     */

    public List<Pair<ResourceLocation, StructureStart>> doStructures(ChunkPos chunkPos) {
        ProtoChunk protoChunk = (ProtoChunk) previewLevel.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);
        chunkGenerator.createStructures(registryAccess, chunkGeneratorStructureState, structureManager, protoChunk, structureTemplateManager, dimension);
        Map<Structure, StructureStart> raw = protoChunk.getAllStarts();
        List<Pair<ResourceLocation, StructureStart>> res = new ArrayList<>(raw.size());
        for (Map.Entry<Structure, StructureStart> x : protoChunk.getAllStarts().entrySet()) {
            res.add(new Pair<>(structureRegistry.getKey(x.getKey()), x.getValue()));
        }
        return res;
    }

    public NoiseChunk getNoiseChunk(ChunkPos startChunk, int numChunks, boolean keepAquifer) {
        NoiseSettings noiseSettings = noiseGeneratorSettings.noiseSettings();
        NoiseChunk noiseChunk = new NoiseChunk(
                (numChunks * 16) / noiseSettings.getCellWidth(),
                randomState,
                startChunk.getMinBlockX(),
                startChunk.getMinBlockZ(),
                noiseSettings,
                DensityFunctions.BeardifierMarker.INSTANCE,
                noiseGeneratorSettings,
                ((NoiseBasedChunkGeneratorAccessor) chunkGenerator).getGlobalFluidPicker().get(),
                Blender.empty()
        );
        if (!keepAquifer) {
            ((NoiseChunkAccessor) noiseChunk).setAquifer(new EmptyAquifer());
        }
        return noiseChunk;
    }

    public NoiseGeneratorSettings noiseGeneratorSettings() {
        return noiseGeneratorSettings;
    }

    public short doHeightSlow(BlockPos pos) {
        return (short) chunkGenerator.getBaseHeight(
                pos.getX(),
                pos.getZ(),
                Heightmap.Types.OCEAN_FLOOR_WG,
                levelHeightAccessor,
                randomState
        );
    }

    public NoiseColumn doIntersectionsSlow(BlockPos pos) {
        return chunkGenerator.getBaseColumn(pos.getX(), pos.getZ(), levelHeightAccessor, randomState);
    }

    @Override
    public void close() throws Exception {
        // FileUtils.deleteDirectory(tempDir.toFile());
        if (serverLevel != null) {
            serverLevel.close();
        }
        if (tempDir != null) {
            deleteDirectoryLegacyIO(tempDir.toFile());
        }
    }

    // Source https://mkyong.com/java/how-to-delete-directory-in-java/
    public static void deleteDirectoryLegacyIO(File file) {
        File[] list = file.listFiles();
        if (list != null) {
            for (File temp : list) {
                //recursive delete
                deleteDirectoryLegacyIO(temp);
            }
        }

        if (!file.delete()) {
            LOGGER.warn("Unable to delete file or directory : {}", file);
        }
    }

    public CloseableResourceManager resourceManager() {
        return resourceManager;
    }

    public RegistryAccess registryAccess() {
        return registryAccess;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }
}
