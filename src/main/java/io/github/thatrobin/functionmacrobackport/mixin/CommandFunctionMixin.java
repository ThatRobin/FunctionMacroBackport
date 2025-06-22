package io.github.thatrobin.functionmacrobackport.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.thatrobin.functionmacrobackport.utils.CommandFunctionAccess;
import io.github.thatrobin.functionmacrobackport.utils.Macro;
import io.github.thatrobin.functionmacrobackport.utils.MacroElement;
import io.github.thatrobin.functionmacrobackport.utils.MacroException;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(CommandFunction.class)
public class CommandFunctionMixin implements CommandFunctionAccess {

    @Override
    public CommandFunction functionMacroBackport$withMacroReplaced(@Nullable NbtCompound arguments, CommandDispatcher<ServerCommandSource> dispatcher, ServerCommandSource source) throws MacroException {
        return (CommandFunction) (Object)this;
    }

    @Unique
    private static boolean continuesToNextLine(CharSequence string) {
        int i = string.length();
        return i > 0 && string.charAt(i - 1) == '\\';
    }

    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void createMixin(Identifier id, CommandDispatcher<ServerCommandSource> dispatcher, ServerCommandSource source, List<String> lines, CallbackInfoReturnable<CommandFunction> cir) {
        List<CommandFunction.Element> list = new ArrayList(lines.size());
        Set<String> set = new ObjectArraySet();

        for(int i = 0; i < lines.size(); ++i) {
            int j = i + 1;
            String string = ((String)lines.get(i)).trim();
            String string3;
            String string2;
            if (continuesToNextLine(string)) {
                StringBuilder stringBuilder = new StringBuilder(string);

                while(true) {
                    ++i;
                    if (i == lines.size()) {
                        throw new IllegalArgumentException("Line continuation at end of file");
                    }

                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    string2 = ((String)lines.get(i)).trim();
                    stringBuilder.append(string2);
                    if (!continuesToNextLine(stringBuilder)) {
                        string3 = stringBuilder.toString();
                        break;
                    }
                }
            } else {
                string3 = string;
            }

            StringReader stringReader = new StringReader(string3);
            if (stringReader.canRead() && stringReader.peek() != '#') {
                if (stringReader.peek() == '/') {
                    stringReader.skip();
                    if (stringReader.peek() == '/') {
                        throw new IllegalArgumentException("Unknown or invalid command '" + string3 + "' on line " + j + " (if you intended to make a comment, use '#' not '//')");
                    }

                    string2 = stringReader.readUnquotedString();
                    throw new IllegalArgumentException("Unknown or invalid command '" + string3 + "' on line " + j + " (did you mean '" + string2 + "'? Do not use a preceding forwards slash.)");
                }

                if (stringReader.peek() == '$') {
                    MacroElement macroElement = parseMacro(string3.substring(1), j);
                    list.add(macroElement);
                    set.addAll(macroElement.getVariables());
                } else {
                    try {
                        ParseResults<ServerCommandSource> parseResults = dispatcher.parse(stringReader, source);
                        if (parseResults.getReader().canRead()) {
                            throw CommandManager.getException(parseResults);
                        }

                        list.add(new CommandFunction.CommandElement(parseResults));
                    } catch (CommandSyntaxException var12) {
                        throw new IllegalArgumentException("Whilst parsing command on line " + j + ": " + var12.getMessage());
                    }
                }
            }
        }

        if (set.isEmpty()) {
            cir.setReturnValue(new CommandFunction(id, list.toArray((ix) -> {
                return new CommandFunction.Element[ix];
            })));
        } else {
            cir.setReturnValue(new Macro(id, list.toArray((ix) -> {
                return new CommandFunction.Element[ix];
            }), List.copyOf(set)));
        }
        cir.cancel();
        return;
    }

    @Unique
    @VisibleForTesting
    private static MacroElement parseMacro(String macro, int line) {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        ImmutableList.Builder<String> builder2 = ImmutableList.builder();
        int i = macro.length();
        int j = 0;
        int k = macro.indexOf(36);

        while(true) {
            while(k != -1) {
                if (k != i - 1 && macro.charAt(k + 1) == '(') {
                    builder.add(macro.substring(j, k));
                    int l = macro.indexOf(41, k + 1);
                    if (l == -1) {
                        throw new IllegalArgumentException("Unterminated macro variable in macro '" + macro + "' on line " + line);
                    }

                    String string = macro.substring(k + 2, l);
                    if (!isValidMacroVariableName(string)) {
                        throw new IllegalArgumentException("Invalid macro variable name '" + string + "' on line " + line);
                    }

                    builder2.add(string);
                    j = l + 1;
                    k = macro.indexOf(36, j);
                } else {
                    k = macro.indexOf(36, k + 1);
                }
            }

            if (j == 0) {
                throw new IllegalArgumentException("Macro without variables on line " + line);
            }

            if (j != i) {
                builder.add(macro.substring(j));
            }

            return new MacroElement(builder.build(), builder2.build());
        }
    }

    @Unique
    private static boolean isValidMacroVariableName(String name) {
        for(int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }

        return true;
    }
}
