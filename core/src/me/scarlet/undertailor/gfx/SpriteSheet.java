package me.scarlet.undertailor.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import me.scarlet.undertailor.Undertailor;
import me.scarlet.undertailor.exception.ConfigurationException;
import me.scarlet.undertailor.exception.TextureTilingException;
import me.scarlet.undertailor.gfx.Sprite.SpriteMeta;
import me.scarlet.undertailor.util.ConfigurateUtil;
import ninja.leaping.configurate.ConfigurationNode;

import java.io.File;
import java.io.FileNotFoundException;

public class SpriteSheet {
    
    public static class SpriteSheetMeta {
        
        public int gridX, gridY;
        public SpriteMeta[] spriteMeta; // metadata per frame
        
        public SpriteSheetMeta() {
            this.gridX = 1;
            this.gridY = 1;
            this.spriteMeta = null;
        }
    }
    
    public static SpriteSheet fromConfig(File folder, ConfigurationNode node) throws FileNotFoundException, TextureTilingException {
        String name = node.getKey().toString();
        SpriteSheetMeta meta = new SpriteSheetMeta();
        FileHandle textureFile = Gdx.files.absolute(new File(folder, ConfigurateUtil.processString(node.getNode("file"), null)).getAbsolutePath());
        if(!textureFile.exists()) {
            throw new FileNotFoundException("Texture file didn't exist");
        }
        
        Texture texture = new Texture(textureFile);
        try {
            meta.gridX = ConfigurateUtil.processInt(node.getNode("gridSizeX"), null);
            meta.gridY = ConfigurateUtil.processInt(node.getNode("gridSizeY"), null);
            ConfigurationNode metaRoot = node.getNode("spritemeta");
            if(!metaRoot.isVirtual()) {
                int spriteCount = meta.gridX * meta.gridY;
                meta.spriteMeta = new SpriteMeta[spriteCount];
                for(int i = 0; i < spriteCount; i++) {
                    for(ConfigurationNode metaNode : metaRoot.getChildrenMap().values()) {
                        if(metaNode.getKey().toString().contains("" + i)) {
                            if(meta.spriteMeta[i] == null) {
                                meta.spriteMeta[i] = new SpriteMeta();
                            }
                            
                            SpriteMeta cmeta = meta.spriteMeta[i];
                            try {
                                cmeta.offX = ConfigurateUtil.processInt(metaNode.getNode("offX"), cmeta.offX);
                                cmeta.offY = ConfigurateUtil.processInt(metaNode.getNode("offY"), cmeta.offY);
                                cmeta.wrapX = ConfigurateUtil.processInt(metaNode.getNode("wrapX"), cmeta.wrapX);
                                cmeta.wrapY = ConfigurateUtil.processInt(metaNode.getNode("wrapY"), cmeta.wrapY);
                                cmeta.originX = ConfigurateUtil.processFloat(metaNode.getNode("originX"), cmeta.originX);
                                cmeta.originY = ConfigurateUtil.processFloat(metaNode.getNode("originY"), cmeta.originY);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                            
                            Undertailor.debug("sheetman", "sheet " + name + " loaded sprite at index " + i + " with meta "
                                    + meta.spriteMeta[i].toString());
                        }
                    }
                }
            }
        } catch(ConfigurationException e) {
            e.printStackTrace();
            throw e;
        }
        
        return new SpriteSheet(name, texture, meta);
    }
    
    private String sheetName;
    private Sprite[] sprites;
    public SpriteSheet(String sheetName, Texture image, SpriteSheetMeta meta) throws TextureTilingException {
        this.sheetName = sheetName;
        checkTexture(image, meta.gridX, meta.gridY);
        sprites = new Sprite[meta.gridX * meta.gridY];
        int spriteHeight = (int) (image.getHeight() / meta.gridY);
        int spriteWidth = (int) (image.getWidth() / meta.gridX);
        for(int iY = 0; iY < meta.gridY; iY++) {
            for(int iX = 0; iX < meta.gridX; iX++) {
                int pos = (iY * meta.gridX) + iX;
                SpriteMeta smeta = null;
                if(meta.spriteMeta != null && pos < meta.spriteMeta.length && meta.spriteMeta[pos] != null) {
                    smeta = meta.spriteMeta[pos];
                }
                
                TextureRegion region = new TextureRegion(image);
                int height = spriteHeight;
                int width = spriteWidth;
                int wrapY = 0;
                if(smeta != null) {
                    width -= smeta.wrapX;
                    height -= smeta.wrapY;
                    wrapY = smeta.wrapY;
                }
                
                region.setRegion(iX * spriteWidth, (iY * spriteHeight) + wrapY, width, height);
                sprites[pos] = new Sprite(region, smeta);
            }
        }
    }
    
    public Sprite[] getSprites() {
        return sprites;
    }
    
    public Sprite getSprite(int index) {
        return sprites[index];
    }
    
    public String getSheetName() {
        return sheetName;
    }
    
    private void checkTexture(Texture image, int width, int height) throws TextureTilingException {
        if(image.getWidth() % width != 0) {
            throw new TextureTilingException("Texture width is not divisible by defined width");
        }
        
        if(image.getHeight() % height != 0) {
            throw new TextureTilingException("Texture width is not divisible by defined height");
        }
    }
    
    public void sheetTest(Batch batch) {
        int i = 0;
        int i2 = 0;
        for(Sprite sprite : sprites) {
            int y = 400 - (i2 * 35);
            //TextureRegion region = sprite.getTextureRegion();
            sprite.draw(batch, 20 * i + 16, y);
            //batch.draw(region, (20 * i) + 15, y, 0, 0, region.getRegionWidth(), region.getRegionHeight(), 2.0F, 2.0F, 0F);
            i++;
            if(i == 16) {
                i = 0;
                i2++;
            }
        }
    }
}