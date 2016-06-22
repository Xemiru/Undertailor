package me.scarlet.undertailor.lua.impl;

import static me.scarlet.undertailor.lua.LuaObjectValue.of;

import me.scarlet.undertailor.engine.Collider;
import me.scarlet.undertailor.engine.overworld.WorldObject;
import me.scarlet.undertailor.exception.LuaScriptException;
import me.scarlet.undertailor.gfx.Transform;
import me.scarlet.undertailor.lua.LuaImplementable;
import me.scarlet.undertailor.lua.LuaObjectValue;
import me.scarlet.undertailor.lua.ScriptManager;
import me.scarlet.undertailor.util.LuaUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

public class LuaWorldObject extends WorldObject implements LuaImplementable<WorldObject> {

    public static final String FUNC_CREATE = "create";
    public static final String FUNC_PROCESS = "process";
    public static final String FUNC_CATCHEVENT = "catchEvent";
    public static final String FUNC_STARTCOLLISION = "startCollision";
    public static final String FUNC_ENDCOLLISION = "endCollision";
    public static final String FUNC_PRERENDER = "preRender";
    public static final String FUNC_POSTRENDER = "postRender";

    private LuaObjectValue<WorldObject> luaObj;
    private Set<String> checkCache;

    public LuaWorldObject(ScriptManager manager, File luaFile)
        throws FileNotFoundException, LuaScriptException {
        this.luaObj = LuaObjectValue.of(this);
        this.luaObj.load(manager, luaFile);
        this.checkCache = new HashSet<>();

        this.invokeSelf(FUNC_CREATE);
    }

    @Override
    public LuaObjectValue<WorldObject> getObjectValue() {
        return this.luaObj;
    }

    @Override
    public void draw(float x, float y, Transform transform) {
        if (this.hasFunction(FUNC_PRERENDER)) {
            this.invokeSelf(FUNC_PRERENDER);
        }

        super.draw(x, y, transform);

        if (this.hasFunction(FUNC_POSTRENDER)) {
            this.invokeSelf(FUNC_POSTRENDER);
        }
    }

    @Override
    public boolean processObject(Object... params) {
        if (this.checkFunction(FUNC_PROCESS, checkCache)) {
            return this.invokeSelf(FUNC_PROCESS).toboolean(1);
        }

        return false;
    }

    @Override
    public boolean catchEvent(String eventName, Object... data) {
        if (this.checkFunction(FUNC_CATCHEVENT, checkCache)) {
            return this.invokeSelf(FUNC_CATCHEVENT, LuaUtil.varargsOf(data)).toboolean(1);
        }

        return true;
    }

    @Override
    public void startCollision(Collider collider) {
        if (this.checkFunction(FUNC_STARTCOLLISION, checkCache)) {
            this.invokeSelf(FUNC_STARTCOLLISION, of(collider));
        }
    }

    @Override
    public void endCollision(Collider collider) {
        if (this.checkFunction(FUNC_ENDCOLLISION, checkCache)) {
            this.invokeSelf(FUNC_ENDCOLLISION, of(collider));
        }
    }
}
