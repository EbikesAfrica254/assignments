package com.ebikes.assignments.constants;

import com.ebikes.assignments.support.references.ReferenceGenerator;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EventConstants {

  public static final class EventSource {

    private EventSource() {
      // prevent instantiation
    }

    public static final String HOST_SERVICE = "assignments";

    public static String serviceReference() {
      return ReferenceGenerator.generateServiceReference(HOST_SERVICE);
    }
  }

  public static final class EventTypes {

    private EventTypes() {
      // prevent instantiation
    }

    public static final class Assignments {

      private Assignments() {
        // prevent instantiation
      }

      public static final String CANCELLED = EventSource.HOST_SERVICE + ".assignment.cancelled";
      public static final String FAILED = EventSource.HOST_SERVICE + ".assignment.failed";
      public static final String STARTED = EventSource.HOST_SERVICE + ".assignment.started";
      public static final String SUCCEEDED = EventSource.HOST_SERVICE + ".assignment.succeeded";
    }

    public static final class AssignmentOffers {

      private AssignmentOffers() {
        // prevent instantiation
      }

      public static final String ACCEPTED = EventSource.HOST_SERVICE + ".assignment_offer.accepted";
      public static final String CANCELLED =
          EventSource.HOST_SERVICE + ".assignment_offer.cancelled";
      public static final String CREATED = EventSource.HOST_SERVICE + ".assignment_offer.created";
      public static final String DECLINED = EventSource.HOST_SERVICE + ".assignment_offer.declined";
      public static final String EXPIRED = EventSource.HOST_SERVICE + ".assignment_offer.expired";
    }
  }

  public static final class RoutingKeys {

    private RoutingKeys() {
      // prevent instantiation
    }

    // inbound — consumed event assignments keys
    public static final String ORDERS_ORDER_ENTERED_PENDING_ASSIGNMENT =
        "orders.order.entered_pending_assignment";
    public static final String ORDERS_ORDER_REASSIGNMENT_REQUESTED =
        "orders.order.reassignment_requested";
    public static final String WORKFORCE_AGENT_SHORTLIST_RESOLVED =
        "workforce.agent_shortlist.resolved";

    // outbound — audit assignments keys
    public static final String ASSIGNMENT_AUDIT = audit(EventSource.HOST_SERVICE + ".assignment");
    public static final String ASSIGNMENT_OFFER_AUDIT =
        audit(EventSource.HOST_SERVICE + ".assignment-offer");

    // outbound — domain event assignments keys
    public static final String ASSIGNMENT_CANCELLED = EventTypes.Assignments.CANCELLED;
    public static final String ASSIGNMENT_FAILED = EventTypes.Assignments.FAILED;
    public static final String ASSIGNMENT_STARTED = EventTypes.Assignments.STARTED;
    public static final String ASSIGNMENT_SUCCEEDED = EventTypes.Assignments.SUCCEEDED;
    public static final String ASSIGNMENT_OFFER_ACCEPTED = EventTypes.AssignmentOffers.ACCEPTED;
    public static final String ASSIGNMENT_OFFER_CANCELLED = EventTypes.AssignmentOffers.CANCELLED;
    public static final String ASSIGNMENT_OFFER_CREATED = EventTypes.AssignmentOffers.CREATED;
    public static final String ASSIGNMENT_OFFER_DECLINED = EventTypes.AssignmentOffers.DECLINED;
    public static final String ASSIGNMENT_OFFER_EXPIRED = EventTypes.AssignmentOffers.EXPIRED;

    public static String audit(String domain) {
      return domain + ".audit";
    }
  }

  public static final class MessageHeaders {

    private MessageHeaders() {
      // prevent instantiation
    }

    public static final String EVENT_TYPE = "eventType";
    public static final String OUTBOX_ID = "outboxId";
    public static final String ROUTING_KEY = "assignmentsKey";
  }
}
