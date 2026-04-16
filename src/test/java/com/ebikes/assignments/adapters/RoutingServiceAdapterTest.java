package com.ebikes.assignments.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.ebikes.assignments.adapters.routing.RoutingServiceAdapter;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.entities.Shortlist;
import com.ebikes.assignments.dtos.internal.MatrixEntry;
import com.ebikes.assignments.dtos.internal.MatrixResponse;
import com.ebikes.assignments.dtos.responses.api.SuccessResponse;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.exceptions.ExternalServiceException;
import com.ebikes.assignments.support.fixtures.AssignmentFixtures;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;
import com.ebikes.assignments.support.fixtures.ShortlistFixtures;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("RoutingServiceAdapter")
class RoutingServiceAdapterTest {

  private static final String BASE_URL = "http://routing-service";
  private static final String MATRICES_PATH = "/matrices";

  private MockRestServiceServer server;
  private RoutingServiceAdapter adapter;
  private ObjectMapper objectMapper;

  private OrderContext orderContext;
  private List<Shortlist> candidates;

  @BeforeEach
  void setUp() {
    objectMapper = JsonMapper.builder().build();

    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    server = MockRestServiceServer.bindTo(builder).build();
    adapter = new RoutingServiceAdapter(builder.build());

    orderContext = OrderContextFixtures.standard();
    Assignment assignment =
        AssignmentFixtures.withStrategy(orderContext, AssignmentStrategy.RANKED);
    candidates =
        List.of(ShortlistFixtures.unranked(assignment), ShortlistFixtures.unranked(assignment));
  }

  @Nested
  @DisplayName("computeMatrix")
  class ComputeMatrix {

    @Test
    @DisplayName("should return matrix response on success")
    void shouldReturnMatrixResponseOnSuccess() {
      MatrixResponse expected =
          new MatrixResponse(
              List.of(new MatrixEntry("agent-001", 500, 120, false)),
              new BigDecimal("51.509865"),
              new BigDecimal("-0.118092"));

      String body =
          objectMapper.writeValueAsString(new SuccessResponse<>("SUCCESS", expected, null));

      server
          .expect(requestTo(BASE_URL + MATRICES_PATH))
          .andExpect(method(HttpMethod.POST))
          .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

      MatrixResponse result = adapter.computeMatrix(orderContext, candidates);

      assertThat(result.entries()).hasSize(1);
      assertThat(result.entries().getFirst().agentId()).isEqualTo("agent-001");
    }

    @Test
    @DisplayName("should throw ExternalServiceException when response body is null")
    void shouldThrowWhenResponseBodyIsNull() {
      server
          .expect(requestTo(BASE_URL + MATRICES_PATH))
          .andExpect(method(HttpMethod.POST))
          .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

      assertThatThrownBy(() -> adapter.computeMatrix(orderContext, candidates))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should throw ExternalServiceException when response data is null")
    void shouldThrowWhenResponseDataIsNull() {
      String body = objectMapper.writeValueAsString(new SuccessResponse<>("SUCCESS", null, null));

      server
          .expect(requestTo(BASE_URL + MATRICES_PATH))
          .andExpect(method(HttpMethod.POST))
          .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

      assertThatThrownBy(() -> adapter.computeMatrix(orderContext, candidates))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should throw ExternalServiceException on 5xx error")
    void shouldThrowExternalServiceExceptionOnServerError() {
      server
          .expect(requestTo(BASE_URL + MATRICES_PATH))
          .andExpect(method(HttpMethod.POST))
          .andRespond(withServerError());

      assertThatThrownBy(() -> adapter.computeMatrix(orderContext, candidates))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should throw ExternalServiceException on 4xx error")
    void shouldThrowExternalServiceExceptionOnClientError() {
      server
          .expect(requestTo(BASE_URL + MATRICES_PATH))
          .andExpect(method(HttpMethod.POST))
          .andRespond(withStatus(HttpStatus.BAD_REQUEST));

      assertThatThrownBy(() -> adapter.computeMatrix(orderContext, candidates))
          .isInstanceOf(ExternalServiceException.class);
    }
  }
}
