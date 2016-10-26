package io.kaitai.struct.visualizer;

import io.kaitai.struct.KaitaiStruct;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

public class DebugAids {
    private Map<String, Integer> attrStart;
    private Map<String, Integer> attrEnd;
    private Map<String, ArrayList<Integer>> arrStart;
    private Map<String, ArrayList<Integer>> arrEnd;

    private DebugAids(
            Map<String, Integer> attrStart,
            Map<String, Integer> attrEnd,
            Map<String, ArrayList<Integer>> arrStart,
            Map<String, ArrayList<Integer>> arrEnd
    ) {
        this.attrStart = attrStart;
        this.attrEnd = attrEnd;
        this.arrStart = arrStart;
        this.arrEnd = arrEnd;
    }

    public Integer getStart(String attrName) {
        return attrStart.get(attrName);
    }

    public Integer getEnd(String attrName) {
        return attrEnd.get(attrName);
    }

    public Integer getStart(String attrName, int idx) {
        ArrayList<Integer> positions = arrStart.get(attrName);
        return (positions != null) ? positions.get(idx) : null;
    }

    public Integer getEnd(String attrName, int idx) {
        ArrayList<Integer> positions = arrEnd.get(attrName);
        return (positions != null) ? positions.get(idx) : null;
    }

    public static DebugAids fromStruct(KaitaiStruct struct) throws NoSuchFieldException, IllegalAccessException {
        Class<?> ksyClass = struct.getClass();

        Field fAttrStart = ksyClass.getDeclaredField("_attrStart");
        Field fAttrEnd = ksyClass.getDeclaredField("_attrEnd");
        Field fArrStart = ksyClass.getDeclaredField("_arrStart");
        Field fArrEnd = ksyClass.getDeclaredField("_arrEnd");

        return new DebugAids(
                (Map<String, Integer>) fAttrStart.get(struct),
                (Map<String, Integer>) fAttrEnd.get(struct),
                (Map<String, ArrayList<Integer>>) fArrStart.get(struct),
                (Map<String, ArrayList<Integer>>) fArrEnd.get(struct)
        );
    }
}
