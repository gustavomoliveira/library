# Quarta Entrega: Refatoração para Arquitetura Orientada a Eventos

> Disciplina: Engenharia de Softwares Escaláveis
> Projeto: pblibrary
> Autor: Gustavo M. Oliveira

## 1. Objetivo da etapa

Nas entregas anteriores, o serviço `fines-api` (multas) foi extraído do monólito `library-api` como um microsserviço independente, comunicando-se de forma **síncrona** via **Feign** no momento da devolução de um empréstimo (`POST /loans/{id}/return`). Essa comunicação síncrona criava um acoplamento temporal forte entre os dois serviços: se `fines-api` estivesse fora do ar, lento, ou apenas indisponível por um instante, a operação de devolução do empréstimo em `library-api` também falhava ou ficava sujeita à indisponibilidade do serviço dependente, mesmo a devolução em si sendo uma operação que não deveria depender do cálculo de multas para ser concluída.

O objetivo desta quarta etapa é **desacoplar** essa comunicação, substituindo a chamada síncrona via Feign por uma **comunicação assíncrona orientada a eventos**, usando um message broker (RabbitMQ). Com isso:

- `library-api` publica um evento de domínio (`LoanReturnedEvent`) quando um empréstimo é devolvido, sem saber (nem se importar) quem consome esse evento;
- `fines-api` consome esse evento de forma independente e calcula a multa, se houver;
- a indisponibilidade temporária de `fines-api` deixa de impedir a devolução do empréstimo em `library-api`.

Como orientação principal para toda a implementação desta etapa, foi adotado um critério explícito de simplicidade: **tudo deveria ser implementado da forma mais simples possível**, evitando overengineering, com o único objetivo de demonstrar que a comunicação orientada a eventos funciona de ponta a ponta — incluindo tratamento básico de falhas (fila de mensagens mortas) e idempotência do consumidor — sem introduzir complexidade desnecessária (sem Sagas, sem Outbox Pattern nesta etapa, sem múltiplos exchanges/filas por tipo de evento, etc.).

## 2. Avaliação da Arquitetura Orientada a Eventos

Antes de decidir pela implementação, foi feita uma avaliação consciente da troca entre manter a comunicação síncrona (Feign) e migrar para comunicação assíncrona orientada a eventos (RabbitMQ), incluindo a comparação com Kafka como alternativa de broker.

### 2.1 Prós

- **Desacoplamento temporal**: o produtor (`library-api`) não precisa que o consumidor (`fines-api`) esteja disponível no momento da publicação. A devolução do empréstimo é concluída independentemente do estado do serviço de multas.
- **Resiliência a falhas do consumidor**: se `fines-api` cair, as mensagens permanecem na fila do RabbitMQ até que o serviço volte, sem perda de dados nem necessidade de reprocessamento manual (ver Cenário 2).
- **Escalabilidade independente**: cada serviço pode escalar de acordo com sua própria carga, sem que um dependa diretamente da capacidade de resposta do outro.
- **Evolução mais simples do domínio**: novos serviços interessados em "empréstimo devolvido" podem simplesmente se inscrever no mesmo evento, sem exigir alterações em `library-api` (característica de um modelo pub/sub).

### 2.2 Contras

- **Complexidade operacional adicional**: é necessário manter um broker de mensagens em execução (RabbitMQ), o que adiciona mais uma peça de infraestrutura para configurar, monitorar e versionar (via `docker-compose.yml`).
- **Consistência eventual em vez de imediata**: o cálculo da multa não acontece mais no mesmo instante da devolução, e sim de forma assíncrona, o que exige que o consumidor lide com identidade e idempotência (ver seção 4.3) e que o sistema como um todo tolere uma pequena janela de atraso.
- **Depuração mais difícil**: rastrear o fluxo de uma requisição que atravessa um broker é mais trabalhoso do que seguir uma pilha de chamadas síncronas — exige correlacionar logs de dois serviços distintos.
- **Limitação identificada nesta implementação**: caso o próprio broker esteja indisponível no momento da publicação, o evento é perdido (ver Cenário 4 e a seção 8, que discute o Transactional Outbox Pattern como evolução futura para este ponto).

