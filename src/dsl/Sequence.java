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

    public void addCall(String apiCall) {
        calls.add(apiCall);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof Sequence))
            return false;
        Sequence seq = (Sequence) o;
        List<String> otherSequence = seq.calls;
        if (calls.size() != otherSequence.size())
            return false;
        for (int i = 0; i < calls.size(); i++)
            if (! calls.get(i).equals(otherSequence.get(i)))
                return false;
        return true;
    }

    @Override
    public int hashCode() {
        int code = 17;
        for (String call : calls)
            code = 31 * code + call.hashCode();
        return code;
    }
}
