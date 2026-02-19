package com.teamhub.managers;

import com.teamhub.common.AppException;
import com.teamhub.common.ErrorCode;
import com.teamhub.models.BillingPlan;
import com.teamhub.models.Organization;
import com.teamhub.repositories.BillingPlanRepository;
import com.teamhub.repositories.MemberRepository;
import com.teamhub.repositories.OrganizationRepository;
import com.teamhub.repositories.ProjectRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BillingManager {

    private static final Logger logger = LoggerFactory.getLogger(BillingManager.class);

    private final BillingPlanRepository billingPlanRepository;
    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;

    public BillingManager(BillingPlanRepository billingPlanRepository,
                          OrganizationRepository organizationRepository,
                          MemberRepository memberRepository,
                          ProjectRepository projectRepository) {
        this.billingPlanRepository = billingPlanRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Get the current billing plan for an organization.
     * Falls back to a default FREE plan if not found.
     */
    public Future<BillingPlan> getCurrentPlan(String organizationId) {
        return organizationRepository.findById(organizationId).compose(orgDoc -> {
            if (orgDoc == null) {
                return Future.failedFuture(new AppException(ErrorCode.NOT_FOUND, "Organization not found"));
            }

            String planId = orgDoc.getString("billingPlanId", "free");
            return billingPlanRepository.findById(planId).map(planDoc -> {
                if (planDoc == null) {
                    // Return default free plan if plan not found in DB
                    return getDefaultFreePlan();
                }
                return BillingPlan.fromJson(planDoc);
            });
        });
    }

    /**
     * Check resource limits against the current billing plan.
     */
    public Future<JsonObject> getUsage(String organizationId) {
        return getCurrentPlan(organizationId).compose(plan ->
                memberRepository.countByOrganization(organizationId).compose(memberCount ->
                        projectRepository.countByOrganization(organizationId).map(projectCount -> {
                            return new JsonObject()
                                    .put("plan", plan.toJson())
                                    .put("usage", new JsonObject()
                                            .put("members", memberCount)
                                            .put("maxMembers", plan.getMaxMembers())
                                            .put("projects", projectCount)
                                            .put("maxProjects", plan.getMaxProjects()));
                        })
                )
        );
    }

    /**
     * Calculate monthly pricing for a given tier.
     */
    public Future<JsonObject> calculatePricing(String tier) {
        return billingPlanRepository.findByTier(tier).map(planDoc -> {
            BillingPlan plan = planDoc != null ? BillingPlan.fromJson(planDoc) : getDefaultPlanForTier(tier);
            return new JsonObject()
                    .put("tier", plan.getTier().name())
                    .put("name", plan.getName())
                    .put("pricePerMonth", plan.getPricePerMonth())
                    .put("maxMembers", plan.getMaxMembers())
                    .put("maxProjects", plan.getMaxProjects())
                    .put("features", plan.getFeatures());
        });
    }

    /**
     * Determine if the organization should be prompted to upgrade their tier.
     * Returns true if the current member count has outgrown the plan capacity.
     */
    public Future<Boolean> shouldUpgradeTier(String organizationId) {
        return getCurrentPlan(organizationId)
                .compose(plan -> memberRepository.countByOrganization(organizationId)
                        .map(memberCount -> {
                            // Check if current tier can still accommodate the team
                            if (memberCount < plan.getMaxMembers()) {
                                return false; // current tier handles the load
                            }
                            return true; // recommend upgrade
                        }));
    }

    private BillingPlan getDefaultFreePlan() {
        return BillingPlan.builder()
                .id("free")
                .name("Free")
                .tier(BillingPlan.Tier.FREE)
                .maxMembers(5)
                .maxProjects(3)
                .pricePerMonth(0.0)
                .features(java.util.List.of("Basic project management", "Up to 5 members", "Up to 3 projects"))
                .build();
    }

    private BillingPlan getDefaultPlanForTier(String tier) {
        return switch (tier) {
            case "STARTER" -> BillingPlan.builder()
                    .id("starter")
                    .name("Starter")
                    .tier(BillingPlan.Tier.STARTER)
                    .maxMembers(15)
                    .maxProjects(10)
                    .pricePerMonth(9.99)
                    .features(java.util.List.of("Advanced project management", "Up to 15 members", "Up to 10 projects"))
                    .build();
            case "PROFESSIONAL" -> BillingPlan.builder()
                    .id("professional")
                    .name("Professional")
                    .tier(BillingPlan.Tier.PROFESSIONAL)
                    .maxMembers(50)
                    .maxProjects(50)
                    .pricePerMonth(29.99)
                    .features(java.util.List.of("Full project management", "Up to 50 members", "Up to 50 projects", "Analytics"))
                    .build();
            case "ENTERPRISE" -> BillingPlan.builder()
                    .id("enterprise")
                    .name("Enterprise")
                    .tier(BillingPlan.Tier.ENTERPRISE)
                    .maxMembers(Integer.MAX_VALUE)
                    .maxProjects(Integer.MAX_VALUE)
                    .pricePerMonth(99.99)
                    .features(java.util.List.of("Unlimited members", "Unlimited projects", "Priority support", "SSO"))
                    .build();
            default -> getDefaultFreePlan();
        };
    }
}
