package mysimpleagent.acp;

import com.agentclientprotocol.sdk.agent.AcpAgent;
import com.agentclientprotocol.sdk.agent.AcpSyncAgent;
import com.agentclientprotocol.sdk.agent.SyncPromptContext;
import com.agentclientprotocol.sdk.agent.transport.StdioAcpAgentTransport;
import com.agentclientprotocol.sdk.spec.AcpSchema;

import java.util.UUID;

public class AcpServer implements Runnable {

    public void run() {
        var transport = new StdioAcpAgentTransport();

        AcpSyncAgent agent = AcpAgent.sync(transport)
                .initializeHandler(this::syncInitHandler)
                .newSessionHandler(this::newSessionSyncHandler)
                .promptHandler(this::promptSyncHandler)
                .build();

        agent.run();  // Blocks until client disconnects
    }

    private AcpSchema.InitializeResponse syncInitHandler(AcpSchema.InitializeRequest initializeRequest) {
        return AcpSchema.InitializeResponse.ok();
    }

    private AcpSchema.NewSessionResponse newSessionSyncHandler(AcpSchema.NewSessionRequest newSessionRequest) {
        return new AcpSchema.NewSessionResponse(UUID.randomUUID().toString(), null, null);
    }

    private AcpSchema.PromptResponse promptSyncHandler(AcpSchema.PromptRequest promptRequest, SyncPromptContext ctx) {
//        context.sendThought("Thinking...");
//        context.sendMessage("Here's my response.");
//        return PromptResponse.endTurn();
        ctx.sendMessage("Hello from the agent!");
        return AcpSchema.PromptResponse.endTurn();
    }
}
