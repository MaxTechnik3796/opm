package cz.maxtechnik.opm.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import cz.maxtechnik.opm.client.handler.DebugScreenState;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenMixin {

    // ==========================================
    // LEVÝ SLOUPEC
    // ==========================================
    @ModifyReturnValue(method = "getGameInformation", at = @At("RETURN"))
    private List<String> modifyGameInfo(List<String> original) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return original;

        List<String> list = new ArrayList<>();
        Entity entity = mc.getCameraEntity();
        assert entity != null;
        BlockPos blockpos = entity.blockPosition();
        ChunkPos chunkpos = new ChunkPos(blockpos);
        Level level = mc.level;
        LongSet forcedChunks = LongSets.EMPTY_SET;

        // Řádek 1 - verze Minecraftu, verze launcheru, název mod loaderu
        list.add("Minecraft " + SharedConstants.getCurrentVersion().getName()
                + " (" + mc.getLaunchedVersion()
                + "/" + ClientBrandRetriever.getClientModName() + ")");

        // Řádek 2 - FPS
        list.add("FPS: " + mc.getFps());

        // Řádek 3 - PRÁZDNÝ
        list.add("");

        // Řádek 4 - přesná XYZ pozice hráče
        list.add(String.format(Locale.ROOT, "XYZ: %.3f / %.5f / %.3f",
                entity.getX(), entity.getY(), entity.getZ()));

        // Řádek 5 - pozice v blocích + relativně v chunku
        list.add(String.format(Locale.ROOT, "Block: %d %d %d [%d %d %d]",
                blockpos.getX(), blockpos.getY(), blockpos.getZ(),
                blockpos.getX() & 15, blockpos.getY() & 15, blockpos.getZ() & 15));

        // Řádek 6 - chunk koordináty + region soubor
        list.add(String.format(Locale.ROOT, "Chunk: %d %d %d [%d %d in r.%d.%d.mca]",
                chunkpos.x, SectionPos.blockToSectionCoord(blockpos.getY()), chunkpos.z,
                chunkpos.getRegionLocalX(), chunkpos.getRegionLocalZ(),
                chunkpos.getRegionX(), chunkpos.getRegionZ()));

        // Řádek 7 - směr pohledu
        String facingDescription = switch (entity.getDirection()) {
            case NORTH -> "negative Z";
            case SOUTH -> "positive Z";
            case WEST  -> "negative X";
            case EAST  -> "positive X";
            default    -> "Invalid";
        };
        list.add(String.format(Locale.ROOT, "Facing: %s (%s) (%.1f / %.1f)",
                entity.getDirection(), facingDescription,
                Mth.wrapDegrees(entity.getYRot()),
                Mth.wrapDegrees(entity.getXRot())));

        // Řádek 8 - světlost
        int totalLight = mc.level.getLightEngine().getRawBrightness(blockpos, 0);
        int skyLight   = mc.level.getBrightness(LightLayer.SKY, blockpos);
        int blockLight = mc.level.getBrightness(LightLayer.BLOCK, blockpos);
        list.add("Client Light: " + totalLight + " (" + skyLight + " sky, " + blockLight + " block)");

        // Řádek 9 - biom
        var biomeHolder = mc.level.getBiome(blockpos);
        String biomeName = biomeHolder.unwrap()
                .map(key -> key.location().toString(), b -> "unregistered");
        list.add("Biome: " + biomeName);

        // Řádek 10 - dimenze
        list.add("Dim: " + level.dimension().location());

        // Řádek 11 - herní den + force loaded chunky
        list.add("Day " + mc.level.getDayTime() / 24000L + " | FC: " + forcedChunks.size());

        // Řádek 12 - shader
        PostChain postchain = mc.gameRenderer.currentEffect();
        list.add("Shader: " + (postchain != null ? postchain.getName() : "none"));

        return list;
    }

    // ==========================================
    // PRAVÝ SLOUPEC
    // ==========================================
    @ModifyReturnValue(method = "getSystemInformation", at = @At("RETURN"))
    private List<String> modifySystemInfo(List<String> original) {
        Minecraft mc = Minecraft.getInstance();
        List<String> list = new ArrayList<>();

        // Řádek 1 - Java verze
        list.add("Java: " + System.getProperty("java.version"));

        // Řádek 2 - RAM
        long maxMem   = Runtime.getRuntime().maxMemory();
        long totalMem = Runtime.getRuntime().totalMemory();
        long freeMem  = Runtime.getRuntime().freeMemory();
        long usedMem  = totalMem - freeMem;
        list.add(String.format(Locale.ROOT, "Mem: %2d%% %03d/%03dMB",
                usedMem * 100L / maxMem,
                usedMem / 1024L / 1024L,
                maxMem / 1024L / 1024L));

        // Řádek 3 - PRÁZDNÝ
        list.add("");

        // Řádek 4 - CPU
        list.add("CPU: " + com.mojang.blaze3d.platform.GlUtil.getCpuInfo());

        // Řádek 5 - GPU
        list.add("GPU: " + com.mojang.blaze3d.platform.GlUtil.getRenderer());

        // Řádek 6 - PRÁZDNÝ
        list.add("");

        // Řádek 7 - rozlišení okna
        list.add(String.format(Locale.ROOT, "Display: %dx%d",
                mc.getWindow().getWidth(),
                mc.getWindow().getHeight()));

        // Řádek 8 - OpenGL verze
        list.add("OpenGL: " + com.mojang.blaze3d.platform.GlUtil.getOpenGLVersion());

        // Řádek 9 - PRÁZDNÝ
        list.add("");

        // ==========================================
        // TARGETED BLOCK / FLUID / ENTITY
        // Vanilla chování - bereme přímo z originalu beze změn
        // F3+4 přepíná plné tagy vs jen počet
        // ==========================================
        boolean foundTargeted = false;
        List<String> currentTagLines = new ArrayList<>();

        for (String line : original) {
            if (!foundTargeted) {
                if (line.contains("Targeted Block") || line.contains("Targeted Fluid") ||
                        line.contains("Targeted Entity")) {
                    foundTargeted = true;
                    list.add(line);
                }
                continue;
            }

            if (line.startsWith("#")) {
                if (DebugScreenState.showFullTags) {
                    // F3+4 zapnuté - zobraz všechny tagy jako vanilla
                    list.add(line);
                } else {
                    // F3+4 vypnuté - sbíráme pro počet
                    currentTagLines.add(line);
                }
            } else if (line.contains("Targeted Block") || line.contains("Targeted Fluid") ||
                    line.contains("Targeted Entity")) {
                // Nová targeted sekce
                if (!DebugScreenState.showFullTags && !currentTagLines.isEmpty()) {
                    list.add("Tags: " + currentTagLines.size());
                    currentTagLines.clear();
                }
                list.add("");
                list.add(line);
            } else {
                // Normální řádek - block ID, block state atd.
                if (!DebugScreenState.showFullTags && !currentTagLines.isEmpty()) {
                    list.add("Tags: " + currentTagLines.size());
                    currentTagLines.clear();
                }
                list.add(line);
            }
        }

        // Zbývající tagy na konci
        if (!DebugScreenState.showFullTags && !currentTagLines.isEmpty()) {
            list.add("Tags: " + currentTagLines.size());
        }

        return list;
    }
}