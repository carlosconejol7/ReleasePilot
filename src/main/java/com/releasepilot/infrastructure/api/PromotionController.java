package com.releasepilot.infrastructure.api;

import com.releasepilot.application.command.ApprovePromotionCommand;
import com.releasepilot.application.command.ApprovePromotionCommandHandler;
import com.releasepilot.application.command.CancelPromotionCommand;
import com.releasepilot.application.command.CancelPromotionCommandHandler;
import com.releasepilot.application.command.CompletePromotionCommand;
import com.releasepilot.application.command.CompletePromotionCommandHandler;
import com.releasepilot.application.command.RequestPromotionCommand;
import com.releasepilot.application.command.RequestPromotionCommandHandler;
import com.releasepilot.application.command.RollbackPromotionCommand;
import com.releasepilot.application.command.RollbackPromotionCommandHandler;
import com.releasepilot.application.command.StartDeploymentCommand;
import com.releasepilot.application.command.StartDeploymentCommandHandler;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.infrastructure.api.dto.ApprovePromotionRequest;
import com.releasepilot.infrastructure.api.dto.CancelPromotionRequest;
import com.releasepilot.infrastructure.api.dto.CompletePromotionRequest;
import com.releasepilot.infrastructure.api.dto.PromotionIdResponse;
import com.releasepilot.infrastructure.api.dto.RequestPromotionRequest;
import com.releasepilot.infrastructure.api.dto.RollbackPromotionRequest;
import com.releasepilot.infrastructure.api.dto.StartDeploymentRequest;
import com.releasepilot.query.model.PromotionView;
import com.releasepilot.query.service.PromotionQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing both the read-only Promotion detail endpoint and the write-side
 * endpoints that trigger Promotion lifecycle state transitions.
 *
 * <p>Kept intentionally thin: read queries are delegated to {@link PromotionQueryService}, and
 * every write operation simply translates its HTTP request body into the corresponding
 * {@code Command} and delegates to the already-existing command handler. All domain rule
 * enforcement happens in the {@code Promotion} aggregate and its handlers; this controller
 * performs no business logic of its own.</p>
 *
 * <p>Write endpoints return as soon as the command has been applied and its resulting domain
 * events published; any side effects (projections, workflow orchestration, audit logging) are
 * handled asynchronously by their respective listeners and do not delay the HTTP response.</p>
 */
@RestController
@RequestMapping("/promotions")
public class PromotionController {

    private final PromotionQueryService promotionQueryService;
    private final RequestPromotionCommandHandler requestPromotionCommandHandler;
    private final ApprovePromotionCommandHandler approvePromotionCommandHandler;
    private final StartDeploymentCommandHandler startDeploymentCommandHandler;
    private final CompletePromotionCommandHandler completePromotionCommandHandler;
    private final CancelPromotionCommandHandler cancelPromotionCommandHandler;
    private final RollbackPromotionCommandHandler rollbackPromotionCommandHandler;

    public PromotionController(PromotionQueryService promotionQueryService,
                                RequestPromotionCommandHandler requestPromotionCommandHandler,
                                ApprovePromotionCommandHandler approvePromotionCommandHandler,
                                StartDeploymentCommandHandler startDeploymentCommandHandler,
                                CompletePromotionCommandHandler completePromotionCommandHandler,
                                CancelPromotionCommandHandler cancelPromotionCommandHandler,
                                RollbackPromotionCommandHandler rollbackPromotionCommandHandler) {
        this.promotionQueryService = promotionQueryService;
        this.requestPromotionCommandHandler = requestPromotionCommandHandler;
        this.approvePromotionCommandHandler = approvePromotionCommandHandler;
        this.startDeploymentCommandHandler = startDeploymentCommandHandler;
        this.completePromotionCommandHandler = completePromotionCommandHandler;
        this.cancelPromotionCommandHandler = cancelPromotionCommandHandler;
        this.rollbackPromotionCommandHandler = rollbackPromotionCommandHandler;
    }

    /**
     * Returns the detail view (including full state history) of a single Promotion.
     *
     * @param id the identifier of the promotion
     * @return {@code 200 OK} with the {@link PromotionView} if found, {@code 404 Not Found} otherwise
     */
    @GetMapping("/{id}")
    public ResponseEntity<PromotionView> getPromotion(@PathVariable String id) {
        return promotionQueryService.getPromotionById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Requests a new Promotion.
     *
     * @param request the request describing the promotion to create
     * @return {@code 201 Created} with the identifier of the newly created promotion
     */
    @PostMapping
    public ResponseEntity<PromotionIdResponse> requestPromotion(@RequestBody RequestPromotionRequest request) {
        RequestPromotionCommand command = new RequestPromotionCommand(
                request.applicationId(),
                request.version(),
                request.sourceEnvironment(),
                request.targetEnvironment(),
                request.requestedBy()
        );
        PromotionId promotionId = requestPromotionCommandHandler.handle(command);
        return ResponseEntity.status(201).body(new PromotionIdResponse(promotionId.value()));
    }

    /**
     * Approves an existing Promotion.
     *
     * @param id      the identifier of the promotion to approve
     * @param request the request describing who is approving the promotion
     * @return {@code 204 No Content} once the approval has been applied
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approvePromotion(@PathVariable String id, @RequestBody ApprovePromotionRequest request) {
        approvePromotionCommandHandler.handle(new ApprovePromotionCommand(id, request.approver()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Starts deployment for an existing Promotion.
     *
     * @param id      the identifier of the promotion whose deployment is starting
     * @param request the request describing who is starting the deployment
     * @return {@code 204 No Content} once the transition has been applied
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startDeployment(@PathVariable String id, @RequestBody StartDeploymentRequest request) {
        startDeploymentCommandHandler.handle(new StartDeploymentCommand(id, request.operator()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Completes deployment for an existing Promotion.
     *
     * @param id      the identifier of the promotion whose deployment is completing
     * @param request the request describing who is completing the deployment
     * @return {@code 204 No Content} once the transition has been applied
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completePromotion(@PathVariable String id, @RequestBody CompletePromotionRequest request) {
        completePromotionCommandHandler.handle(new CompletePromotionCommand(id, request.operator()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Cancels an existing Promotion.
     *
     * @param id      the identifier of the promotion to cancel
     * @param request the request describing who is cancelling the promotion and why
     * @return {@code 204 No Content} once the transition has been applied
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelPromotion(@PathVariable String id, @RequestBody CancelPromotionRequest request) {
        cancelPromotionCommandHandler.handle(new CancelPromotionCommand(id, request.cancelledBy(), request.reason()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Rolls back an existing Promotion.
     *
     * @param id      the identifier of the promotion to roll back
     * @param request the request describing who is performing the rollback and why
     * @return {@code 204 No Content} once the transition has been applied
     */
    @PostMapping("/{id}/rollback")
    public ResponseEntity<Void> rollbackPromotion(@PathVariable String id, @RequestBody RollbackPromotionRequest request) {
        rollbackPromotionCommandHandler.handle(new RollbackPromotionCommand(id, request.operator(), request.reason()));
        return ResponseEntity.noContent().build();
    }
}
