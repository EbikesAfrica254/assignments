package com.ebikes.assignments.adapters.routing;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.entities.Shortlist;
import com.ebikes.assignments.dtos.internal.MatrixAgent;
import com.ebikes.assignments.dtos.internal.MatrixRequest;
import com.ebikes.assignments.dtos.internal.MatrixResponse;
import com.ebikes.assignments.dtos.responses.api.SuccessResponse;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.exceptions.ExternalServiceException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RoutingServiceAdapter {

  private static final String MATRICES_ENDPOINT = "/matrices";

  private final RestClient restClient;

  public RoutingServiceAdapter(@Qualifier("routingServiceRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  public MatrixResponse computeMatrix(OrderContext orderContext, List<Shortlist> candidates) {

    MatrixRequest request = buildRequest(orderContext, candidates);

    log.debug(
        "Requesting matrix from assignments service: orderId={}, candidateCount={}",
        orderContext.getOrderId(),
        candidates.size());

    try {
      SuccessResponse<MatrixResponse> response =
          restClient
              .post()
              .uri(MATRICES_ENDPOINT)
              .body(request)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || response.data() == null) {
        throw new ExternalServiceException(
            MATRICES_ENDPOINT,
            "Routing service returned empty response",
            ResponseCode.EXTERNAL_SERVICE_ERROR);
      }

      log.debug(
          "Matrix received: orderId={}, resultCount={}",
          orderContext.getOrderId(),
          response.data().entries().size());

      return response.data();

    } catch (RestClientException e) {
      log.error("Routing service matrix call failed: orderId={}", orderContext.getOrderId(), e);
      throw new ExternalServiceException(
          MATRICES_ENDPOINT,
          "Failed to compute matrix from assignments service: " + e.getMessage(),
          ResponseCode.fromHttpStatus(extractStatus(e)),
          e);
    }
  }

  private MatrixRequest buildRequest(OrderContext orderContext, List<Shortlist> candidates) {
    List<MatrixAgent> agents =
        candidates.stream()
            .map(c -> new MatrixAgent(c.getAgentId(), c.getLatitude(), c.getLongitude()))
            .toList();

    return new MatrixRequest(
        agents,
        orderContext.getPickupLocation().getLatitude(),
        orderContext.getPickupLocation().getLongitude(),
        orderContext.getVehicleClass());
  }

  private int extractStatus(RestClientException e) {
    if (e instanceof HttpStatusCodeException statusEx) {
      return statusEx.getStatusCode().value();
    }
    return 500;
  }
}
