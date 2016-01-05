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

package me.scarlet.undertailor.scheduler;

import me.scarlet.undertailor.Undertailor;
import me.scarlet.undertailor.util.InputRetriever.InputData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Scheduler {
    
    public static int nextId;
    public static final String MANAGER_TAG = "scheduler";
    
    static {
        nextId = 0;
    }
    
    private Map<Integer, Task> tasks;
    private Map<Integer, Task> activeTasks;
    public Scheduler() {
        this.tasks = new HashMap<>();
        this.activeTasks = new LinkedHashMap<>();
    }
    
    public void process(float delta, InputData data) {
        Iterator<Entry<Integer, Task>> iterator = tasks.entrySet().iterator();
        while(iterator.hasNext()) {
            Entry<Integer, Task> entry = iterator.next();
            int id = entry.getKey();
            Task task = entry.getValue();
            String taskName = (task.getName() == null ? "#" + id : task.getName() + " (#" + id + ")");
            
            try {
                if(task.process(delta, data)) {
                    Undertailor.instance.debug(MANAGER_TAG, "task " + taskName + " finished and was removed");
                    iterator.remove();
                }
            } catch(Exception e) {
                Undertailor.instance.warn(MANAGER_TAG, "task " + taskName + " was removed due to caught error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                iterator.remove();
            }
        }
        
        iterator = activeTasks.entrySet().iterator();
        if(iterator.hasNext()) {
            Entry<Integer, Task> entry = iterator.next();
            int id = entry.getKey();
            Task task = entry.getValue();
            String taskName = (task.getName() == null ? "#" + id : task.getName() + " (#" + id + ")");
            
            try {
                if(task.process(delta, data)) {
                    Undertailor.instance.debug(MANAGER_TAG, "active task " + taskName + " finished and was removed");
                    iterator.remove();
                }
            } catch(Exception e) {
                Undertailor.instance.warn(MANAGER_TAG, "active task " + taskName + " was removed due to caught error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                iterator.remove();
            }
        }
    }
    
    public int registerTask(Task task, boolean active) {
        int id = nextId++;
        String taskName = (task.getName() == null ? "#" + id : task.getName() + " (#" + id + ")");
        if(active) {
            activeTasks.put(id, task);
            Undertailor.instance.debug(MANAGER_TAG, "active task " + taskName + " registered");
        } else {
            tasks.put(id, task);
            Undertailor.instance.debug(MANAGER_TAG, "task " + taskName + " registered");
        }
        
        return id;
    }
    
    public void cancelTask(int id) {
        if(tasks.containsKey(id)) {
            Task task = tasks.get(id);
            String taskName = (task.getName() == null ? "#" + id : task.getName() + " (#" + id + ")");
            
            task.onFinish(true);
            Undertailor.instance.debug(MANAGER_TAG, "task " + taskName + " was removed by scheduler call");
            tasks.remove(id);
        }
        
        if(activeTasks.containsKey(id)) {
            Task task = activeTasks.get(id);
            String taskName = (task.getName() == null ? "#" + id : task.getName() + " (#" + id + ")");
            
            task.onFinish(true);
            Undertailor.instance.debug(MANAGER_TAG, "active task " + taskName + " was removed by scheduler call");
            activeTasks.remove(id);
        }
    }
}
