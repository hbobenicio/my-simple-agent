package br.com.hbobenicio.mysimpleagent.llm.chatcompletions;

import br.com.hbobenicio.mysimpleagent.llm.chatcompletions.payloads.stream.CompletionUsage;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.Optional;

public class TokenUsagePrinter {
    private final Terminal terminal;

    public TokenUsagePrinter(Terminal terminal) {
        this.terminal = terminal;
    }

    public void println(CompletionUsage tokenUsage) {
        var sb = new StringBuilder(512);
        sb.append("prompt tokens    : ");
        sb.append(tokenUsage.promptTokens());
        sb.append(" (input: system + user prompts + tools) ");

        var maybePromptTokenDetails = Optional.ofNullable(tokenUsage.promptTokensDetails());
        if (maybePromptTokenDetails.isPresent()) {
            sb.append("[cached: ");
            sb.append(maybePromptTokenDetails.get().cachedTokens());
            sb.append("]");
        }
        sb.append("\n");

        var maybeCompletionTokenDetails = Optional.ofNullable(tokenUsage.completionTokensDetails());
        if (maybeCompletionTokenDetails.isPresent()) {
            sb.append("reasoning tokens : ");
            sb.append(maybeCompletionTokenDetails.get().reasoningTokens());
            sb.append(" (output)\n");
        }

        sb.append("completion tokens: ");
        sb.append(tokenUsage.completionTokens());
        sb.append(" (output: reasoning + assistant answer + tool calls)\n");

        sb.append("total tokens     : ");
        sb.append(tokenUsage.totalTokens());
        sb.append(" (input + output)");

        String msg = sb.toString();

        AttributedStyle style = AttributedStyle.DEFAULT.foreground(86, 95, 137);
        String ansiFormattedMsg = new AttributedStringBuilder()
                .style(style)
                .append(msg)
                .toAttributedString()
                .toAnsi(this.terminal);

        this.terminal.writer().println(ansiFormattedMsg);
        this.terminal.flush();
    }
}