### 2.3 Quando vale a pena

A comunicação orientada a eventos compensa quando a ação que dispara o evento (aqui, a devolução do empréstimo) **não deveria depender logicamente** do sucesso da ação consequente (aqui, o cálculo da multa) — ou seja, quando existe uma fronteira de domínio clara entre "o que aconteceu" e "o que fazer a respeito disso". Esse é exatamente o caso do par empréstimo/multa: a devolução é um fato que ocorreu independentemente de haver ou não multa a ser calculada depois. Quando, ao contrário, o resultado do serviço dependente é indispensável para a operação atual (por exemplo, uma validação que bloqueia a operação se falhar), a comunicação síncrona continua sendo a escolha mais direta.

Entre RabbitMQ e Kafka, optou-se por RabbitMQ por ser mais simples de configurar e operar localmente para o volume e a natureza deste caso de uso (mensagens de comando/evento discretas, sem necessidade de replay de histórico ou processamento de streams), o que está alinhado ao critério de simplicidade definido para esta etapa.

## 3. Arquitetura resultante

```
                          HTTP (síncrono)                       HTTP (síncrono)
        ┌──────────┐   POST /loans/{id}/return    ┌──────────────┐
        │  Cliente │ ────────────────────────────▶│  library-api │
        └──────────┘                               └──────┬───────┘
                                                            │
                                                            │ publica evento
                                                            │ (após commit da transação)
                                                            ▼
                                        ┌───────────────────────────────────┐
                                        │        RabbitMQ (broker)          │
                                        │                                   │
                                        │  Exchange: library.events (topic) │
                                        │        routing key:               │
                                        │        loan.returned              │
                                        │                                   │
                                        │  Fila: fines.loan-returned        │
                                        │        (bound a loan.returned)    │
                                        │                                   │
                                        │  Fila: fines.loan-returned.dlq    │
                                        │        (dead letter queue)        │
                                        └────────────────┬──────────────────┘
                                                          │ consome
                                                          ▼
                                                  ┌──────────────┐
                                                  │  fines-api   │
                                                  └──────┬───────┘
                                                          │
                     ┌────────────────┐                  │              ┌──────────────────┐
                     │ discovery-server │◀── registro ────┼── registro ─▶│ (Eureka)          │
                     └────────────────┘                  │              └──────────────────┘
                                                          │
        ┌──────────────┐                          ┌──────┴───────┐
        │  library-db   │◀── JPA/Hibernate ────────│  library-api │
        │  (Postgres)   │                          └──────────────┘
        └──────────────┘

        ┌──────────────┐                          ┌──────────────┐
        │  fines-db     │◀── JPA/Hibernate ────────│  fines-api    │
        │  (Postgres)   │                          └──────────────┘
        └──────────────┘
```

Pontos-chave da arquitetura:

- A comunicação `library-api → fines-api` deixou de existir via Feign/HTTP e passou a ser inteiramente mediada pelo RabbitMQ.
- `discovery-server` (Eureka) permanece em uso apenas para o registro dos serviços — não é mais utilizado para descoberta de instâncias entre `library-api` e `fines-api`, já que essa comunicação direta foi removida.
- Cada serviço mantém seu próprio banco de dados (`library-db` e `fines-db`), reforçando o isolamento de domínio já estabelecido nas entregas anteriores.
- Toda a infraestrutura local (RabbitMQ + bancos Postgres) é orquestrada via `docker-compose.yml` na raiz do repositório.

## 4. Padrões de mensagem implementados

### 4.1 Event Notification (pub/sub via topic exchange)

