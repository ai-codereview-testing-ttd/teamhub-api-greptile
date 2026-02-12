package com.teamhub.managers;

import com.teamhub.common.AppException;
import com.teamhub.common.ErrorCode;
import com.teamhub.models.Member;
import com.teamhub.repositories.MemberRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public class MemberManager {

    private static final Logger logger = LoggerFactory.getLogger(MemberManager.class);

    private final MemberRepository memberRepository;
    private final BillingManager billingManager;

    public MemberManager(MemberRepository memberRepository, BillingManager billingManager) {
        this.memberRepository = memberRepository;
        this.billingManager = billingManager;
    }

    public Future<Member> inviteMember(JsonObject body, String organizationId, String invitedByUserId) {
        String email = body.getString("email");
        String name = body.getString("name", "");
        String roleStr = body.getString("role", Member.Role.MEMBER.name());

        Member.Role role;
        try {
            role = Member.Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            return Future.failedFuture(new AppException(ErrorCode.VALIDATION_ERROR, "Invalid role: " + roleStr));
        }

        if (role == Member.Role.OWNER) {
            return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN, "Cannot invite a member as OWNER"));
        }

        // Check if member already exists in org
        return memberRepository.findByEmail(email, organizationId).compose(existing -> {
            if (existing != null) {
                return Future.failedFuture(new AppException(ErrorCode.CONFLICT, "Member already exists in this organization"));
            }

            // Check member limit per billing plan
            return billingManager.getCurrentPlan(organizationId).compose(plan ->
                    memberRepository.countByOrganization(organizationId).compose(count -> {
                        if (count >= plan.getMaxMembers()) {
                            return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN,
                                    "Member limit reached for current billing plan. Max: " + plan.getMaxMembers()));
                        }

                        JsonObject memberDoc = new JsonObject()
                                .put("email", email)
                                .put("name", name)
                                .put("role", role.name())
                                .put("organizationId", organizationId)
                                .put("invitedAt", Instant.now().toString())
                                .put("joinedAt", Instant.now().toString());

                        return memberRepository.insert(memberDoc).map(id -> {
                            memberDoc.put("_id", id);
                            logger.info("Member invited: {} to org: {}", email, organizationId);
                            return Member.fromJson(memberDoc);
                        });
                    })
            );
        });
    }

    public Future<Member> getMember(String memberId, String organizationId) {
        return memberRepository.findById(memberId).compose(doc -> {
            if (doc == null) {
                return Future.failedFuture(new AppException(ErrorCode.NOT_FOUND, "Member not found"));
            }
            Member member = Member.fromJson(doc);
            if (!organizationId.equals(member.getOrganizationId())) {
                return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN, "Access denied to this member"));
            }
            return Future.succeededFuture(member);
        });
    }

    public Future<List<Member>> listMembers(String organizationId, int skip, int limit) {
        return memberRepository.findByOrganization(organizationId, skip, limit)
                .map(docs -> docs.stream().map(Member::fromJson).toList());
    }

    public Future<Long> countMembers(String organizationId) {
        return memberRepository.countByOrganization(organizationId);
    }

    public Future<Member> updateRole(String memberId, String newRoleStr, String organizationId, String actingUserId) {
        Member.Role newRole;
        try {
            newRole = Member.Role.valueOf(newRoleStr);
        } catch (IllegalArgumentException e) {
            return Future.failedFuture(new AppException(ErrorCode.VALIDATION_ERROR, "Invalid role: " + newRoleStr));
        }

        // Get the acting user's role
        return getMemberByUserId(actingUserId, organizationId).compose(actingMember -> {
            // Get the target member
            return getMember(memberId, organizationId).compose(targetMember -> {
                // Validate role hierarchy: acting user must have higher role
                if (!actingMember.getRole().isHigherThan(targetMember.getRole())) {
                    return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN,
                            "Cannot modify a member with equal or higher role"));
                }

                if (!actingMember.getRole().isHigherThan(newRole)) {
                    return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN,
                            "Cannot assign a role equal to or higher than your own"));
                }

                if (newRole == Member.Role.OWNER) {
                    return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN,
                            "Cannot assign OWNER role"));
                }

                JsonObject update = new JsonObject().put("role", newRole.name());
                return memberRepository.update(memberId, update)
                        .compose(v -> getMember(memberId, organizationId));
            });
        });
    }

    public Future<Void> removeMember(String memberId, String organizationId, String actingUserId) {
        return getMemberByUserId(actingUserId, organizationId).compose(actingMember ->
                getMember(memberId, organizationId).compose(targetMember -> {
                    if (targetMember.getRole() == Member.Role.OWNER) {
                        return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN,
                                "Cannot remove the organization owner"));
                    }

                    if (!actingMember.getRole().isHigherThan(targetMember.getRole())) {
                        return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN,
                                "Cannot remove a member with equal or higher role"));
                    }

                    logger.info("Removing member: {} from org: {}", memberId, organizationId);
                    return memberRepository.softDelete(memberId);
                })
        );
    }

    /**
     * Retrieves a member and verifies they belong to the specified organization's active member list.
     * Used for operations that require strict organization membership validation.
     */
    public Future<Member> getMemberWithOrgCheck(String memberId, String organizationId) {
        return memberRepository.findById(memberId)
                .compose(doc -> {
                    if (doc == null) {
                        return Future.failedFuture(new AppException(ErrorCode.NOT_FOUND, "Member not found"));
                    }
                    Member member = Member.fromJson(doc);
                    String memberOrg = member.getOrganizationId();

                    // Verify member is in the active organization member list
                    return memberRepository.findByOrganization(organizationId, 0, 1000)
                            .map(members -> {
                                members.stream()
                                        .filter(m -> m.getString("_id").equals(memberId))
                                        .findFirst()
                                        .get(); // throws if member not in org list
                                return member;
                            });
                });
    }

    /**
     * Looks up a member by user ID (which is the member's _id in this simplified model).
     */
    private Future<Member> getMemberByUserId(String userId, String organizationId) {
        return getMember(userId, organizationId);
    }
}
