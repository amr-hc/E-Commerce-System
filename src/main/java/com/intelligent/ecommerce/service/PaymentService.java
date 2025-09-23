package com.intelligent.ecommerce.service;

import com.intelligent.ecommerce.entity.Payment;
import com.intelligent.ecommerce.exception.PaymentFailedException;
import com.intelligent.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment processPayment(Payment payment) {
        if ("FAIL".equalsIgnoreCase(payment.getStatus())) {
            throw new PaymentFailedException("Payment failed for method " + payment.getPaymentMethod());
        }
        return paymentRepository.save(payment);
    }
}
