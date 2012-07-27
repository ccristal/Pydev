package com.aptana.js.interactive_console.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;

import com.aptana.interactive_console.console.ui.IConsoleStyleProvider;
import com.aptana.interactive_console.console.ui.ScriptStyleRange;
import com.aptana.js.interactive_console.console.prefs.ColorManager;
import com.aptana.shared_core.utils.FastStringBuffer;
import com.aptana.shared_core.utils.Tuple;

/**
 * @author fabioz
 *
 */
public final class ConsoleStyleProvider implements IConsoleStyleProvider {

    private class AnsiState {
        private FastStringBuffer escBuf = new FastStringBuffer();
        private FastStringBuffer buf = new FastStringBuffer();
        private int state = 0;
        private static final int STATE_DEFAULT = 0;
        private static final int STATE_IN_ANSI = 1;
        private TextAttribute prevAttribute;
    }

    private final AnsiState err = new AnsiState();
    private final AnsiState out = new AnsiState();

    private ScriptStyleRange getIt(String content, int offset, TextAttribute attr, int scriptStyle) {
        //background is the default (already set)
        return new ScriptStyleRange(offset, content.length(), attr.getForeground(), null, scriptStyle, attr.getStyle());
    }

    public Tuple<List<ScriptStyleRange>, String> createInterpreterStdStyle(String content, int offset,
            AnsiState ansiState, ColorManager colorManager, TextAttribute attr, int style) {

        Tuple<List<ScriptStyleRange>, String> ret = new Tuple<List<ScriptStyleRange>, String>(
                new ArrayList<ScriptStyleRange>(), "");
        FastStringBuffer retBuf = new FastStringBuffer();

        if (ansiState.prevAttribute == null) {
            ansiState.prevAttribute = attr;
        }

        int len = content.length();
        for (int i = 0; i < len; i++) {
            char c = content.charAt(i);

            if (ansiState.state == AnsiState.STATE_IN_ANSI) {
                ansiState.escBuf.append(c);

                if (!Character.isDigit(c) && c != ';' && c != '[') {
                    String str = ansiState.escBuf.toString();
                    ansiState.prevAttribute = colorManager.getAnsiTextAttribute(str, ansiState.prevAttribute, attr);

                    ansiState.escBuf.clear();
                    ansiState.state = AnsiState.STATE_DEFAULT;
                }
            } else {
                if (c == '\u001B') {
                    int bufLen = ansiState.buf.length();
                    if (bufLen > 0) {
                        ret.o1.add(getIt(ansiState.buf.toString(), offset, ansiState.prevAttribute, style));
                        retBuf.append(ansiState.buf);
                        offset += bufLen;
                        ansiState.buf.clear();
                    }
                    ansiState.state = AnsiState.STATE_IN_ANSI;
                } else {
                    ansiState.buf.append(c);
                }
            }
        }

        int bufLen = ansiState.buf.length();
        if (bufLen > 0) {
            ret.o1.add(getIt(ansiState.buf.toString(), offset, ansiState.prevAttribute, style));
            retBuf.append(ansiState.buf);
            offset += bufLen;
            ansiState.buf.clear();
        }
        ret.o2 = retBuf.toString();
        return ret;
    }

    public Tuple<List<ScriptStyleRange>, String> createInterpreterErrorStyle(String content, int offset) {
        ColorManager colorManager = ColorManager.getDefault();
        TextAttribute attr = colorManager.getConsoleErrorTextAttribute();
        return createInterpreterStdStyle(content, offset, err, colorManager, attr, ScriptStyleRange.STDERR);
    }

    public Tuple<List<ScriptStyleRange>, String> createInterpreterOutputStyle(String content, int offset) {
        ColorManager colorManager = ColorManager.getDefault();
        TextAttribute attr = colorManager.getConsoleOutputTextAttribute();
        return createInterpreterStdStyle(content, offset, out, colorManager, attr, ScriptStyleRange.STDOUT);
    }

    public ScriptStyleRange createPromptStyle(String content, int offset) {
        TextAttribute attr = ColorManager.getDefault().getConsolePromptTextAttribute();
        return getIt(content, offset, attr, ScriptStyleRange.PROMPT);
    }

    public ScriptStyleRange createUserInputStyle(String content, int offset) {
        TextAttribute attr = ColorManager.getDefault().getConsoleInputTextAttribute();
        return getIt(content, offset, attr, ScriptStyleRange.STDIN);
    }
}