O padrão adotado é o de **Event Notification**: `library-api` publica um fato de domínio ocorrido (`LoanReturnedEvent`) em um **exchange do tipo topic** (`library.events`), sem endereçar diretamente nenhum consumidor específico. `fines-api` declara sua própria fila (`fines.loan-returned`) e a vincula (*binding*) ao exchange usando a routing key `loan.returned`. Esse desenho permite que, no futuro, outros serviços se inscrevam no mesmo evento sem qualquer alteração em `library-api`.

### 4.2 Dead Letter Queue (DLQ)

A fila principal de `fines-api` (`fines.loan-returned`) foi configurada com os argumentos `x-dead-letter-exchange` e `x-dead-letter-routing-key`, de forma que qualquer mensagem rejeitada (por exceção não tratada no listener, esgotamento das tentativas de retry, ou falha de conversão/desserialização) seja automaticamente redirecionada para uma fila secundária, a **Dead Letter Queue** (`fines.loan-returned.dlq`), em vez de ser descartada silenciosamente. Isso foi combinado com a política de retry do próprio Spring AMQP (`spring.rabbitmq.listener.simple.retry.enabled=true`, `max-attempts=3`), de forma que uma falha passageira seja reprocessada algumas vezes antes de a mensagem ser considerada definitivamente não processável e enviada à DLQ (ver Cenário 3).

### 4.3 Consumidor idempotente

Como a entrega de mensagens em sistemas de mensageria não garante exclusividade (uma mesma mensagem pode, em cenários de falha/retry, ser entregue mais de uma vez), o listener em `fines-api` foi implementado de forma idempotente: antes de criar uma nova multa, verifica-se se já existe uma multa registrada para aquele `loanId` (`existsByLoanId`). Como camada adicional de segurança — caso duas instâncias processassem a mesma mensagem concorrentemente —, a coluna `loan_id` da tabela `fine` recebeu uma constraint `UNIQUE` no banco de dados, garantindo que o próprio banco rejeite qualquer duplicidade que escapasse da verificação em nível de aplicação.

## 5. Detalhamento técnico

### 5.1 Produtor (`library-api`)

A configuração de infraestrutura do RabbitMQ (exchange e conversor de mensagens JSON) é declarada em código:

```java
// library-api: infrastructure/messaging/RabbitMQConfig.java
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "library.events";
    public static final String LOAN_RETURNED_KEY = "loan.returned";

    @Bean
    public TopicExchange libraryEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
```

O evento de domínio é um record simples, posicionado no pacote de domínio (`domain.loan`), e não em infraestrutura — reforçando que ele representa um conceito do domínio, não um detalhe técnico de mensageria:

```java
// library-api: domain/loan/LoanReturnedEvent.java
public record LoanReturnedEvent(Long loanId, Long userId, LocalDate loanDate, LocalDate returnDate) {
}
```

O ponto mais importante da implementação do produtor é **quando** o evento é efetivamente publicado no broker. Publicar dentro da mesma transação que persiste a devolução do empréstimo criaria o risco de o evento ser publicado mesmo que a transação viesse a sofrer rollback depois. Por isso, o serviço de domínio apenas registra a intenção do evento via `ApplicationEventPublisher` (evento interno do Spring, não confundir com o RabbitMQ):

```java
// library-api: domain/loan/LoanService.java (trecho de returnLoan())
eventPublisher.publishEvent(new LoanReturnedEvent(
    savedLoan.getId(),
    savedLoan.getUser().getId(),
    savedLoan.getLoanDate(),
    savedLoan.getReturnDate()
));
```

E a publicação real no RabbitMQ só acontece depois que a transação é efetivamente confirmada no banco, através de um listener separado com `@TransactionalEventListener(phase = AFTER_COMMIT)`:

```java
// library-api: infrastructure/messaging/LoanEventPublisher.java
@Component
public class LoanEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public LoanEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanReturned(LoanReturnedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.LOAN_RETURNED_KEY, event);
        } catch (Exception e) {
            log.error("Falha ao publicar LoanReturnedEvent para o empréstimo {}", event.loanId(), e);
        }
    }
}
```

