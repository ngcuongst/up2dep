package constants;

import com.intellij.util.messages.Topic;
import listener.IQuickFixListener;

public class Topics {
    public static Topic<IQuickFixListener> QUICK_FIX_LISTENER_TOPIC = Topic.create("QuickFixListener", IQuickFixListener.class);
}
