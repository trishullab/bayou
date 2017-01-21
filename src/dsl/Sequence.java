package dsl;

import java.util.ArrayList;
import java.util.List;

public class Sequence {
    final List<String> calls;

    public Sequence() {
        this.calls = new ArrayList<>();
    }

    public Sequence(List<String> calls) {
        this.calls = new ArrayList<>(calls);
    }

    public List<String> getCalls() {
        return calls;
    }

    public void addCall(String apiCall) {
        calls.add(apiCall);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof Sequence))
            return false;
        Sequence seq = (Sequence) o;
        return calls.equals(seq.calls);
    }

    @Override
    public int hashCode() {
        int code = 17;
        for (String call : calls)
            code = 31 * code + call.hashCode();
        return code;
    }
}
