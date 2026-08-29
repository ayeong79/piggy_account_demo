package com.kakaobank.piggybank.repository;

import com.kakaobank.piggybank.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
}
