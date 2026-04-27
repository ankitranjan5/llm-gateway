# Enterprise LLM Gateway & Security Layer

A centralized, stateless AI Gateway built in Java 21 and Spring Boot. This middleware abstracts multi-provider LLM integrations (OpenAI, Anthropic, Google Gemini, Groq) for downstream enterprise microservices.

Rather than acting as a simple API wrapper, this gateway functions as an observable, self-healing intelligence layer that enforces strict data governance, optimizes inference costs through dynamic routing, and protects internal systems from volatile third-party AI network latency.

## 🏗️ Core Architecture: The Stateless Proxy

To ensure infinite horizontal scalability and eliminate the need for heavy database connection pools, the Gateway is strictly stateless. Downstream applications manage their own conversational state, while the Gateway evaluates and processes every request in isolation.

A custom stream-healing abstraction layer dynamically translates flat message arrays into the strict schemas required by different providers (e.g., extracting system prompts and merging consecutive roles for Anthropic's pedantic Messages API, or building nested multimodal parts for Gemini) entirely on the fly.

## ✨ Key Capabilities

### 1. ACL-Aware RAG (Pre-Retrieval Filtering)
Standard post-retrieval filtering in vector databases risks data leakage or returning empty contexts. This Gateway enforces **Pre-Retrieval Filtering** using PostgreSQL and `pgvector`.

Access Control Lists (ACLs) are attached directly to vector embeddings during ingestion. By leveraging PostgreSQL's array overlap operator (`&&`), the system enforces Role-Based Access Control (RBAC) *inside* the vector search query, guaranteeing mathematical certainty that the LLM is only augmented with authorized context.

### 2. Smart Routing & FinOps Arbitrage
Running trivial queries through flagship models is a massive financial drain. The Gateway features an intelligent routing engine that evaluates prompt complexity before execution.
* **Complex Reasoning:** Routed to flagship models like GPT-4o or Claude 3.5 Sonnet.
* **Simple Queries:** Transparently downgraded to highly capable open-weight models (e.g., Llama-3-70b via Groq).

### 3. Asynchronous "Shadow Cost" Observability
The Gateway tracks both the *Actual Cost* and the *Shadow Cost* (the theoretical cost if the request had not been downgraded).
To prevent blocking the main execution thread and degrading Time-To-First-Token (TTFT), the system captures the payload metadata mid-stream and executes the FinOps calculations and database persistence in an `@Async` background thread. These metrics are pushed to Grafana via Micrometer to visualize real-time infrastructure savings.

### 4. Edge Resilience & Traffic Control
Third-party AI APIs are subject to latency spikes and outages. To prevent thread exhaustion from cascading into the internal microservice ecosystem, every AI provider integration is wrapped in a dedicated **Resilience4j Circuit Breaker and Rate Limiter**.

If a provider degrades, the circuit trips, failing fast and instantly routing the request down a configured fallback chain to maintain 99.9% availability.

## 🔌 Supported AI Providers

* **OpenAI** (GPT-4o, GPT-4o-mini)
* **Anthropic** (Claude 3.5 Sonnet, Opus)
* **Google** (Gemini 2.5 Pro, Flash)
* **Groq** (Llama 3 family, Mixtral)
* **Local/OSS** (Ollama integrations for zero-cost embeddings and testing)

## 🛠️ Technology Stack

* **Language Framework:** Java 21, Spring Boot
* **AI Integration:** Official Provider SDKs, LangChain4j (Local Embeddings)
* **Database & Vector Store:** PostgreSQL with `pgvector`, Hibernate 6
* **Resilience & Security:** Resilience4j (Circuit Breakers/Rate Limiters), Spring Security
* **Observability:** Grafana

---

## 🚀 Getting Started

### 1. Infrastructure Setup (Docker Compose)
The repository includes a comprehensive `docker-compose.yml` file to instantly provision the required infrastructure layer. Running this will spin up:
* **PostgreSQL** (with the `pgvector` extension for RBAC RAG)
* **Ollama** (for local testing and zero-cost embeddings)
* **Zipkin** (for OpenTelemetry distributed tracing)
* **Prometheus & Grafana** (for FinOps and metrics observability)

```bash
docker compose up -d
```

### 2. Environment Variables (API Keys)
Security is handled via environment variables. Before starting the Spring Boot application, you must inject your provider API keys. The application.yml is configured to map these dynamically.

### 3. Run and Test
Once the infrastructure is running and variables are set, start the Spring Boot application.

You can test the Gateway's routing and stream-healing capabilities immediately using Postman or cURL. Send a POST request to your completions endpoint with the payload below.

Notice how the payload uses a flat message array with a system prompt—the Gateway will automatically extract the system instruction and format it to satisfy Anthropic's strict schema requirements on the fly.

```json
{
    "model": "claude-opus-4-7",
    "messages": [
        {
            "role": "system",
            "content": "You are a developer to me, an AI platforms architect"
        },
        {
            "role": "user",
            "content": "Testing my LLM Gateway. Let me know if it's a success. Also, are you GPT, Llama, Gemini, or Claude?"
        }
    ],
    "stream": true
}
```

## 📊 Cost Savings Dashboard Example

Below is a real Grafana dashboard showing **17% cost savings** on LLM inference, thanks to smart routing and FinOps optimizations:

![Grafana Cost Savings (17%)](docs/grafana-cost-savings.png)

This dashboard visualizes:
- Total spend and savings
- Latency by provider
- Traffic distribution
- Savings by target model
- Top spenders (user audit)
