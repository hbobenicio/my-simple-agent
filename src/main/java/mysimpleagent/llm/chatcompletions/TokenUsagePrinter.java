package mysimpleagent.llm.chatcompletions;

import mysimpleagent.llm.chatcompletions.stream.ChatCompletionUsage;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class TokenUsagePrinter {
    private final Terminal terminal;

    public TokenUsagePrinter(Terminal terminal) {
        this.terminal = terminal;
    }

    public void println(ChatCompletionUsage tokenUsage) {
        String msg = new StringBuilder()
                .append(String.format("prompt tokens    : %d (input: system + user prompts + tools)\n", tokenUsage.promptTokens()))
                .append(String.format("reasoning tokens : %d (output)\n", tokenUsage.completionTokensDetails().reasoningTokens()))
                .append(String.format("completion tokens: %d (output: reasoning + assistant answer + tool calls)\n", tokenUsage.completionTokens()))
                .append(String.format("total tokens     : %d (input + output)", tokenUsage.totalTokens()))
                .toString();

        //  • Mensagens de Reasoning / Pensamento (Cor de Comentário do Tokyo Night):
        //      • Hex:  #565f89  (ou  #414868  em algumas variações)
        //      • ANSI TrueColor (RGB):  \x1b[38;2;86;95;137m
        //      • ANSI 256-cores:  \x1b[38;5;60m
        //  • Mensagens de Saída do Assistente (Cor de Texto Principal / Foreground do Tokyo Night):
        //      • Hex:  #c0caf5
        //      • ANSI TrueColor (RGB):  \x1b[38;2;192;202;245m
        //      • ANSI 256-cores:  \x1b[38;5;146m

        //  • Reasoning Messages (Model Thought / Comments)
        //      • RGB:  rgb(86, 95, 137)   (Hex:  #565f89 )
        //      • Alternative variant:  rgb(65, 72, 104)  (Hex:  #414868 )
        //  • Assistant Output Messages (Main Text / Foreground)
        //      • RGB:  rgb(192, 202, 245)  (Hex:  #c0caf5 )

        AttributedStyle style = AttributedStyle.DEFAULT.foreground(86, 95, 137);  // dark gray
        String ansiFormattedMsg = new AttributedStringBuilder()
                .style(style)
                .append(msg)
                .toAttributedString()
                .toAnsi(this.terminal);

        this.terminal.writer().println(ansiFormattedMsg);
        this.terminal.flush();
    }
}
