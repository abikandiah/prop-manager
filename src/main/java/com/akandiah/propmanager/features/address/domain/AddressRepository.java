package com.akandiah.propmanager.features.address.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, UUID> {
}
