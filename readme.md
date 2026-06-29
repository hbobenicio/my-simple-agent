# my-simple-agent

## Why another agent?

- Every coding agent out there has telemetry
- Free providers gather your code/chats for retraining their models (You are the product!)
- They are full of AI Slops, downloading binaries and (even browsers!!!) hidden from you and execute on you machine
- They require postinstall npm/pip scripts. Supply Chain Attack fiesta!
- They will happily recommend npx/uvx for plugins or MCP. Supply Chain Attack fiesta!
- Some of them hide corporate MCP servers from you
- Some require internet connections and do whatever over random/proprietary servers

## Our Promises

- No telemetry. Never!
- No data out!
- No Browsers!
- No Internet access! (except for your explicit LLM URL)
- No npx/uvx, postinstall scripts madness!
- No hidden downloads over the internet, binaries or not!
- No hidden MCP's!

## Usage

```bash
java -Xss256k -Xmx64m -jar target/my-simple-agent-0.0.0-SNAPSHOT.jar

LLM_MODEL_NAME=qwen3/qwen3-4b java -Xss256k -Xmx64m -jar target/my-simple-agent-0.0.0-SNAPSHOT.jar
```

## Config Environment Variables

| Variable         | Description                                                   |
|------------------|---------------------------------------------------------------|
| `LLM_BASE_URL`   | Base URL for the LLM API. Example `http://localhost:12434/v1` |
| `LLM_MODEL_NAME` | (Optional) Model ID Name passed to LLM API                    |
| `LLM_API_KEY`    | (Optional) API Key passed to LLM API                          |
