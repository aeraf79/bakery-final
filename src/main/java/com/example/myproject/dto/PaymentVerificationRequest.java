package com.example.myproject.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationRequest {
    
    @NotBlank(message = "Razorpay order ID is required")
    private String razorpay_order_id;
    
    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpay_payment_id;
    
    @NotBlank(message = "Razorpay signature is required")
    private String razorpay_signature;
}