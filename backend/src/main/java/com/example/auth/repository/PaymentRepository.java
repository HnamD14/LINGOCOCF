package com.example.auth.repository;

import com.example.auth.model.Payment;
import com.example.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderCode(String orderCode);
    Optional<Payment> findByTransactionCode(String txnCode);
    List<Payment> findByUserOrderByCreatedAtDesc(User user);
    List<Payment> findByStatusOrderByCreatedAtDesc(Payment.Status status);
    boolean existsByOrderCode(String orderCode);
}
