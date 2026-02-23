package com.akandiah.propmanager.features.organization.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface MemberScopeRepository extends JpaRepository<MemberScope, UUID> {

	List<MemberScope> findByMembershipId(UUID membershipId);

	List<MemberScope> findByMembershipIdIn(Collection<UUID> membershipIds);

	List<MemberScope> findByMembershipIdAndScopeType(UUID membershipId, ScopeType scopeType);

	List<MemberScope> findByScopeTypeAndScopeId(ScopeType scopeType, UUID scopeId);

	@Modifying
	void deleteByMembershipId(UUID membershipId);

	boolean existsByIdAndMembershipId(UUID id, UUID membershipId);

	Optional<MemberScope> findByIdAndMembershipId(UUID id, UUID membershipId);
}
