package me.scarlet.undertailor.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JsePlatform;

import me.scarlet.undertailor.exception.LuaScriptException;
import me.scarlet.undertailor.util.LuaUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ScriptManager {

    private static final List<LuaLibrary> BASE_LIBRARIES;

    static {
        BASE_LIBRARIES = new ArrayList<>();
    }

    public static void registerBaseLibrary(LuaLibrary library) {
        if (!ScriptManager.BASE_LIBRARIES.contains(library))
            ScriptManager.BASE_LIBRARIES.add(library);
    }

    private List<LuaLibrary> libraries;

    public ScriptManager() {
        this.libraries = new ArrayList<>();
    }

    public void registerLibrary(LuaLibrary library) {
        if (!this.libraries.contains(library))
            this.libraries.add(library);
    }

    public Globals generateGlobals() {
        Globals returned = new Globals();
        JsePlatform.standardGlobals();
        returned.load(new JseBaseLib());
        returned.load(new PackageLib());
        returned.load(new Bit32Lib());
        returned.load(new TableLib());
        returned.load(new StringLib());
        returned.load(new JseMathLib());

        returned.set("debug", LuaValue.NIL);
        LoadState.install(returned);
        LuaC.install(returned);

        for (LuaLibrary library : this.libraries) {
            returned.load(library);
        }

        return returned;
    }

    public LuaTable loadAsModule(File luaFile) throws FileNotFoundException, LuaScriptException {
        return this.loadAsModule(luaFile, null);
    }

    public LuaTable loadAsModule(File luaFile, LuaTable store)
        throws FileNotFoundException, LuaScriptException {
        LuaTable table;
        if (store == null) {
            table = new LuaTable();
        } else {
            table = store;
        }

        Globals globals = this.generateGlobals();
        InputStream stream = globals.finder.findResource(luaFile.getAbsolutePath());
        if (stream == null) {
            throw new FileNotFoundException(luaFile.getAbsolutePath());
        }

        String chunkname = "@" + luaFile.getName();
        LuaValue value = globals.load(stream, chunkname, "bt", globals).invoke().arg(1);
        if (value.istable()) {
            LuaUtil.iterateTable((LuaTable) value, vargs -> {
                table.set(vargs.arg(1), vargs.arg(2));
            });
        } else {
            throw new LuaScriptException("Script does not resolve as a module");
        }

        return table;
    }
}
