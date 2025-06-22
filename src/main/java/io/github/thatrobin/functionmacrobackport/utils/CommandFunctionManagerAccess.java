package io.github.thatrobin.functionmacrobackport.utils;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import org.jetbrains.annotations.Nullable;

public interface CommandFunctionManagerAccess {

    int functionMacroBackport$execute(CommandFunction function, ServerCommandSource source, @Nullable CommandFunctionManager.Tracer tracer, @Nullable NbtCompound arguments) throws MacroException;
}
