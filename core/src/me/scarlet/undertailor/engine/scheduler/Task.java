/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Tellerva, Marc Lawrence
 *
 * Permission is hereby granted, free of charge, to any
 * person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the
 * Software without restriction, including without
 * limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package me.scarlet.undertailor.engine.scheduler;

import me.scarlet.undertailor.engine.Processable;

/**
 * Functional class used to perform a task to run across
 * several frames of a running game instance.
 */
public interface Task extends Processable {

    /**
     * Returns the name of this {@link Task}.
     * 
     * @return the name of this Task
     */
    String getName();

    /**
     * Called when this {@link Task} finishes (when
     * {@link #process(Object...)} returns false).
     * 
     * @param forced if the task was ended preemptively by
     *        means of an error or a scheduler call
     */
    void onFinish(boolean forced);

    /**
     * Processes this {@link Task} for the current frame.
     * 
     * <p>The generic return value resolves to whether or
     * not this Task should keep running after this method
     * has been called. If false is returned, the Task is
     * removed from the host scheduler.</p>
     * 
     * @return if the Task should keep running after this
     *         frame
     */
    @Override
    boolean process(Object... params);
}