Essa separação entre "o que aconteceu no domínio" e "quando/como isso é propagado para fora do serviço" é o que garante que nenhum evento seja publicado para uma devolução que, no fim das contas, não foi persistida.

### 5.2 Consumidor (`fines-api`)

A infraestrutura de fila (incluindo DLQ e binding) também é declarada em código, junto ao serviço consumidor:

```java
// fines-api: infrastructure/messaging/RabbitMQConfig.java
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE = "fines.loan-returned";
    public static final String DLQ = "fines.loan-returned.dlq";
    public static final String EXCHANGE = "library.events";
    public static final String ROUTING_KEY = "loan.returned";

    @Bean
    public Queue loanReturnedQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Queue loanReturnedDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public TopicExchange libraryEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(loanReturnedQueue())
                .to(libraryEventsExchange())
                .with(ROUTING_KEY);
    }
}
```

O listener consome o evento, verifica idempotência e cria a multa quando aplicável. Deliberadamente, **não há try/catch** ao redor do processamento: qualquer exceção deve propagar para que o mecanismo de retry do Spring e, em seguida, o roteamento para a DLQ, sejam acionados corretamente.

```java
// fines-api: infrastructure/messaging/LoanReturnedListener.java
@Component
public class LoanReturnedListener {

    private final FineService fineService;

    public LoanReturnedListener(FineService fineService) {
        this.fineService = fineService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handle(LoanReturnedEvent event) {
        if (fineService.existsByLoanId(event.loanId())) {
            log.info("Multa já registrada para o empréstimo {}, ignorando (idempotência)", event.loanId());
            return;
        }
        fineService.processReturnedLoan(event);
    }
}
```

Note-se que `fines-api` mantém sua **própria cópia local** do record `LoanReturnedEvent`, no pacote de infraestrutura — não há biblioteca compartilhada entre os dois serviços, mantendo a consistência com decisões tomadas em etapas anteriores do projeto (cada bounded context define seus próprios contratos).

### 5.3 Infraestrutura declarada em código

| Elemento | Declarado por | Observação |
|---|---|---|
| Exchange `library.events` (topic) | `library-api` **e** `fines-api` | Ambos declaram (idempotente no RabbitMQ; garante que o exchange exista independentemente da ordem de subida dos serviços) |
| Routing key `loan.returned` | `library-api` (produtor) / `fines-api` (binding) | Convenção compartilhada apenas por nome, sem contrato de código compartilhado |
| Fila `fines.loan-returned` | `fines-api` | Fila de consumo principal, com argumentos de DLQ |
| Fila `fines.loan-returned.dlq` | `fines-api` | Fila de destino das mensagens não processáveis |
| Binding fila ↔ exchange | `fines-api` | Vincula a fila principal ao exchange usando a routing key |
| Retry (3 tentativas) | `fines-api` (`application.properties`) | `spring.rabbitmq.listener.simple.retry.*` |

## 6. Cenários de validação

Como a apresentação prática desta entrega foi cancelada pelo professor da disciplina, os cenários abaixo foram validados manualmente em ambiente local e documentados através de evidências (logs e capturas de tela do RabbitMQ Management UI e das respostas das APIs).

### 6.1 Cenário 1 — Fluxo normal (produtor e consumidor disponíveis)

Uma devolução de empréstimo é realizada com ambos os serviços no ar. O evento é publicado por `library-api`, consumido por `fines-api`, e a multa (quando aplicável) é criada com sucesso.

![Setup da devolução](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario1-setup-devolucao.png)
*Requisição de devolução do empréstimo, disparando o fluxo de publicação do evento.*

![Log de library-api](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario1-log-library-api.png)
*Log de `library-api` confirmando a publicação do `LoanReturnedEvent` após o commit da transação.*

