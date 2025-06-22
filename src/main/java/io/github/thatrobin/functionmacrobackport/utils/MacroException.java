package io.github.thatrobin.functionmacrobackport.utils;

import net.minecraft.text.MutableText;

public class MacroException extends Exception {
    public MacroException(MutableText translatable) {
        super(translatable.getString());
    }
}
