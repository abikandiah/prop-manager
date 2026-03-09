package com.akandiah.propmanager.features.activity.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.dto.PageResponse;
import com.akandiah.propmanager.features.activity.api.dto.ActivityEventResponse;
import com.akandiah.propmanager.features.activity.domain.ActivityEventRepository;
import com.akandiah.propmanager.features.activity.domain.ActivityEventType;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ActivityEventService {

	private final ActivityEventRepository repository;

	public PageResponse<ActivityEventResponse> findByOrgId(UUID orgId, List<ActivityEventType> eventTypes, int page,
			int size) {
		var pageable = PageRequest.of(page, size, Sort.by(Direction.DESC, "createdAt"));
		var result = (eventTypes != null && !eventTypes.isEmpty())
				? repository.findByOrgIdAndEventTypeIn(orgId, eventTypes, pageable)
				: repository.findByOrgId(orgId, pageable);
		return PageResponse.from(result.map(ActivityEventResponse::from));
	}
}
