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

package me.scarlet.undertailor.manager;

import me.scarlet.undertailor.Undertailor;
import me.scarlet.undertailor.util.LuaUtil;
import me.scarlet.undertailor.wrappers.AnimationSetWrapper;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.json.JSONConfigurationLoader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AnimationManager extends Manager<AnimationSetWrapper> {
    
    public static final String MANAGER_TAG = "animman";
    
    // owning object name, animation name, animation
    private Map<String, AnimationSetWrapper> animationMap;
    public AnimationManager() {
        this.animationMap = new HashMap<>();
    }
    
    public void loadObjects(File dir) {
        loadObjects(dir, null);
        Undertailor.instance.log(MANAGER_TAG, animationMap.keySet().size() + " animation set(s) currently loaded");
    }
    
    private void loadObjects(File dir, String heading) {
        String dirPath = dir.getAbsolutePath();
        if(!dir.exists()) {
            dir.mkdirs();
            return;
        }
        
        if(!dir.isDirectory()) {
            Undertailor.instance.warn(MANAGER_TAG, "could not load animation directory " + dirPath + " (not a directory)");
            return;
        }
        
        if(heading == null) {
            heading = "";
        }
        Undertailor.instance.log(MANAGER_TAG, "searching for animations in " + dirPath);
        for(File file : dir.listFiles(file1 -> file1.getName().endsWith(".tailoranim") || file1.isDirectory())) {
            if(file.isDirectory()) {
                loadObjects(file, heading + (heading.isEmpty() ? "" : ".") + file.getName());
                continue;
            }
            
            String name = file.getName().substring(0, file.getName().length() - 11);
            String entryName = heading + (heading.isEmpty() ? "" : ".") + name;
            JSONConfigurationLoader loader = JSONConfigurationLoader.builder().setFile(file).build();
            try {
                ConfigurationNode node = loader.load();
                if(node.getNode("meta").isVirtual() || node.getNode("animation").isVirtual()) {
                    Undertailor.instance.warn(MANAGER_TAG, "ignoring animation file " + file.getName() + " containing invalid animation configuration");
                    continue;
                }
                
                Undertailor.instance.debug(MANAGER_TAG, "loading animation set " + entryName);
                animationMap.put(entryName, new AnimationSetWrapper(entryName, node));
            } catch(Exception e) {
                Undertailor.instance.error(MANAGER_TAG, "could not load animationset " + name + ": " + LuaUtil.formatJavaException(e), e);
            }
        }
    }
    
    public AnimationSetWrapper getAnimation(String name) {
        if(animationMap.containsKey(name)) {
            return animationMap.get(name);
        }
        
        Undertailor.instance.warn(MANAGER_TAG, "system requested non-existing animation set (" + name + ")");
        return null;
    }
}