![Log de fines-api](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario1-log-fines-api.png)
*Log de `fines-api` confirmando o consumo do evento e o processamento da multa.*

![Lista de multas](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario1-fines-list.png)
*Multa registrada com sucesso em `fines-api`, refletindo o processamento assíncrono do evento.*

### 6.2 Cenário 2 — Consumidor indisponível (fines-api parado)

Com `fines-api` propositalmente desligado, uma devolução é realizada em `library-api`. A operação é concluída normalmente (a devolução não depende do consumidor), e o evento permanece acumulado na fila do RabbitMQ aguardando processamento. Ao subir `fines-api` novamente, a mensagem pendente é consumida e a multa correspondente é criada — demonstrando que nenhuma informação é perdida mesmo com o consumidor fora do ar.

![Devolução com fines-api parado](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario2-devolucao-fines-parado.png)
*Devolução concluída normalmente em `library-api`, mesmo com `fines-api` fora do ar.*

![Fila aguardando](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario2-fila-aguardando.png)
*RabbitMQ Management UI mostrando a mensagem acumulada na fila `fines.loan-returned`, aguardando um consumidor.*

![Log da multa processada com atraso](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario2-log-multa-atrasada.png)
*Ao subir `fines-api` novamente, a mensagem pendente é consumida e a multa é processada.*

### 6.3 Cenário 3 — Mensagem inválida (acionando retry e Dead Letter Queue)

Uma mensagem inválida (que não pode ser corretamente desserializada/processada pelo listener) é publicada diretamente no exchange. O Spring AMQP tenta reprocessá-la conforme a política de retry configurada (3 tentativas) e, após o esgotamento das tentativas, a mensagem é automaticamente roteada para a Dead Letter Queue, evidenciando o cabeçalho `x-death` que o RabbitMQ adiciona para rastrear o motivo e o histórico da rejeição.

![Publicação de mensagem inválida](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario3-publish-invalido.png)
*Mensagem inválida publicada manualmente no exchange `library.events`, via RabbitMQ Management UI.*

![Log de falha de conversão](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario3-log-conversao-invalida.png)
*Log de `fines-api` registrando a falha ao processar/converter a mensagem, disparando o mecanismo de retry.*

![Mensagem na DLQ](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario3-dlq-com-mensagem.png)
*Após o esgotamento das tentativas de retry, a mensagem aparece na fila `fines.loan-returned.dlq`.*

![Cabeçalho x-death](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario3-dlq-x-death.png)
*Detalhe do cabeçalho `x-death` da mensagem morta, evidenciando o exchange/fila de origem e o motivo da rejeição.*

### 6.4 Cenário 4 — Broker indisponível no momento da publicação

Com o RabbitMQ propositalmente desligado, uma devolução é realizada em `library-api`. A operação de devolução é concluída normalmente no banco de dados (o domínio não depende do broker), mas a tentativa de publicação do evento falha e é registrada em log — o evento é perdido, já que não há mecanismo de reenvio nesta implementação. `fines-api` nunca recebe o evento correspondente, e nenhuma multa é criada para esse empréstimo, mesmo quando ele geraria uma.

![Devolução com RabbitMQ parado](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario4-devolucao-rabbit-parado.png)
*Devolução concluída normalmente em `library-api`, mesmo com o broker RabbitMQ indisponível.*

![Log de falha na publicação](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario4-log-falha-publicacao.png)
*Log de `library-api` capturando a exceção ao tentar publicar o evento, sem interromper a operação de devolução.*

![Multa perdida](../../../../Downloads/ARQUITETURA-EVENTOS/docs/evidencias/cenario4-fines-sem-multa-perdida.png)
*`fines-api` sem registro de multa para o empréstimo devolvido nessa janela, confirmando a perda do evento.*

Esse cenário é a limitação mais relevante identificada nesta implementação, discutida na seção 8.

