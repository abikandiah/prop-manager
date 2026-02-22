package com.akandiah.propmanager.features.organization.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.PermissionStringValidator;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.organization.api.dto.CreateMembershipRequest;
import com.akandiah.propmanager.features.organization.api.dto.MembershipResponse;
import com.akandiah.propmanager.features.organization.api.dto.UpdateMembershipRequest;
import com.akandiah.propmanager.features.organization.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.permission.domain.PermissionTemplate;
import com.akandiah.propmanager.features.permission.domain.PermissionTemplateRepository;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService {

	private final MembershipRepository membershipRepository;
	private final MemberScopeRepository memberScopeRepository;
	private final OrganizationRepository organizationRepository;
	private final UserRepository userRepository;
	private final PermissionTemplateRepository permissionTemplateRepository;

	public List<MembershipResponse> findByOrganizationId(UUID organizationId) {
		return membershipRepository.findByOrganizationIdWithUserAndOrg(organizationId).stream()
				.map(MembershipResponse::from)
				.toList();
	}

	public List<MembershipResponse> findByUserId(UUID userId) {
		return membershipRepository.findByUserIdWithUserAndOrg(userId).stream()
				.map(MembershipResponse::from)
				.toList();
	}

	public MembershipResponse findById(UUID id) {
		Membership m = membershipRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", id));
		return MembershipResponse.from(m);
	}

	@Transactional
	public MembershipResponse create(UUID organizationId, CreateMembershipRequest request) {
		Organization org = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
		User user = userRepository.findById(request.userId())
				.orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

		Map<String, String> permissions = resolvePermissionsForCreate(
				organizationId, request.permissions(), request.templateId());
		if (permissions != null) {
			PermissionStringValidator.validate(permissions);
		}

		// Duplicate (user, org) is rejected by uk_memberships_user_org → DataIntegrityViolationException → 409
		Membership m = Membership.builder()
				.user(user)
				.organization(org)
				.role(request.role())
				.permissions(permissions)
				.build();
		m = membershipRepository.save(m);
		return MembershipResponse.from(m);
	}

	@Transactional
	public MembershipResponse update(UUID id, UpdateMembershipRequest request) {
		Membership m = membershipRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", id));
		OptimisticLockingUtil.requireVersionMatch("Membership", id, m.getVersion(), request.version());
		m.setRole(request.role());
		if (request.permissions() != null) {
			PermissionStringValidator.validate(request.permissions());
			m.setPermissions(request.permissions());
		}
		m = membershipRepository.save(m);
		return MembershipResponse.from(m);
	}

	@Transactional
	public void deleteById(UUID id) {
		Membership m = membershipRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", id));
		memberScopeRepository.deleteByMembershipId(id);
		membershipRepository.delete(m);
	}

	/**
	 * Resolve permissions for create: request.permissions if non-null; else copy from template if templateId present; else null.
	 */
	private Map<String, String> resolvePermissionsForCreate(UUID orgId, Map<String, String> requestPermissions, UUID templateId) {
		if (requestPermissions != null) {
			return requestPermissions;
		}
		if (templateId != null) {
			PermissionTemplate template = permissionTemplateRepository.findById(templateId)
					.orElseThrow(() -> new ResourceNotFoundException("PermissionTemplate", templateId));
			if (template.getOrg() != null && !template.getOrg().getId().equals(orgId)) {
				throw new IllegalArgumentException("Permission template does not belong to this organization");
			}
			Map<String, String> defaultPerms = template.getDefaultPermissions();
			return defaultPerms == null || defaultPerms.isEmpty()
					? null
					: new HashMap<>(defaultPerms);
		}
		return null;
	}
}
