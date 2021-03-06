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

package me.scarlet.undertailor.lua.lib.game;

import static me.scarlet.undertailor.util.LuaUtil.asFunction;
import static me.scarlet.undertailor.lua.LuaObjectValue.orNil;

import me.scarlet.undertailor.audio.AudioManager;
import me.scarlet.undertailor.lua.LuaLibrary;

/**
 * Audio library accessible by Lua.
 * 
 * <p>Wraps around {@link AudioManager}.</p>
 */
public class AudioLib extends LuaLibrary {

    public AudioLib(AudioManager manager) {
        super("audio");

        // ---------------- g/s volumes ----------------

        // audio.getMasterVolume()
        set("getMasterVolume", asFunction(vargs -> {
            return valueOf(manager.getMasterVolume());
        }));

        // audio.setMasterVolume(nVol)
        set("setMasterVolume", asFunction(vargs -> {
            manager.setMasterVolume(vargs.checknumber(1).tofloat());
            return NIL;
        }));

        // audio.getMusicVolume()
        set("getMusicVolume", asFunction(vargs -> {
            return valueOf(manager.getMusicVolume());
        }));

        // audio.setMusicVolume(nVol)
        set("setMusicVolume", asFunction(vargs -> {
            manager.setMusicVolume(vargs.checknumber(1).tofloat());
            return NIL;
        }));

        // audio.getSoundVolume()
        set("getSoundVolume", asFunction(vargs -> {
            return valueOf(manager.getSoundVolume());
        }));

        // audio.setSoundVolume(nVol)
        set("setSoundVolume", asFunction(vargs -> {
            manager.setSoundVolume(vargs.checknumber(1).tofloat());
            return NIL;
        }));

        // ---------------- loading methods ----------------

        // audio.getSound(sKey)
        set("getSound", asFunction(vargs -> {
            return orNil(manager.getSound(vargs.checkjstring(1)));
        }));

        // audio.getMusic(sKey)
        set("getMusic", asFunction(vargs -> {
            return orNil(manager.getMusic(vargs.checkjstring(1)));
        }));

        // ---------------- functional methods ----------------

        // audio.stopAllAudio()
        set("stopAllAudio", asFunction(vargs -> {
            manager.stopAllAudio();
            return NIL;
        }));
    }
}
