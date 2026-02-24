package com.akandiah.propmanager.features.membership.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.akandiah.propmanager.common.permission.ResourceType;

public interface MemberScopeRepository extends JpaRepository<MemberScope, UUID> {

	List<MemberScope> findByMembershipId(UUID membershipId);

	List<MemberScope> findByMembershipIdIn(Collection<UUID> membershipIds);

	List<MemberScope> findByMembershipIdAndScopeType(UUID membershipId, ResourceType scopeType);

	List<MemberScope> findByScopeTypeAndScopeId(ResourceType scopeType, UUID scopeId);

	@Modifying
	void deleteByMembershipId(UUID membershipId);

	@Modifying
	void deleteByScopeTypeAndScopeId(ResourceType scopeType, UUID scopeId);

	boolean existsByIdAndMembershipId(UUID id, UUID membershipId);

	Optional<MemberScope> findByIdAndMembershipId(UUID id, UUID membershipId);
}
