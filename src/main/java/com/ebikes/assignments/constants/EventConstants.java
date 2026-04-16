package com.ebikes.assignments.constants;

import com.ebikes.assignments.support.references.ReferenceGenerator;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EventConstants {

  @UtilityClass
  public static final class Source {
    public static final String HOST_SERVICE = "assignments";
    public static String serviceReference() {
      return ReferenceGenerator.generateServiceReference(HOST_SERVICE);
    }
  }

  @UtilityClass
  public static final class DomainEvents {

    @UtilityClass
    public static final class Assignments {
      public static final String CANCELLED = Source.HOST_SERVICE + ".assignment.cancelled";
      public static final String FAILED = Source.HOST_SERVICE + ".assignment.failed";
      public static final String SUCCEEDED = Source.HOST_SERVICE + ".assignment.succeeded";
    }

    @UtilityClass
    public static final class Offers {
      public static final String ACCEPTED = Source.HOST_SERVICE + ".offer.accepted";
      public static final String CANCELLED = Source.HOST_SERVICE + ".offer.cancelled";
      public static final String CREATED = Source.HOST_SERVICE + ".offer.created";
      public static final String DECLINED = Source.HOST_SERVICE + ".offer.declined";
    }
  }

  @UtilityClass
  public static final class ExternalContracts {
    public static final String ORDERS_ORDER_ENTERED_PENDING_ASSIGNMENT =
        "orders.order.entered_pending_assignment";
    public static final String ORDERS_ORDER_REASSIGNMENT_REQUESTED =
        "orders.order.reassignment_requested";
    public static final String WORKFORCE_AGENT_SHORTLIST_RESOLVED =
        "workforce.agent_shortlist.resolved";
  }

  @UtilityClass
  public static final class RoutingKeys {

    // outbound — domain event assignments keys
    public static final String ASSIGNMENT_CANCELLED = DomainEvents.Assignments.CANCELLED;
    public static final String ASSIGNMENT_FAILED = DomainEvents.Assignments.FAILED;
    public static final String ASSIGNMENT_SUCCEEDED = DomainEvents.Assignments.SUCCEEDED;
    public static final String ASSIGNMENT_OFFER_ACCEPTED = DomainEvents.Offers.ACCEPTED;
    public static final String ASSIGNMENT_OFFER_CANCELLED = DomainEvents.Offers.CANCELLED;
    public static final String ASSIGNMENT_OFFER_CREATED = DomainEvents.Offers.CREATED;
    public static final String ASSIGNMENT_OFFER_DECLINED = DomainEvents.Offers.DECLINED;

    public static String audit(String domain) {
      return domain + ".audit";
    }
  }
}
