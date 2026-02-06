package com.akandiah.propmanager.features.prop.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest;
import com.akandiah.propmanager.features.prop.api.dto.PropResponse;
import com.akandiah.propmanager.features.prop.api.dto.UpdatePropRequest;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;

@Service
public class PropService {

	private final PropRepository repository;

	public PropService(PropRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<PropResponse> findAll() {
		return repository.findAll().stream()
				.map(PropResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PropResponse findById(UUID id) {
		Prop prop = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Prop", id));
		return PropResponse.from(prop);
	}

	@Transactional
	public PropResponse create(CreatePropRequest request) {
		Prop prop = Prop.builder()
				.name(request.name())
				.description(request.description())
				.build();
		prop = repository.save(prop);
		return PropResponse.from(prop);
	}

	@Transactional
	public PropResponse update(UUID id, UpdatePropRequest request) {
		Prop prop = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Prop", id));
		if (request.name() != null)
			prop.setName(request.name());
		if (request.description() != null)
			prop.setDescription(request.description());
		prop = repository.save(prop);
		return PropResponse.from(prop);
	}

	@Transactional
	public void deleteById(UUID id) {
		if (!repository.existsById(id))
			throw new ResourceNotFoundException("Prop", id);
		repository.deleteById(id);
	}
}
