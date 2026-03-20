package com.ebikes.assignments.exceptions;

import java.io.Serial;

import com.ebikes.assignments.enums.ResponseCode;

public class AuthorizationException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public AuthorizationException(ResponseCode code, String developerMessage) {
    super(code, developerMessage);
  }
}
