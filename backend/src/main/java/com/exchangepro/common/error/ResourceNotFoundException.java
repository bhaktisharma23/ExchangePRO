package com.exchangepro.common.error;

public class ResourceNotFoundException extends ExchangeProException {

    public ResourceNotFoundException(String resource, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resource + " not found: " + id);
    }
}
