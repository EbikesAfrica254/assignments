package com.ebikes.assignments.exceptions;

import java.io.Serial;

import com.ebikes.assignments.enums.ResponseCode;

public class ResourceNotFoundException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public ResourceNotFoundException(ResponseCode code, String developerMessage) {
    super(code, developerMessage);
  }

}
