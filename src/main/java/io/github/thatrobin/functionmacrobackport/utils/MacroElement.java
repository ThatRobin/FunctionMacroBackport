package io.github.thatrobin.functionmacrobackport.utils;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.thatrobin.functionmacrobackport.FunctionMacroBackportMod;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.List;

public class MacroElement implements CommandFunction.Element {
    private final List<String> parts;
    private final List<String> variables;

    public MacroElement(List<String> parts, List<String> variables) {
        this.parts = parts;
        this.variables = variables;
    }

    public List<String> getVariables() {
        return this.variables;
    }

    public String getCommand(List<String> arguments) {
        StringBuilder stringBuilder = new StringBuilder();

        for(int i = 0; i < this.variables.size(); ++i) {
            stringBuilder.append(this.parts.get(i)).append(arguments.get(i));
        }

        if (this.parts.size() > this.variables.size()) {
            stringBuilder.append(this.parts.get(this.parts.size() - 1));
        }
        FunctionMacroBackportMod.LOGGER.info(stringBuilder.toString());
        return stringBuilder.toString();
    }

    public void execute(CommandFunctionManager commandFunctionManager, ServerCommandSource serverCommandSource, Deque<CommandFunctionManager.Entry> deque, int i, int j, @Nullable CommandFunctionManager.Tracer tracer) throws CommandSyntaxException {
        throw new IllegalStateException("Tried to execute an uninstantiated macro");
    }

    @Override
    public String toString() {
        return "{" + this.variables.toString() + ", " + this.parts.toString() + "}";
    }
}
