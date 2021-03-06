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

package me.scarlet.undertailor.util;

import com.badlogic.gdx.utils.ObjectSet;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import me.scarlet.undertailor.lua.LuaObjectValue;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class for interaction with lua-based objects.
 */
public class LuaUtil {

    static final ObjectSet<Object> MISC_SET;

    static {
        MISC_SET = new ObjectSet<>();
    }

    /**
     * Iterates through a given {@link LuaTable}, passing
     * each key/value pair as {@link Varargs} to the
     * provided {@link Consumer}.
     * 
     * <p>Assuming the table is used as a table (as in, not
     * an array), the first arg within the Varargs
     * (<code>varargs.arg(1)</code>) is the key. The second
     * (<code>varargs.arg(2)</code>) is the value. If the
     * table is an array, then the first arg refers to the
     * value, while the second remains nil.</p>
     * 
     * @param table the table to iterate through
     * @param consumer the consumer processing each table
     *        entry
     */
    public static void iterateTable(LuaTable table, Consumer<Varargs> consumer) {
        LuaValue key = LuaValue.NIL;
        while (true) {
            Varargs pair = table.next(key);
            if (pair.isnil(1)) {
                break;
            }

            consumer.accept(pair);
            key = pair.arg1();
        }
    }

    /**
     * Copies the values of the provided table into a new
     * table.
     * 
     * <p>Deep copies will also duplicate tables within the
     * source table.</p>
     * 
     * @param table the source table to copy
     * @param deep whether or not to also duplicate tables
     *        found within the source table
     * 
     * @return a copy of the provided table
     */
    public static LuaTable copyTable(LuaTable table, boolean deep) {
        if (table.next(LuaValue.NIL) == LuaValue.NIL) {
            return new LuaTable(); // copied a blank table
        }

        LuaTable copy = new LuaTable();
        iterateTable(table, vargs -> {
            if (deep && vargs.arg(2).istable()) {
                copy.set(vargs.arg(1), copyTable(vargs.arg(2).checktable(), true));
            } else {
                copy.set(vargs.arg(1), vargs.arg(2));
            }
        });

        return copy;
    }

