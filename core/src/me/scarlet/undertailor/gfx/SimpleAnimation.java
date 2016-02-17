/* 
 * The MIT License (MIT)
 * 
 * Copyright (c) 2016 Tellerva, Marc Lawrence
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.scarlet.undertailor.gfx;

import me.scarlet.undertailor.Undertailor;
import me.scarlet.undertailor.exception.AnimationLoadException;
import me.scarlet.undertailor.exception.ConfigurationException;
import me.scarlet.undertailor.gfx.KeyFrame.FrameObjectMeta;
import me.scarlet.undertailor.gfx.KeyFrame.SimpleKeyFrame;
import me.scarlet.undertailor.manager.AnimationManager;
import me.scarlet.undertailor.util.ConfigurateUtil;
import me.scarlet.undertailor.util.MapUtil;
import ninja.leaping.configurate.ConfigurationNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SimpleAnimation extends Animation<SimpleKeyFrame>{
    
    public static final int TYPE_ID = 0;
    
    public static SimpleAnimation fromConfig(ConfigurationNode node) {
        String name = node.getKey().toString();
        Undertailor.instance.debug(AnimationManager.MANAGER_TAG, "loading simpleanimation " + name);
        
        String[] frames = ConfigurateUtil.processStringArray(node.getNode("frames"), null);
        boolean looping = ConfigurateUtil.processBoolean(node.getNode("looping"), false);
        
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("flipX", ConfigurateUtil.processBoolean(node.getNode("flipX"), false));
        defaultData.put("flipY", ConfigurateUtil.processBoolean(node.getNode("flipY"), false));
        defaultData.put("frameTime", ConfigurateUtil.processFloat(node.getNode("frameTime"), 0.5F));
        SimpleKeyFrame[] keyFrames = new SimpleKeyFrame[frames.length];
        
        try {
            for(int i = 0; i < frames.length; i++) {
                String frame = frames[i];
                String frameIndexStr = frame.split(";")[0];
                Map<String, Object> data;
                if(frame.length() > frameIndexStr.length()) {
                    data = Animation.parseParameters(defaultData, frame.substring(frameIndexStr.length() + 1)); // number and ;
                } else {
                    data = defaultData;
                }
                
                int frameIndex = Integer.parseInt(frameIndexStr);
                FrameObjectMeta meta = FrameObjectMeta.fromMapping(data);
                float frameTime = (float) data.get("frameTime");
                keyFrames[i] = new SimpleKeyFrame(frameIndex, (long) (1000.0 * frameTime), meta);
            }
        } catch(NumberFormatException e) {
            throw new ConfigurationException("bad frame object data: invalid frame index");
        }
        
        return new SimpleAnimation(name, looping, keyFrames);
    }
    
    private Map<Long, SimpleKeyFrame> frames;
    public SimpleAnimation(String name, boolean loop, SimpleKeyFrame... frames) {
        super(name, loop);
        this.frames = new LinkedHashMap<>();
        long lastTime = 0;
        for(SimpleKeyFrame frame : frames) {
            if(lastTime <= 0) {
                lastTime = frame.getFrameTime();
            } else {
                lastTime += frame.getFrameTime();
            }
            
            this.frames.put(lastTime, frame);
        }
    }

    @Override
    public Map<Long, SimpleKeyFrame> getFrames() {
        return new LinkedHashMap<>(frames);
    }
    
    @Override
    public void finalChecks() {
        for(SimpleKeyFrame frame : frames.values()) {
            for(Sprite[] set : this.getParentSet().getSpritesets()) {
                if(set.length < frame.getSpriteIndex() + 1) {
                    throw new AnimationLoadException("animation " + this.getName() + " in animation set " + this.getParentSet().getName() + " referenced a non-existing sprite index");
                }
            }
        }
    }

    @Override
    public SimpleKeyFrame getFrame(long stateTime, boolean looping) {
        long time = stateTime;
        Entry<Long, SimpleKeyFrame> last = MapUtil.getLastEntry(frames);
        if(time > last.getKey()) {
            if(looping) {
                time = (long) (time - (last.getKey() * (Math.floor(time / last.getKey()))));
            } else {
                return last.getValue();
            }
        }
        
        Entry<Long, SimpleKeyFrame> current = null;
        Entry<Long, SimpleKeyFrame> previous = null;
        Iterator<Entry<Long, SimpleKeyFrame>> iterator = this.frames.entrySet().iterator();
        while(iterator.hasNext()) {
            previous = current;
            current = iterator.next();
            if(time <= current.getKey()) {
                if(previous == null) {
                    return current.getValue();
                } else {
                    if(time > previous.getKey()) {
                        return current.getValue();
                    }
                }
            }
        }
        
        return current.getValue(); // returns the last frame
    }
    
    @Override
    public void drawFrame(SimpleKeyFrame frame, String spriteset, float posX, float posY, float scale, float rotation) {
        if(frame.getSpriteIndex() <= -1) {
            return;
        }
        
        Sprite sprite = this.getParentSet().getSpriteset(spriteset)[frame.getSpriteIndex()];
        FrameObjectMeta meta = frame.getMeta() == null ? new FrameObjectMeta() : frame.getMeta();
        float scaleX = meta.scaleX * scale;
        float scaleY = meta.scaleY * scale;
        float offX = meta.offX * scaleX;
        float offY = meta.offY * scaleY;
        sprite.draw(posX + (offX * scaleX), posY + (offY * scaleY), scaleX, scaleY, meta.rotation + rotation, meta.flipX, meta.flipY, sprite.getTextureRegion().getRegionWidth(), sprite.getTextureRegion().getRegionHeight(), false);
    }
}
