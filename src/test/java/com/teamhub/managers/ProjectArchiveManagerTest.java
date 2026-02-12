package com.teamhub.managers;

import com.teamhub.TestBase;
import com.teamhub.models.Project;
import com.teamhub.repositories.ProjectRepository;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class ProjectArchiveManagerTest extends TestBase {

    @Mock
    private ProjectRepository projectRepository;

    private ProjectArchiveManager archiveManager;

    @BeforeEach
    void setUp() {
        archiveManager = new ProjectArchiveManager(projectRepository);
    }

    @Test
    void listArchivedProjects_success(Vertx vertx, VertxTestContext ctx) {
        JsonObject archivedProject = createTestProject(randomId(), TEST_ORG_ID)
                .put("status", "ARCHIVED");

        when(projectRepository.findArchived(TEST_ORG_ID, 0, 20))
                .thenReturn(Future.succeededFuture(List.of(archivedProject)));

        archiveManager.listArchivedProjects(TEST_ORG_ID, 0, 20)
                .onComplete(ctx.succeeding(projects -> {
                    ctx.verify(() -> {
                        assertEquals(1, projects.size());
                        assertEquals(Project.Status.ARCHIVED, projects.get(0).getStatus());
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void listArchivedProjects_empty(Vertx vertx, VertxTestContext ctx) {
        when(projectRepository.findArchived(TEST_ORG_ID, 0, 20))
                .thenReturn(Future.succeededFuture(List.of()));

        archiveManager.listArchivedProjects(TEST_ORG_ID, 0, 20)
                .onComplete(ctx.succeeding(projects -> {
                    ctx.verify(() -> {
                        assertTrue(projects.isEmpty());
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void isProjectOwnedByOrg_true(Vertx vertx, VertxTestContext ctx) {
        String projectId = randomId();
        JsonObject projectDoc = createTestProject(projectId, TEST_ORG_ID);

        when(projectRepository.findById(projectId))
                .thenReturn(Future.succeededFuture(projectDoc));

        archiveManager.isProjectOwnedByOrg(projectId, TEST_ORG_ID)
                .onComplete(ctx.succeeding(owned -> {
                    ctx.verify(() -> assertTrue(owned));
                    ctx.completeNow();
                }));
    }

    @Test
    void isProjectOwnedByOrg_wrongOrg(Vertx vertx, VertxTestContext ctx) {
        String projectId = randomId();
        JsonObject projectDoc = createTestProject(projectId, "other-org-id");

        when(projectRepository.findById(projectId))
                .thenReturn(Future.succeededFuture(projectDoc));

        archiveManager.isProjectOwnedByOrg(projectId, TEST_ORG_ID)
                .onComplete(ctx.succeeding(owned -> {
                    ctx.verify(() -> assertFalse(owned));
                    ctx.completeNow();
                }));
    }

    @Test
    void isProjectOwnedByOrg_notFound(Vertx vertx, VertxTestContext ctx) {
        when(projectRepository.findById("nonexistent"))
                .thenReturn(Future.succeededFuture(null));

        archiveManager.isProjectOwnedByOrg("nonexistent", TEST_ORG_ID)
                .onComplete(ctx.succeeding(owned -> {
                    ctx.verify(() -> assertFalse(owned));
                    ctx.completeNow();
                }));
    }

    @Test
    void bulkArchive_allSuccess(Vertx vertx, VertxTestContext ctx) {
        String projectId1 = randomId();
        String projectId2 = randomId();

        JsonObject projectDoc1 = createTestProject(projectId1, TEST_ORG_ID);
        JsonObject projectDoc2 = createTestProject(projectId2, TEST_ORG_ID);

        when(projectRepository.findById(projectId1)).thenReturn(Future.succeededFuture(projectDoc1));
        when(projectRepository.findById(projectId2)).thenReturn(Future.succeededFuture(projectDoc2));
        when(projectRepository.update(eq(projectId1), any(JsonObject.class)))
                .thenReturn(Future.succeededFuture());
        when(projectRepository.update(eq(projectId2), any(JsonObject.class)))
                .thenReturn(Future.succeededFuture());

        archiveManager.bulkArchive(List.of(projectId1, projectId2), TEST_ORG_ID, TEST_USER_ID)
                .onComplete(ctx.succeeding(result -> {
                    ctx.verify(() -> {
                        JsonObject summary = result.getJsonObject("summary");
                        assertEquals(2, summary.getInteger("total"));
                        assertEquals(2, summary.getInteger("archived"));
                        assertEquals(0, summary.getInteger("failed"));
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void bulkArchive_partialFailure(Vertx vertx, VertxTestContext ctx) {
        String projectId1 = randomId();
        String projectId2 = randomId();

        JsonObject projectDoc1 = createTestProject(projectId1, TEST_ORG_ID);

        when(projectRepository.findById(projectId1)).thenReturn(Future.succeededFuture(projectDoc1));
        when(projectRepository.findById(projectId2)).thenReturn(Future.succeededFuture(null));
        when(projectRepository.update(eq(projectId1), any(JsonObject.class)))
                .thenReturn(Future.succeededFuture());

        archiveManager.bulkArchive(List.of(projectId1, projectId2), TEST_ORG_ID, TEST_USER_ID)
                .onComplete(ctx.succeeding(result -> {
                    ctx.verify(() -> {
                        JsonObject summary = result.getJsonObject("summary");
                        assertEquals(2, summary.getInteger("total"));
                        assertEquals(1, summary.getInteger("archived"));
                        assertEquals(1, summary.getInteger("failed"));
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void bulkArchive_alreadyArchived(Vertx vertx, VertxTestContext ctx) {
        String projectId = randomId();
        JsonObject archivedDoc = createTestProject(projectId, TEST_ORG_ID).put("status", "ARCHIVED");

        when(projectRepository.findById(projectId)).thenReturn(Future.succeededFuture(archivedDoc));

        archiveManager.bulkArchive(List.of(projectId), TEST_ORG_ID, TEST_USER_ID)
                .onComplete(ctx.succeeding(result -> {
                    ctx.verify(() -> {
                        JsonObject summary = result.getJsonObject("summary");
                        assertEquals(1, summary.getInteger("total"));
                        assertEquals(0, summary.getInteger("archived"));
                        // Skipped counts as not-archived, not failed
                        assertEquals(1, summary.getInteger("failed"));
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void bulkRestore_success(Vertx vertx, VertxTestContext ctx) {
        String projectId = randomId();
        JsonObject archivedDoc = createTestProject(projectId, TEST_ORG_ID).put("status", "ARCHIVED");

        when(projectRepository.findById(projectId)).thenReturn(Future.succeededFuture(archivedDoc));
        when(projectRepository.update(eq(projectId), any(JsonObject.class)))
                .thenReturn(Future.succeededFuture());

        archiveManager.bulkRestore(List.of(projectId), TEST_ORG_ID, TEST_USER_ID)
                .onComplete(ctx.succeeding(result -> {
                    ctx.verify(() -> {
                        JsonObject summary = result.getJsonObject("summary");
                        assertEquals(1, summary.getInteger("total"));
                        assertEquals(1, summary.getInteger("restored"));
                        assertEquals(0, summary.getInteger("failed"));
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void getArchiveSummary_success(Vertx vertx, VertxTestContext ctx) {
        when(projectRepository.countArchived(TEST_ORG_ID)).thenReturn(Future.succeededFuture(3L));
        when(projectRepository.countByOrganization(TEST_ORG_ID)).thenReturn(Future.succeededFuture(7L));

        archiveManager.getArchiveSummary(TEST_ORG_ID)
                .onComplete(ctx.succeeding(summary -> {
                    ctx.verify(() -> {
                        assertEquals(3L, summary.getLong("archivedProjects"));
                        assertEquals(7L, summary.getLong("activeProjects"));
                        assertEquals(10L, summary.getLong("totalProjects"));
                    });
                    ctx.completeNow();
                }));
    }
}
