package com.akandiah.propmanager.features.permission.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionStringValidator;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.common.util.SecurityUtils;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.permission.api.dto.CreatePermissionTemplateRequest;
import com.akandiah.propmanager.features.permission.api.dto.PermissionTemplateResponse;
import com.akandiah.propmanager.features.permission.api.dto.UpdatePermissionTemplateRequest;
import com.akandiah.propmanager.features.permission.domain.PermissionTemplate;
import com.akandiah.propmanager.features.permission.domain.PermissionTemplateRepository;
import com.akandiah.propmanager.security.OrgGuard;
import com.akandiah.propmanager.security.PermissionGuard;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PermissionTemplateService {

	private final PermissionTemplateRepository repository;
	private final OrganizationRepository organizationRepository;
	private final OrgGuard orgGuard;
	private final PermissionGuard permissionGuard;

	public List<PermissionTemplateResponse> listByOrg(UUID orgId) {
		return repository.findByOrgIsNullOrOrg_IdOrderByNameAsc(orgId).stream()
				.map(PermissionTemplateResponse::from)
				.toList();
	}

	public PermissionTemplateResponse findById(UUID id) {
		PermissionTemplate template = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("PermissionTemplate", id));

		if (template.getOrg() != null) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (!orgGuard.isMember(template.getOrg().getId(), auth)) {
				throw new AccessDeniedException("Access denied to this organization's templates");
			}
		}

		return PermissionTemplateResponse.from(template);
	}

	@Transactional
	public PermissionTemplateResponse create(CreatePermissionTemplateRequest request) {
		if (request.orgId() == null && !SecurityUtils.isGlobalAdmin()) {
			throw new AccessDeniedException("Only system administrators can create system templates");
		}

		PermissionStringValidator.validate(request.defaultPermissions());

		Organization org = null;
		if (request.orgId() != null) {
			org = organizationRepository.findById(request.orgId())
					.orElseThrow(() -> new ResourceNotFoundException("Organization", request.orgId()));
		}

		PermissionTemplate template = PermissionTemplate.builder()
				.org(org)
				.name(request.name())
				.defaultPermissions(request.defaultPermissions())
				.build();
		template = repository.save(template);
		return PermissionTemplateResponse.from(template);
	}

	@Transactional
	public PermissionTemplateResponse update(UUID id, UpdatePermissionTemplateRequest request) {
		PermissionTemplate template = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("PermissionTemplate", id));

		if (template.getOrg() == null) {
			if (!SecurityUtils.isGlobalAdmin()) {
				throw new AccessDeniedException("Only system administrators can modify system templates");
			}
		} else {
			UUID orgId = template.getOrg().getId();
			if (!SecurityUtils.isGlobalAdmin() &&
					!permissionGuard.hasAccess(Actions.UPDATE, "o", ResourceType.ORG, orgId, orgId)) {
				throw new AccessDeniedException("Insufficient permissions to modify this organization's templates");
			}
		}

		OptimisticLockingUtil.requireVersionMatch("PermissionTemplate", id, template.getVersion(), request.version());

		if (request.defaultPermissions() != null) {
			PermissionStringValidator.validate(request.defaultPermissions());
			template.setDefaultPermissions(request.defaultPermissions());
		}
		if (request.name() != null) {
			template.setName(request.name());
		}

		template = repository.save(template);
		return PermissionTemplateResponse.from(template);
	}

	@Transactional
	public void deleteById(UUID id) {
		PermissionTemplate template = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("PermissionTemplate", id));

		if (template.getOrg() == null) {
			if (!SecurityUtils.isGlobalAdmin()) {
				throw new AccessDeniedException("Only system administrators can delete system templates");
			}
		} else {
			UUID orgId = template.getOrg().getId();
			if (!SecurityUtils.isGlobalAdmin() &&
					!permissionGuard.hasAccess(Actions.DELETE, "o", ResourceType.ORG, orgId, orgId)) {
				throw new AccessDeniedException("Insufficient permissions to delete this organization's templates");
			}
		}

		repository.delete(template);
	}
}
