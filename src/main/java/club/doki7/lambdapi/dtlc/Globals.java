package club.doki7.lambdapi.dtlc;

import java.util.HashMap;
import java.util.Map;

public record Globals(Map<String, Value> values, Map<String, Type> types) {
    public void clear() {
        values.clear();
        types.clear();
    }

    public static Globals empty() {
        return new Globals(new HashMap<>(), new HashMap<>());
    }
}
