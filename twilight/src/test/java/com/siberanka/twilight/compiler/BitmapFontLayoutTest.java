package com.siberanka.twilight.compiler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.siberanka.twilight.config.TwilightConfig;
import com.siberanka.twilight.source.ContentSource;
import com.siberanka.twilight.source.ResourceIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BitmapFontLayoutTest {
    @TempDir Path root;
    private final JsonArray providers = new JsonArray();

    @Test
    void matchesJavaHeightAndAscentIncludingRepresentableNegativeAscent() throws Exception {
        // Client calibration: a native cell starts 4 GUI units above H;
        // Java bitmap top is 7-ascent. These are measured reference bounds.
        int[][] cases = {{4,4,7,11},{4,2,9,13},{4,0,11,15},{2,-2,13,15},{8,8,3,11},{9,8,3,12}};
        for (int i=0;i<cases.length;i++) add(0xE000+i, 16, cases[i][0], cases[i][1], 0,16);
        var output = compile();
        assertTrue(output.result.problems().isEmpty(), output.result.problems().toString());
        BufferedImage page = output.page("E0");
        for (int i=0;i<cases.length;i++) {
            for (int y=0;y<16;y++) assertEquals(y>=cases[i][2] && y<cases[i][3],
                    page.getRGB(i*16,y)>>>24!=0, "case="+i+", y="+y);
        }
    }

    @Test
    void sourceResolutionAndOtherPageMembersCannotMoveOrResizeGlyphs() throws Exception {
        add(0xE000,8,8,7,0,8);
        add(0xE100,64,8,7,0,64);
        int[] before = compile().page("E0").getRGB(0,0,16,16,null,0,16);
        add(0xE001,16,12,10,0,16);
        var output = compile();
        assertArrayEquals(before,output.page("E0").getRGB(0,0,16,16,null,0,16));
        assertArrayEquals(before,output.page("E1").getRGB(0,0,16,16,null,0,16));
    }

    @Test
    void refusesVisibleOverflowWithoutShrinkingOrDrawingInAdjacentCells() throws Exception {
        add(0xE000,16,16,16,0,16); // top=-5, visible data above native cell
        add(0xE001,16,9,0,0,16);   // bottom=20
        add(0xE002,16,4,4,0,16);
        var output=compile();
        assertEquals(1,output.result.glyphs());
        assertTrue(output.result.problems().stream().anyMatch(p->p.contains("baselines require a Bedrock layout adapter")));
        var page=output.page("E0");
        for(int y=0;y<16;y++) for(int x=0;x<32;x++) assertEquals(0,page.getRGB(x,y));
        assertNotEquals(0,page.getRGB(32,7));
    }

    @Test
    void transparentPaddingMayLeaveCellWithoutErasingNeighborGlyphs() throws Exception {
        add(0xE000,16,16,11,0,16);
        add(0xE010,16,16,16,8,16); // source padding above cell; visible y=3..10
        var output=compile();
        assertTrue(output.result.problems().isEmpty());
        assertEquals(2,output.result.glyphs());
        var page=output.page("E0");
        for(int y=0;y<16;y++) assertNotEquals(0,page.getRGB(0,y));
        assertEquals(0,page.getRGB(0,18));
        assertNotEquals(0,page.getRGB(0,19));
        assertNotEquals(0,page.getRGB(0,26));
        assertEquals(0,page.getRGB(0,27));
    }

    private void add(int codePoint,int sourceSize,int height,int ascent,int visibleStart,int visibleEnd) throws Exception {
        String name="g"+Integer.toHexString(codePoint);
        BufferedImage image=new BufferedImage(sourceSize,sourceSize,BufferedImage.TYPE_INT_ARGB);
        for(int y=visibleStart;y<visibleEnd;y++) for(int x=0;x<sourceSize;x++) image.setRGB(x,y,0xFFFF0000);
        Path path=root.resolve("assets/test/textures/"+name+".png");Files.createDirectories(path.getParent());
        ImageIO.write(image,"PNG",path.toFile());
        JsonObject provider=new JsonObject();provider.addProperty("type","bitmap");provider.addProperty("file","test:"+name+".png");
        provider.addProperty("height",height);provider.addProperty("ascent",ascent);
        JsonArray chars=new JsonArray();chars.add(Character.toString(codePoint));provider.add("chars",chars);providers.add(provider);
    }

    private Output compile() throws Exception {
        Path path=root.resolve("assets/minecraft/font/default.json");Files.createDirectories(path.getParent());
        JsonObject font=new JsonObject();font.add("providers",providers);Files.writeString(path,font.toString());
        var config=new TwilightConfig(false,true,false,false,40,100,10_000_000,1000,false,false,List.of(),"auto",false,false,3);
        try(var index=ResourceIndex.build(List.of(new ContentSource("test",ContentSource.Kind.RESOURCE_PACK,root,1)),config)) {
            Map<String,byte[]> files=new LinkedHashMap<>();
            return new Output(new BitmapFontCompiler(index,null).compile(files),files);
        }
    }
    private record Output(BitmapFontCompiler.Result result,Map<String,byte[]> files) {
        BufferedImage page(String hex) throws Exception {return ImageIO.read(new ByteArrayInputStream(files.get("font/glyph_"+hex+".png")));}
    }
}
