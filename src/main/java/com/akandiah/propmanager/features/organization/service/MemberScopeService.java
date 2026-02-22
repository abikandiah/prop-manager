package com.akandiah.propmanager.features.organization.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.PermissionStringValidator;
import com.akandiah.propmanager.features.organization.api.dto.CreateMemberScopeRequest;
import com.akandiah.propmanager.features.organization.api.dto.MemberScopeResponse;
import com.akandiah.propmanager.features.organization.domain.MemberScope;
import com.akandiah.propmanager.features.organization.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.MembershipRepository;
import com.akandiah.propmanager.features.permission.domain.PermissionTemplate;
import com.akandiah.propmanager.features.permission.domain.PermissionTemplateRepository;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberScopeService {

	private final MemberScopeRepository memberScopeRepository;
	private final MembershipRepository membershipRepository;
	private final PermissionTemplateRepository permissionTemplateRepository;
	private final PropRepository propRepository;
	private final UnitRepository unitRepository;

	public List<MemberScopeResponse> findByMembershipId(UUID membershipId) {
		return memberScopeRepository.findByMembershipId(membershipId).stream()
				.map(s -> MemberScopeResponse.from(s, membershipId))
				.toList();
	}

	@Transactional
	public MemberScopeResponse create(UUID membershipId, CreateMemberScopeRequest request) {
		Membership membership = membershipRepository.findById(membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));

		UUID orgId = membership.getOrganization().getId();
		validateScopeBelongsToOrg(request, orgId);

		Map<String, String> permissions = resolvePermissionsForCreate(
				orgId, request.permissions(), request.templateId());
		PermissionStringValidator.validate(permissions);

		MemberScope scope = MemberScope.builder()
				.membership(membership)
				.scopeType(request.scopeType())
				.scopeId(request.scopeId())
				.permissions(permissions)
				.build();
		scope = memberScopeRepository.save(scope);
		return MemberScopeResponse.from(scope);
	}

	@Transactional
	public void deleteById(UUID membershipId, UUID scopeId) {
		if (!memberScopeRepository.existsByIdAndMembershipId(scopeId, membershipId)) {
			throw new ResourceNotFoundException("MemberScope", scopeId);
		}
		memberScopeRepository.deleteById(scopeId);
	}

	private void validateScopeBelongsToOrg(CreateMemberScopeRequest request, UUID orgId) {
		boolean valid = switch (request.scopeType()) {
			case PROPERTY -> propRepository.existsByIdAndOrganization_Id(request.scopeId(), orgId);
			case UNIT -> unitRepository.existsByIdAndProp_Organization_Id(request.scopeId(), orgId);
		};
		if (!valid) {
			throw new ResourceNotFoundException(request.scopeType().name(), request.scopeId());
		}
	}

	/**
	 * Resolve permissions for create: request.permissions if non-null; else copy from template if templateId present; else empty map.
	 */
	private Map<String, String> resolvePermissionsForCreate(UUID orgId, Map<String, String> requestPermissions, UUID templateId) {
		if (requestPermissions != null && !requestPermissions.isEmpty()) {
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
					? Collections.emptyMap()
					: new HashMap<>(defaultPerms);
		}
		return Collections.emptyMap();
	}
}
