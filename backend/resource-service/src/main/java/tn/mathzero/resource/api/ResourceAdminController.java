package tn.mathzero.resource.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tn.mathzero.resource.domain.ExternalResource;
import tn.mathzero.resource.service.ResourceService;
import tn.mathzero.resource.service.ResourceSuggestionService;

@Validated
@RestController
@RequestMapping("/api/v1/admin/resources")
public class ResourceAdminController {
    private final ResourceService resourceService;
    private final ResourceSuggestionService suggestionService;

    public ResourceAdminController(
            ResourceService resourceService, ResourceSuggestionService suggestionService) {
        this.resourceService = resourceService;
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public ResourceDtos.ApiResponse<ResourceDtos.Page<ResourceDtos.AdminResourceView>> list(
            @RequestParam(required = false) ExternalResource.ReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return response(resourceService.findForAdmin(
                status, PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)),
                        Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/search")
    public ResourceDtos.ApiResponse<List<ResourceDtos.ProviderSearchResult>> searchProviders(
            @RequestParam @NotBlank @Size(max = 200) String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            @RequestParam(defaultValue = "fr") @Pattern(regexp = "fr|ar") String language) {
        return response(resourceService.searchProviders(q, limit, language));
    }

    @GetMapping("/suggestions")
    public ResourceDtos.ApiResponse<ResourceDtos.SuggestionList> suggestions() {
        return response(new ResourceDtos.SuggestionList(suggestionService.suggestions()));
    }

    @PostMapping("/suggestions/approve")
    public ResourceDtos.ApiResponse<ResourceDtos.AdminResourceView> approveSuggestion(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ResourceDtos.ApproveSuggestionRequest request) {
        return response(suggestionService.approve(actorId(jwt), request));
    }

    @PostMapping("/providers/import")
    public ResponseEntity<ResourceDtos.ApiResponse<ResourceDtos.AdminResourceView>> importProviderResult(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ResourceDtos.ImportProviderResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(resourceService.importProviderResult(actorId(jwt), request)));
    }

    @PostMapping
    public ResponseEntity<ResourceDtos.ApiResponse<ResourceDtos.AdminResourceView>> createManual(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ResourceDtos.CreateManualRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(resourceService.createManual(actorId(jwt), request)));
    }

    @PostMapping("/{resourceId}/review")
    public ResourceDtos.ApiResponse<ResourceDtos.AdminResourceView> review(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID resourceId,
            @Valid @RequestBody ResourceDtos.ReviewRequest request) {
        return response(resourceService.review(actorId(jwt), resourceId, request));
    }

    @GetMapping("/{resourceId}/reviews")
    public ResourceDtos.ApiResponse<java.util.List<ResourceDtos.ReviewView>> reviewHistory(
            @PathVariable UUID resourceId) {
        return response(resourceService.reviewHistory(resourceId));
    }

    @GetMapping("/providers")
    public ResourceDtos.ApiResponse<ResourceDtos.ProviderList> providers() {
        return response(new ResourceDtos.ProviderList(resourceService.providerStatus()));
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static <T> ResourceDtos.ApiResponse<T> response(T data) {
        return new ResourceDtos.ApiResponse<>(data, new ResourceDtos.Meta(UUID.randomUUID().toString()));
    }
}
