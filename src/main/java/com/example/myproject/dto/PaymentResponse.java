package com.example.myproject.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private Boolean success;
    private String message;
    private String orderNumber;
    private Long orderId;
    private String paymentStatus;
    private String orderStatus;
    
    public static PaymentResponse success(String orderNumber, Long orderId, String message) {
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setOrderNumber(orderNumber);
        response.setOrderId(orderId);
        return response;
    }
    
    public static PaymentResponse failure(String message) {
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}