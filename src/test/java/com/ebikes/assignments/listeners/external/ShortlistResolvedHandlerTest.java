package com.ebikes.assignments.listeners.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.ebikes.assignments.configurations.properties.ServiceProperties;
import com.ebikes.assignments.constants.EventConstants.ExternalContracts;
import com.ebikes.assignments.dtos.events.incoming.AgentShortlistResolvedEvent;
import com.ebikes.assignments.services.assignments.OrchestrationService;
import com.ebikes.assignments.services.events.InboxService;
import com.ebikes.assignments.support.context.EventContext;
import com.ebikes.assignments.support.context.ExecutionContext;
import com.ebikes.assignments.support.fixtures.AgentShortlistResolvedEventFixtures;
import com.ebikes.assignments.support.fixtures.CandidateFixtures;
import com.ebikes.assignments.support.infrastructure.AbstractListenerTest;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("ShortlistResolvedHandler")
class ShortlistResolvedHandlerTest extends AbstractListenerTest {

  @Mock private InboxService inboxService;
  @Mock private OrchestrationService orchestrationService;
  @Mock private ServiceProperties serviceProperties;

  private final JsonMapper objectMapper = JsonMapper.builder().build();
  private ShortlistResolvedHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new ShortlistResolvedHandler(
            inboxService, objectMapper, orchestrationService, serviceProperties);
  }

  @Nested
  @DisplayName("handle")
  class Handle {

    @Test
    @DisplayName("should skip when event context is absent")
    void shouldSkipWhenEventContextIsAbsent() {
      AgentShortlistResolvedEvent event =
          AgentShortlistResolvedEventFixtures.standard(List.of(CandidateFixtures.standard()));
      byte[] payload = objectMapper.writeValueAsBytes(event);

      handler.handle(payload);

      verify(inboxService, never()).receive(any(), any(), any());
    }

    @Test
    @DisplayName("should skip on duplicate event")
    void shouldSkipOnDuplicateEvent() {
      AgentShortlistResolvedEvent event =
          AgentShortlistResolvedEventFixtures.standard(List.of(CandidateFixtures.standard()));
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(false);

      handler.handle(payload);

      verify(orchestrationService, never()).start(any(AgentShortlistResolvedEvent.class));
      verify(inboxService, never()).markProcessed(any());
    }

    @Test
    @DisplayName("should reject when candidates are empty")
    void shouldRejectWhenCandidatesAreEmpty() {
      AgentShortlistResolvedEvent event = AgentShortlistResolvedEventFixtures.emptyCandidates();
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      handler.handle(payload);

      verify(orchestrationService, never()).start(any(AgentShortlistResolvedEvent.class));
    }

    @Test
    @DisplayName("should reject when shortlist is expired")
    void shouldRejectWhenShortlistIsExpired() {
      AgentShortlistResolvedEvent event =
          AgentShortlistResolvedEventFixtures.expired(List.of(CandidateFixtures.standard()));
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      handler.handle(payload);

      verify(orchestrationService, never()).start(any(AgentShortlistResolvedEvent.class));
    }

    @Test
    @DisplayName("should reject when candidates exceed shortlist max")
    void shouldRejectWhenCandidatesExceedShortlistMax() {
      AgentShortlistResolvedEvent event =
          AgentShortlistResolvedEventFixtures.standard(
              List.of(CandidateFixtures.standard(), CandidateFixtures.standard()));
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(true);
      ServiceProperties.Candidates candidates = new ServiceProperties.Candidates();
      candidates.setShortlistMax(1);
      when(serviceProperties.getCandidates()).thenReturn(candidates);

      handler.handle(payload);

      verify(orchestrationService, never()).start(any(AgentShortlistResolvedEvent.class));
    }

    @Test
    @DisplayName("should process event and mark processed")
    void shouldProcessEventAndMarkProcessed() {
      AgentShortlistResolvedEvent event =
          AgentShortlistResolvedEventFixtures.standard(List.of(CandidateFixtures.standard()));
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(true);
      ServiceProperties.Candidates candidates = new ServiceProperties.Candidates();
      candidates.setShortlistMax(10);
      when(serviceProperties.getCandidates()).thenReturn(candidates);

      handler.handle(payload);

      verify(orchestrationService).start(event);
      verify(inboxService).markProcessed(event.serviceReference());
    }

    @Test
    @DisplayName("should clear execution context after processing")
    void shouldClearExecutionContextAfterProcessing() {
      AgentShortlistResolvedEvent event =
          AgentShortlistResolvedEventFixtures.standard(List.of(CandidateFixtures.standard()));
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(true);
      ServiceProperties.Candidates candidates = new ServiceProperties.Candidates();
      candidates.setShortlistMax(10);
      when(serviceProperties.getCandidates()).thenReturn(candidates);

      handler.handle(payload);

      assertThatThrownBy(ExecutionContext::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should swallow exception when process throws")
    void shouldSwallowExceptionWhenProcessThrows() {
      AgentShortlistResolvedEvent event =
          AgentShortlistResolvedEventFixtures.standard(List.of(CandidateFixtures.standard()));
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(true);
      ServiceProperties.Candidates candidates = new ServiceProperties.Candidates();
      candidates.setShortlistMax(10);
      when(serviceProperties.getCandidates()).thenReturn(candidates);
      doThrow(new RuntimeException("process failed"))
          .when(orchestrationService)
          .start(any(AgentShortlistResolvedEvent.class));

      assertThatCode(() -> handler.handle(payload)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should clear execution context when process throws")
    void shouldClearExecutionContextWhenProcessThrows() {
      AgentShortlistResolvedEvent event =
          AgentShortlistResolvedEventFixtures.standard(List.of(CandidateFixtures.standard()));
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(true);
      ServiceProperties.Candidates candidates = new ServiceProperties.Candidates();
      candidates.setShortlistMax(10);
      when(serviceProperties.getCandidates()).thenReturn(candidates);
      doThrow(new RuntimeException("process failed"))
          .when(orchestrationService)
          .start(any(AgentShortlistResolvedEvent.class));

      handler.handle(payload);

      assertThatThrownBy(ExecutionContext::get).isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("matches")
  class Matches {

    @Test
    @DisplayName("should match workforce agent shortlist resolved routing key")
    void shouldMatchWorkforceAgentShortlistResolvedRoutingKey() {
      assertThat(handler.matches(ExternalContracts.WORKFORCE_AGENT_SHORTLIST_RESOLVED)).isTrue();
    }

    @Test
    @DisplayName("should not match unrelated routing key")
    void shouldNotMatchUnrelatedRoutingKey() {
      assertThat(handler.matches("some.other.key")).isFalse();
    }
  }
}
