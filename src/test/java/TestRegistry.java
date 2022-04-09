import junit.framework.TestFailure;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.Test;
import us.ajg0702.antixray.Main;
import us.ajg0702.antixray.hooks.Hook;
import us.ajg0702.antixray.hooks.HookRegistry;

public class TestRegistry {
    @Test
    public void RegistryTest() {
        HookRegistry registry = new HookRegistry();
        registry.add(new TestHook());

        if(registry.getHook(TestHook.class).isEnabled()) {
            System.out.println("Im enabled!");
        }
    }

    public static class TestHook extends Hook {

        protected TestHook() {
            super(null, null, true);
        }

        @Override
        public boolean hasRequiredPlugin() {
            return true;
        }

        @Override
        public boolean check(Player player, Location location) {
            return false;
        }
    }
}
