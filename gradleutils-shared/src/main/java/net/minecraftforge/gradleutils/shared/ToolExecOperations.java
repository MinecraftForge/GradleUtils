package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.model.ObjectFactory;
import org.gradle.process.ExecResult;

import javax.inject.Inject;

public abstract class ToolExecOperations {
    protected abstract @Inject ObjectFactory getObjects();

    private final Plugin<? extends EnhancedPlugin<?>> plugin;

    @Inject
    public ToolExecOperations(Plugin<? extends EnhancedPlugin<?>> plugin) {
        this.plugin = plugin;
    }

    public ExecResult toolexec(Tool tool, Action<? super ToolExecSpec> action) {
        var spec = getObjects().newInstance(ToolExecSpec.class, plugin, tool);
        action.execute(spec);
        return spec.exec();
    }
}
