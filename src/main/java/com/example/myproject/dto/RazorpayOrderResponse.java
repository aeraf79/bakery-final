package com.example.myproject.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponse {
    
    private String razorpay_order_id;
    private String razorpay_key_id;
    private Integer amount; // Amount in paise
    private String currency;
    private String order_number;
    private Long order_id;
    private BigDecimal final_amount;
    
    // Customer details for prefill
    private String customer_name;
    private String customer_email;
    private String customer_phone;
}