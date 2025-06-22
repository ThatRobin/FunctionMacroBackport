package io.github.thatrobin.functionmacrobackport.utils;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import org.jetbrains.annotations.Nullable;

public interface CommandFunctionAccess {

    CommandFunction functionMacroBackport$withMacroReplaced(@Nullable NbtCompound arguments, CommandDispatcher<ServerCommandSource> dispatcher, ServerCommandSource source) throws MacroException;
}
