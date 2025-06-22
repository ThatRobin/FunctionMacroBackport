package io.github.thatrobin.functionmacrobackport.mixin;

import com.mojang.brigadier.CommandDispatcher;
import io.github.thatrobin.functionmacrobackport.utils.CommandFunctionAccess;
import io.github.thatrobin.functionmacrobackport.utils.CommandFunctionManagerAccess;
import io.github.thatrobin.functionmacrobackport.utils.MacroException;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CommandFunctionManager.class)
public abstract class CommandFunctionManagerMixin implements CommandFunctionManagerAccess {
    @Unique private static final Text NO_TRACE_IN_FUNCTION_TEXT = Text.translatable("commands.debug.function.noRecursion");

    @Shadow public abstract CommandDispatcher<ServerCommandSource> getDispatcher();

    @Shadow @Nullable @Mutable
    private CommandFunctionManager.Execution execution;

    @Override
    public int functionMacroBackport$execute(CommandFunction function, ServerCommandSource source, @Nullable CommandFunctionManager.Tracer tracer, @Nullable NbtCompound arguments) throws MacroException {
        CommandFunction commandFunction = ((CommandFunctionAccess)function).functionMacroBackport$withMacroReplaced(arguments, this.getDispatcher(), source);
        if (this.execution != null) {
            if (tracer != null) {
                this.execution.reportError(NO_TRACE_IN_FUNCTION_TEXT.getString());
                return 0;
            }
            this.execution.recursiveRun(commandFunction, source);
            return 0;
        }
        try {
            CommandFunctionManager manager  = (CommandFunctionManager)(Object)this;
            this.execution = manager.new Execution(tracer);
            int n = this.execution.run(commandFunction, source);
            return n;
        } finally {
            this.execution = null;
        }
    }

}