    /**
     * Generates a new anonymously-typed {@link LuaFunction}
     * object from the provided {@link Function}.
     * 
     * @param func the function processing the Lua function
     *        call
     * 
     * @return a LuaFunction wrapping the call to the
     *         provided function
     */
    public static LuaFunction asFunction(Function<Varargs, Varargs> func) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                try {
                    return func.apply(args);
                } catch (Throwable e) {
                    throw LuaUtil.causedError(e);
                }
            }
        };
    }

    /**
     * Returns the count of entries within the provided
     * {@link LuaTable}.
     * 
     * @param table the table to count entries for
     * 
     * @return how many entries exist within the given table
     */ // because y'kno, all the length methods for LuaTable assumed integer keys
    public static int getTableSize(LuaTable table) {
        MISC_SET.clear();
        iterateTable(table, MISC_SET::add);

        return MISC_SET.size;
    }

    /**
     * Generates an array of values from the provided set of
     * {@link LuaValue}s.
     * 
     * <p>That is, each value provided is a new entry
     * assigned an integer key, the key being the index it
     * was listed in.</p>
     * 
     * @param values the values of the array
     * 
     * @return the {@link LuaTable} containing the value
     *         array
     */
    public static LuaTable arrayOf(LuaValue... values) {
        LuaTable array = new LuaTable();
        for (int i = 0; i < values.length; i++) {
            array.set(i + 1, values[i]);
        }

        return array;
    }

    /**
     * Returns a {@link Varargs} instance containing the
     * provided {@link LuaValue}s in their given order.
     * 
     * <p>Convenience method replacing
     * {@link LuaValue#varargsOf(LuaValue[])} so we can
     * actually make varargs <strong>with</strong>
     * varargs.</p>
     * 
     * @param values the values to contain within the
     *        Varargs to generate
     * 
     * @return a Varargs instance
     */
    public static Varargs varargsOf(LuaValue... values) {
        return LuaValue.varargsOf(values);
    }

    /**
     * Generates a {@link Varargs} instance, converting the
     * provided Objects into their LuaValue/LuaObjectValue
     * forms packaged into the former.
     * 
     * @param values the Objects to convert to LuaValues
     * 
     * @return the provided Objects as Varargs
     */
    public static Varargs varargsOf(Object... values) {
        LuaValue[] vargs = new LuaValue[values.length];
        for (int i = 0; i < values.length; i++) {
            Object obj = values[i];
            if (obj == null) {
                vargs[i] = LuaValue.NIL;
                continue;
            }

            if (obj instanceof LuaValue) {
                vargs[i] = (LuaValue) obj;
            } else if (NumberUtil.isNumber(obj)) {
                // Apparently, Java's strict about number casting.
                // Maybe its because they aren't primitive types
                // anymore when they're here.
                if (obj instanceof Integer) {
                    vargs[i] = LuaValue.valueOf((int) obj);
                } else if (obj instanceof Long) {
                    vargs[i] = LuaValue.valueOf((long) obj);
                } else if (obj instanceof Short) {
                    vargs[i] = LuaValue.valueOf((short) obj);
                } else if (obj instanceof Float) {
                    vargs[i] = LuaValue.valueOf((float) obj);
                } else {
                    vargs[i] = LuaValue.valueOf((double) obj);
                }
            } else if (Boolean.class.isInstance(obj)) {
                vargs[i] = LuaValue.valueOf((boolean) obj);
            } else if (String.class.isInstance(obj)) {
                vargs[i] = LuaValue.valueOf(obj.toString());
            } else {
                vargs[i] = LuaObjectValue.of(obj);
            }
        }

        return varargsOf(vargs);
    }

    /**
     * Generates an Object array containing the Java
     * versions of the values within the provided
     * {@link Varargs}.
     * 
     * <p>Note that tables are not converted.</p>
     * 
     * @param vargs the Varargs to process
     * 
     * @return a new Object array containing Java variants
     *         of the Varargs' contained values
     */
    public static Object[] asJavaVargs(Varargs vargs) {
        Object[] obj = new Object[vargs.narg()];
        for (int i = 0; i < obj.length; i++) {
            LuaValue arg = vargs.arg(i + 1);
            if (arg instanceof LuaObjectValue) {
                obj[i] = ((LuaObjectValue<?>) arg).getObject();
            } else {
                if (arg.isnumber()) {
                    obj[i] = arg.todouble();
                } else if (arg.isboolean()) {
                    obj[i] = arg.toboolean();
                } else if (arg.istable()) {
                    obj[i] = arg;
                } else {
                    obj[i] = arg.tojstring();
                }
            }
        }

        return obj;
    }

    /**
     * Utility method for more reliably unpacking a
     * {@link LuaTable}.
     * 
     * <p>Alternative for LuaTable.unpack(), of which stops
     * upon finding a nil value during numerical key
     * iteration.</p>
     * 
     * @param table the table to unpack
     * @param len the expected length of table and thus the
     *        maximum length of the returned varargs
     * 
     * @return the unpacked table
     */
    public static Varargs unpack(LuaTable table, int len) {
        LuaValue[] vargs = new LuaValue[len];
        for (int i = 0; i < len; i++) {
            vargs[i] = table.get(i + 1);
        }

        return LuaValue.varargsOf(vargs);
    }

    /**
     * Generates a {@link LuaError} to throw if there was a
     * potential cause of an exception.
     * 
     * @param cause the cause of the exception
     * 
     * @return the LuaError to throw
     */
    public static LuaError causedError(Throwable cause) {
        LuaError err;
        if (cause instanceof IllegalArgumentException) {
            err = new LuaError("bad argument: (Java)" + cause.getMessage());
        } else if (cause instanceof LuaError) {
            return (LuaError) cause;
        } else {
            return new LuaError(cause);
        }

        err.initCause(cause);
        return err;
    }
}
