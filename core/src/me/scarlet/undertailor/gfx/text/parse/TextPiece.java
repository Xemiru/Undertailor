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

package me.scarlet.undertailor.gfx.text.parse;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * A segment of parameterized text.
 * 
 * @author FerusGrim
 */
public class TextPiece {

    private final ObjectMap<TextParam, String> params;
    private String message;

    public TextPiece(String message, ObjectMap<TextParam, String> params) {
        this.message = message;
        this.params = params;
    }

    public ObjectMap<TextParam, String> getParams() {
        return this.params;
    }

    public String getMessage() {
        return this.message;
    }

    public static TextPiece of(ObjectMap<TextParam, String> params, String message) {
        return new TextPiece(message, params);
    }

    public static TextPiece of(String message) {
        return new TextPiece(message, new ObjectMap<>());
    }
}
