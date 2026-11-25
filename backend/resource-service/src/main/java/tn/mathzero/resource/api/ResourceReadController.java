package tn.mathzero.resource.api;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tn.mathzero.resource.domain.ExternalResource;
import tn.mathzero.resource.service.ResourceService;

@Validated
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceReadController {
    private final ResourceService resourceService;

    public ResourceReadController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ResourceDtos.ApiResponse<ResourceDtos.Page<ResourceDtos.ResourceView>> findApproved(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String gradeCode,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) ExternalResource.Cycle cycle,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return response(resourceService.findApproved(
                search, language, gradeCode, subjectCode, cycle,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "title"))));
    }

    private static <T> ResourceDtos.ApiResponse<T> response(T data) {
        return new ResourceDtos.ApiResponse<>(data, new ResourceDtos.Meta(UUID.randomUUID().toString()));
    }
}
