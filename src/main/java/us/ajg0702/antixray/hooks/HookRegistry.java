package us.ajg0702.antixray.hooks;

import java.util.ArrayList;
import java.util.List;

public class HookRegistry {
    private List<Hook> hooks = new ArrayList<>();

    public Hook getHook(Class<?> type) {
        for (Hook hook : hooks) {
            if(hook.getClass().equals(type)) {
                return hook;
            }
        }
        return null;
    }

    public void add(Hook hook) {
        hooks.add(hook);
    }

    public List<Hook> getHooks() {
        return hooks;
    }
}
