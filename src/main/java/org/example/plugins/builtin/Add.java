package org.example.plugins.builtin;

import org.example.context.PluginReadWrapper;
import org.example.context.PluginWriteWrapper;
import org.example.context.ReadContextValue;
import org.example.context.WriteContextValue;
import org.example.context.path.StringPath;
import org.example.plugins.Plugin;
import org.example.plugins.PluginCallback;
import org.example.plugins.PluginNode;

import java.util.List;

@Plugin(id = "builtin.add")
public class Add implements PluginNode {

    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper a = input.resolvePath("a");
        PluginReadWrapper b = input.resolvePath("b");

        PluginWriteWrapper returnValue = PluginWriteWrapper.getObject();

        boolean ifAisNan = isNan(a);
        boolean ifBisNan = isNan(b);

        if (ifAisNan || ifBisNan) {
            returnValue.putPath("out", toString(a) + toString(b));
            cb.completeAndReturn(returnValue, List.of());
            return;
        }

        double aD = 0.0;
        double bD = 0.0;

        long aL = 0;
        long bL = 0;

        if (a.isDouble() || a.isFloat()) {
            aD = a.asDouble(0) + a.asFloat(0);
        }

        if (a.isLong() || a.isInt()) {
            aL = a.asLong(0) + a.asInt(0);
        }

        if (a.isBoolean()) {
            aL = a.asBoolean(false)? 1: 0;
        }

        if (b.isDouble() || b.isFloat()) {
            bD = a.asDouble(0) + a.asFloat(0);
        }

        if (b.isLong() || b.isInt()) {
            bL = a.asLong(0) + a.asInt(0);
        }

        if (b.isBoolean()) {
            bL = b.asBoolean(false)? 1: 0;
        }

        if (a.isFloat() || a.isDouble() || b.isFloat() || b.isDouble()) {
             returnValue.putPath("out",aD + aL + bD + bL);
        } else {
            returnValue.putPath("out", aL + bL);
        }

        cb.completeAndReturn(returnValue, List.of());
    }

    private static boolean isNan(PluginReadWrapper val) {
        return val.isString() || val.isObject() || val.isArray() || val.isMissing();
    }

    private static String toString(PluginReadWrapper val) {
        if (val.isBoolean()) {
            return Boolean.toString(val.asBoolean(false));
        }
        if (val.isString()) {
            return val.asString("");
        }
        if (val.isInt()) {
            return Integer.toString(val.asInt(0));
        }
        if (val.isLong()) {
            return Long.toString(val.asLong(0));
        }
        if (val.isFloat()) {
            return Float.toString(val.asFloat(0));
        }
        if (val.isDouble()) {
            return Double.toString(val.asDouble(0));
        }
        if (val.isMissing()) {
            return "Missing";
        }
        if (val.isObject()) {
            return "Object";
        }
        if (val.isArray()) {
            return "Array";
        }
        return "";
    }
}
