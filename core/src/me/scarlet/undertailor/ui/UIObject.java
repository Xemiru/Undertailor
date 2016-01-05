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

package me.scarlet.undertailor.ui;

import com.badlogic.gdx.math.Vector2;
import me.scarlet.undertailor.Undertailor;
import me.scarlet.undertailor.ui.event.UIEvent;
import me.scarlet.undertailor.util.InputRetriever.InputData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class UIObject {
    
    protected int id;
    protected long startLifetime;
    
    private float alpha;
    private long lifetime; //= 0 - infinitely headed, >=1 - timed headed
    private boolean headless;
    private Vector2 position;
    private boolean isVisible;
    private Set<UIComponent> markAdd;
    private Set<UIComponent> markRemove;
    private List<UIComponent> components;
    
    public UIObject() {
        this(0, false);
    }
    
    public UIObject(long lifetime, boolean headless) {
        this.position = new Vector2(0, 0);
        this.components = new ArrayList<>();
        this.markRemove = new HashSet<>();
        this.markAdd = new HashSet<>();
        this.isVisible = true;
        this.lifetime = lifetime < 0 ? 0 : lifetime;
        this.headless = headless;
        this.id = -1;
    }
    
    public int getId() {
        return id;
    }
    
    public boolean isHeadless() {
        return headless;
    }
    
    public long getLifetime() {
        return lifetime < 0 ? 0 : lifetime;
    }
    
    public Vector2 getPosition() {
        return this.position;
    }
    
    public boolean isVisible() {
        return isVisible;
    }
    
    public void setVisible(boolean flag) {
        this.isVisible = flag;
    }
    
    public void pushEvent(UIEvent event) {
       this.components.forEach(component -> {
            if(this.isComponentActive(component)) {
                component.onEvent(event);
            }
        });
       
       cleanup();
    }
    
    public void process(float delta, InputData input) {
        this.components.forEach(component -> {
            if(this.isComponentActive(component)) {
                component.process(delta, input);
            }
        });
        
        cleanup();
    }
    
    public void render() {
        if(!isVisible) {
            return;
        }
        
        this.components.forEach(component -> {
            if(this.isComponentActive(component)) {
                component.render(alpha);
            } else {
                if(component.renderWhenInactive()) component.render(alpha);
            }
        });
    }
    
    public void destroy() {
        for(UIComponent child : components) {
            child.onDestroy(true);
        }
        
        Undertailor.getUIController().destroyObject(id);
    }
    
    public boolean isComponentActive(UIComponent component) {
        if(!component.getParent().equals(this)) {
            throw new IllegalArgumentException("Component was not a child");
        } else {
            if(this.markRemove.contains(component)) {
                return false;
            } else {
                if(this.components.isEmpty()) {
                    return component.isAlwaysActive(); // weird state, but whatever
                } else {
                    return component.isAlwaysActive() || component.equals(getTopActive());
                }
            }
        }
    }
    
    public void registerChild(UIComponent component) {
        this.markAdd.add(component);
        if(id <= -1) {
            cleanup();
        }
    }
    
    public void destroyChild(UIComponent component) {
        if(component.getParent().equals(this)) {
            this.markRemove.add(component);
            component.onDestroy(false);
            if(id <= -1) {
                cleanup();
            }
        } else {
            Undertailor.instance.warn("ui", "request ignored to destroy non-child component");
        }
    }
    
    private void cleanup() {
        Iterator<UIComponent> marked = markRemove.iterator();
        while(marked.hasNext()) {
            UIComponent component = marked.next();
            component.parent = null;
            this.components.remove(component);
            marked.remove();
        }
        
        marked = markAdd.iterator();
        while(marked.hasNext()) {
            UIComponent component = marked.next();
            component.parent = this;
            this.components.add(component);
            marked.remove();
        }
    }
    
    private UIComponent getTopActive() {
        for(UIComponent component : components) {
            if(!component.isAlwaysActive()) {
                return component;
            }
        }
        
        return components.isEmpty() ? null : components.get(0);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(id + ":");
        for(UIComponent comp : components) {
            sb.append(" " + comp.getComponentTypeName());
        }
        
        return sb.toString().trim();
    }
}