## 7. Testes automatizados

A cobertura de testes foi expandida em ambos os serviços para cobrir o novo fluxo assíncrono:

**`library-api`**
- `LoanServiceTest`: verifica que, ao devolver um empréstimo, o `ApplicationEventPublisher` é corretamente acionado com um `LoanReturnedEvent` contendo os dados esperados (substituindo os testes anteriores baseados em mock do Feign client, removido nesta etapa).
- `LoanEventPublisherTest`: verifica que o listener transacional publica corretamente a mensagem no `RabbitTemplate` (exchange e routing key corretos) e que uma falha na publicação é tratada sem propagar exceção.

**`fines-api`**
- `LoanReturnedListenerTest`: cobre o processamento normal de um evento válido, o comportamento idempotente (evento repetido não gera multa duplicada) e a propagação de exceção quando a mensagem não pode ser processada (necessária para acionar o retry/DLQ).
- `FineRepositoryTest`: cobre o método `existsByLoanId` e a violação da constraint `UNIQUE` da coluna `loan_id` ao tentar persistir duas multas para o mesmo empréstimo.

## 8. Limitações conhecidas e evolução futura

A principal limitação identificada nesta implementação é evidenciada no **Cenário 4**: caso o broker RabbitMQ esteja indisponível no exato momento em que a transação de devolução é confirmada, o evento é perdido de forma definitiva, pois a publicação acontece apenas em memória, após o commit, sem qualquer registro persistente da intenção de publicar.

A evolução recomendada para essa limitação é o **Transactional Outbox Pattern**: em vez de publicar diretamente no broker após o commit, o evento seria persistido em uma tabela auxiliar (`outbox`) **dentro da mesma transação** que persiste a devolução do empréstimo, garantindo atomicidade entre os dois. Um processo separado (um poller ou um mecanismo de *Change Data Capture*, como o Debezium) ficaria responsável por ler essa tabela e publicar os eventos pendentes no RabbitMQ, com re-tentativa automática em caso de falha do broker — eliminando a janela de perda observada no Cenário 4.

Outros pontos identificados como possíveis evoluções futuras, mas fora do escopo desta etapa (por decisão consciente de manter a implementação simples):

- Uso de `ddl-auto=update` não retroage constraints (como `UNIQUE`) em tabelas já existentes — uma ferramenta de migração de schema (Flyway ou Liquibase) resolveria esse problema de forma mais robusta e rastreável do que a alteração manual realizada nesta etapa.
- Não há monitoramento/alertas configurados para o crescimento da Dead Letter Queue — em um cenário real, mensagens acumuladas na DLQ deveriam gerar algum tipo de alerta operacional.
- O binding entre fila e exchange está fixado a uma única routing key (`loan.returned`); a evolução para múltiplos tipos de eventos do domínio de empréstimos exigiria decidir entre múltiplas routing keys específicas ou um padrão de roteamento mais genérico (wildcard).

## 9. Infraestrutura local

Toda a infraestrutura necessária para rodar o projeto localmente (RabbitMQ e os bancos Postgres de cada serviço) é orquestrada via Docker Compose, a partir do arquivo `docker-compose.yml` na raiz do repositório:

```bash
# Subir toda a infraestrutura (RabbitMQ + bancos)
docker compose up -d

# Verificar os containers em execução
docker compose ps

# Derrubar a infraestrutura (mantendo os volumes/dados)
docker compose down
```

Após subir a infraestrutura, o RabbitMQ Management UI fica disponível em `http://localhost:15672` (usuário/senha padrão: `guest`/`guest`), permitindo inspecionar exchanges, filas, bindings e mensagens — usado extensivamente para a validação dos cenários descritos na seção 6.

Com a infraestrutura no ar, os serviços (`discovery-server`, `library-api`, `fines-api`) podem ser iniciados normalmente pela IDE ou via `./mvnw spring-boot:run` em cada módulo.
