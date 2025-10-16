
package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.game.action.ActionIntent;

import java.util.ArrayList;
import java.util.List;

public final class IntentCollector {
    private final List<ActionIntent> list = new ArrayList<>();
    public synchronized void add(ActionIntent i) { if (i != null) list.add(i); }
    public synchronized List<ActionIntent> drain() {
        var out = List.copyOf(list);
        list.clear();
        return out;
    }
}
