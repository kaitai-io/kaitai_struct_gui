package io.kaitai.struct.visualizer;

import io.kaitai.struct.KaitaiStruct;

import java.lang.reflect.Field;
import java.util.Map;

public class DebugAids {
    private Map<String, Integer> attrStart;
    private Map<String, Integer> attrEnd;

    private DebugAids(Map<String, Integer> attrStart, Map<String, Integer> attrEnd) {
        this.attrStart = attrStart;
        this.attrEnd = attrEnd;
    }

    public Map<String, Integer> attrStart() {
        return attrStart;
    }

    public Map<String, Integer> attrEnd() {
        return attrEnd;
    }

    public static DebugAids fromStruct(KaitaiStruct struct) throws NoSuchFieldException, IllegalAccessException {
        Class<?> ksyClass = struct.getClass();

        Field fAttrStart = ksyClass.getDeclaredField("_attrStart");
        Field fAttrEnd = ksyClass.getDeclaredField("_attrEnd");

        return new DebugAids(
                (Map<String, Integer>) fAttrStart.get(struct),
                (Map<String, Integer>) fAttrEnd.get(struct)
        );
    }
}
