package com.ebikes.assignments.support.audit;

import java.util.Map;

public interface Auditable {
  Map<String, String> toAuditMetadata();
}
