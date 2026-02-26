package com.akandiah.propmanager.features.membership.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
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
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.membership.api.dto.CreateMembershipTemplateRequest;
import com.akandiah.propmanager.features.membership.api.dto.MembershipTemplateResponse;
import com.akandiah.propmanager.features.membership.api.dto.UpdateMembershipTemplateRequest;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplate;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplateRepository;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.security.OrgGuard;
import com.akandiah.propmanager.security.PermissionGuard;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MembershipTemplateService {

	private final MembershipTemplateRepository repository;
	private final OrganizationRepository organizationRepository;
	private final MembershipRepository membershipRepository;
	private final OrgGuard orgGuard;
	private final PermissionGuard permissionGuard;
	private final ApplicationEventPublisher eventPublisher;

	public List<MembershipTemplateResponse> listByOrg(UUID orgId) {
		return repository.findByOrgIsNullOrOrg_IdOrderByNameAsc(orgId).stream()
				.map(MembershipTemplateResponse::from)
				.toList();
	}

	public MembershipTemplateResponse findById(UUID id) {
		MembershipTemplate template = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("MembershipTemplate", id));

		if (template.getOrg() != null) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (!orgGuard.isMember(template.getOrg().getId(), auth)) {
				throw new AccessDeniedException("Access denied to this organization's templates");
			}
		}

		return MembershipTemplateResponse.from(template);
	}

	@Transactional
	public MembershipTemplateResponse create(CreateMembershipTemplateRequest request) {
		if (request.orgId() == null && !SecurityUtils.isGlobalAdmin()) {
			throw new AccessDeniedException("Only system administrators can create system templates");
		}

		validateItems(request.items());

		Organization org = null;
		if (request.orgId() != null) {
			org = organizationRepository.findById(request.orgId())
					.orElseThrow(() -> new ResourceNotFoundException("Organization", request.orgId()));
		}

		MembershipTemplate template = MembershipTemplate.builder()
				.org(org)
				.name(request.name())
				.items(request.items().stream()
						.map(i -> i.toEntity())
						.toList())
				.build();
		template = repository.save(template);
		return MembershipTemplateResponse.from(template);
	}

	@Transactional
	public MembershipTemplateResponse update(UUID id, UpdateMembershipTemplateRequest request) {
		MembershipTemplate template = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("MembershipTemplate", id));

		requireUpdateAccess(template);
		OptimisticLockingUtil.requireVersionMatch("MembershipTemplate", id, template.getVersion(), request.version());

		boolean itemsChanged = false;
		if (request.name() != null) {
			template.setName(request.name());
		}
		if (request.items() != null) {
			validateItems(request.items());
			template.getItems().clear();
			template.getItems().addAll(request.items().stream().map(i -> i.toEntity()).toList());
			itemsChanged = true;
		}

		template = repository.save(template);

		if (itemsChanged) {
			evictLinkedMemberships(id);
		}

		return MembershipTemplateResponse.from(template);
	}

	@Transactional
	public void deleteById(UUID id) {
		MembershipTemplate template = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("MembershipTemplate", id));

		requireDeleteAccess(template);

		evictLinkedMemberships(id);
		repository.delete(template);
	}

	// --- helpers ---

	private void validateItems(List<com.akandiah.propmanager.features.membership.api.dto.MembershipTemplateItemView> items) {
		for (var item : items) {
			PermissionStringValidator.validate(item.permissions());
		}
	}

	private void requireUpdateAccess(MembershipTemplate template) {
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
	}

	private void requireDeleteAccess(MembershipTemplate template) {
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
	}

	/**
	 * Evicts the permissions cache for all users whose memberships reference this template.
	 * Called when template items are changed or the template is deleted.
	 */
	private void evictLinkedMemberships(UUID templateId) {
		List<Membership> linked = membershipRepository.findByMembershipTemplateIdAndUserIsNotNull(templateId);
		if (linked.isEmpty()) {
			return;
		}
		Set<UUID> userIds = linked.stream()
				.map(m -> m.getUser().getId())
				.collect(Collectors.toSet());
		eventPublisher.publishEvent(new PermissionsChangedEvent(userIds));
	}
}
