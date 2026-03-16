package com.example.api_gateway.dto;

import lombok.Data;

@Data
public class RateLimitResponse {

    private boolean allowed;
    private int remainingRequests;

}
