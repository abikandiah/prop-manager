package com.akandiah.propmanager.features.activity.domain;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {

	Page<ActivityEvent> findByOrgId(UUID orgId, Pageable pageable);

	Page<ActivityEvent> findByOrgIdAndEventTypeIn(UUID orgId, Collection<ActivityEventType> eventTypes,
			Pageable pageable);
}
