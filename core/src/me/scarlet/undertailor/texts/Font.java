package me.scarlet.undertailor.texts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import me.scarlet.undertailor.exception.ConfigurationException;
import me.scarlet.undertailor.texts.Font.FontData.CharMeta;
import me.scarlet.undertailor.texts.TextComponent.DisplayMeta;
import me.scarlet.undertailor.texts.TextComponent.Text;
import me.scarlet.undertailor.util.ConfigurateUtil;
import ninja.leaping.configurate.ConfigurationNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Font {
    
    // private static final String CHARLIST = "";
    
    public static class FontData {
        
        public static class CharMeta {
            
            public int offX, offY, boundWrapX, boundWrapY;
            
            public CharMeta() {}
            
            public static CharMeta fromConfig(ConfigurationNode root) {
                try {
                    CharMeta meta = new CharMeta(ConfigurateUtil.processIntArray(root, new Integer[] {0, 0, 0, 0}));
                    return meta;
                } catch(ConfigurationException e) {
                    throw e;
                }
            }
            
            public CharMeta(int[] values) {
                this.setValues(values);
            }
            
            public CharMeta(int offX, int offY) {
                this(offX, offY, 0, 0);
            }
            
            public CharMeta(int offX, int offY, int boundWrapX, int boundWrapY) {
                this.offX = offX;
                this.offY = offY;
                this.boundWrapX = boundWrapX;
                this.boundWrapY = boundWrapY;
            }
            
            public int[] values() {
                return new int[] {offX, offY, boundWrapX, boundWrapY};
            }
            
            public void setValues(int[] values) {
                this.offX = values[0];
                this.offY = values[1];
                this.boundWrapX = values[2];
                this.boundWrapY = values[3];
            }
            
            public CharMeta merge(CharMeta otherMeta) {
                int[] set = new int[4];
                int[] values = otherMeta.values();
                for(int i = 0; i < 4; i++) {
                    if(values[i] != 0) {
                        set[i] = values[i];
                    }
                }
                
                return new CharMeta(set);
            }
            
            @Override
            public String toString() {
                return "[" + offX + ", " + offY + ", " + boundWrapX + ", " + boundWrapY + "]";
            }
        }
        
        public static FontData fromConfig(String name, ConfigurationNode node) {
            FontData data = new FontData();
            ConfigurationNode root = node.getNode("font");
            try {
                data.fontName = name;
                data.x = ConfigurateUtil.processInt(root.getNode("gridSizeX"), null);
                data.y = ConfigurateUtil.processInt(root.getNode("gridSizeY"), null);
                data.space = ConfigurateUtil.processInt(root.getNode("spaceSize"), null);
                data.spacing = ConfigurateUtil.processInt(root.getNode("letterSpacing"), null);
                data.characterList = ConfigurateUtil.processString(root.getNode("charList"), null);
                
                data.globalMeta = CharMeta.fromConfig(root.getNode("globalMeta"));
                data.charMeta = new HashMap<>();
                for(Entry<Object, ? extends ConfigurationNode> entry: root.getNode("meta").getChildrenMap().entrySet()) {
                    data.charMeta.put(entry.getKey().toString(), CharMeta.fromConfig(entry.getValue()));
                }
                
                return data;
            } catch(RuntimeException e) {
                e.printStackTrace();
                return null;
            }
        }
        
        private String fontName;
        private CharMeta globalMeta;
        private String characterList;
        private Map<String, CharMeta> charMeta;
        private int x = 1, y = 1; // grid size, letter size
        private int spacing = -1; // pixels between letters
        private int space = -1; // pixels that count as a space
        
        private FontData() {}
        
        public String getName() {
            return fontName;
        }
        
        public CharMeta getFontMeta() {
            return globalMeta;
        }
        
        public CharMeta getCharacterMeta(char ch) {
            Set<CharMeta> collectedMeta = new HashSet<CharMeta>();
            for(Entry<String, CharMeta> entry : charMeta.entrySet()) {
                if(entry.getKey().indexOf(ch) != -1) {
                   collectedMeta.add(entry.getValue());
                }
            }
            
            if(!collectedMeta.isEmpty()) {
                CharMeta compiled = new CharMeta();
                for(CharMeta meta : collectedMeta) {
                    compiled = compiled.merge(meta);
                }
                
                return compiled;
            }
            
            return getFontMeta();
        }
        
        public int getLetterSpacing() {
            return spacing;
        }
        
        public int getSpaceSize() {
            return space;
        }
    }
    
    private FontData data;
    private Map<Character, TextureRegion> font;
    public Font(Texture spriteSheet, FontData data) {
        this.font = new HashMap<>();
        this.data = data;
        
        int current = 0;
        int charHeight = (int) (spriteSheet.getHeight() / data.y);
        int charWidth = (int) (spriteSheet.getWidth() / data.x);
        for(int iy = 0; iy < data.y; iy++) {
            for(int ix = 0; ix < data.x; ix++) {
                char currentChar = data.characterList.charAt(current);
                CharMeta meta = data.getCharacterMeta(currentChar);
                TextureRegion region = new TextureRegion(spriteSheet);
                int height = charHeight;
                int width = charWidth;
                int wrapY = 0;
                if(meta != null) {
                    width = width - meta.boundWrapX;
                    height = height - meta.boundWrapY;
                    wrapY = meta.boundWrapY;
                }
                
                region.setRegion(ix * charWidth, (iy * charHeight) + wrapY, width, height);
                font.put(currentChar, region);
                current++;
                
                if(current == data.characterList.length()) {
                    break;
                }
            }
        }
    }
    
    public FontData getFontData() {
        return data;
    }
    
    public TextureRegion getChar(char ch) {
        return font.get(ch);
    }
    
    public void write(Batch batch, Text text, int posX, int posY) {
        write(batch, text.getText(), text.getStyle(), text.getColor(), posX, posY);
    }
    
    public void write(Batch batch, String text, Style style, Color color, int posX, int posY) {
        write(batch, text, style, color, posX, posY, 1);
    }
    
    public void write(Batch batch, String text, Style style, Color color, int posX, int posY, int scale) {
        write(batch, text, style, color, posX, posY, scale, 1.0F);
    }
    
    public void write(Batch batch, String text, Style style, Color color, int posX, int posY, int scale, float alpha) {
        if(text.trim().isEmpty()) {
            return;
        }
        
        char[] chars = new char[text.length()];
        text.getChars(0, text.length(), chars, 0);
        int pos = 0;
        
        //for(char chara : chars) {
        if(style != null) {
            style.onNextTextRender(Gdx.graphics.getDeltaTime());
        }
        
        for(int i = 0; i < chars.length; i++) {
            char chara = chars[i];
            if(Character.valueOf(' ').compareTo(chara) == 0) {
                pos += (this.getFontData().getSpaceSize() * scale);
                continue;
            }
            
            CharMeta meta = this.getFontData().getCharacterMeta(chara);
            TextureRegion region = new TextureRegion(this.getChar(chara));
            Color used = color == null ? Color.WHITE : new Color(color);
            //System.out.println(color == null ? "null" : color.toString());
            used.a = alpha;
            batch.setColor(used);
            float oX = region.getRegionWidth() / 2.0F;
            float oY = region.getRegionHeight() / 2.0F;
            float aX = 0F, aY = 0F, aScaleX = 1.0F, aScaleY = 1.0F;
            if(style != null) {
                DisplayMeta dmeta = style.applyCharacter(i, text.replaceAll(" ", "").length());
                if(dmeta != null) {
                    aX = dmeta.offX;
                    aY = dmeta.offY;
                    aScaleX = dmeta.scaleX;
                    aScaleY = dmeta.scaleY;
                }
            }
            
            float pX = posX + pos + ((meta.offX) * scale) + (scale * aX);
            float pY = posY + ((meta.offY) * scale) + (scale * aY);
            
            batch.draw(region, pX + oX, pY + oY, oX, oY, region.getRegionWidth(), region.getRegionHeight(), scale * aScaleX, scale * aScaleY, 0);
            pos += ((region.getRegionWidth() + this.getFontData().getLetterSpacing()) * scale);
        }
    }
    
    public void sheetTest(Batch batch) {
        int i = 0;
        int i2 = 0;
        for(TextureRegion region : font.values()) {
            int y = 85 + (i2 * 35);
            batch.draw(region, (20 * i) + 15, y, 0, 0, region.getRegionWidth(), region.getRegionHeight(), 2.0F, 2.0F, 0F);
            i++;
            if(i == 15) {
                i = 0;
                i2++;
            }
        }
    }
}
