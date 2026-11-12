package com.project.cybershop.review.internal.controller;
import com.project.cybershop.review.api.dto.request.ReviewRequest;
import com.project.cybershop.review.api.dto.response.ReviewMediaResponse;
import com.project.cybershop.review.internal.service.ReviewMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController @RequiredArgsConstructor
public class ReviewMediaController {
    private final ReviewMediaService service;

    public record Submission(@Valid ReviewRequest review,List<Integer> retainedMediaIds) {}

    @GetMapping("/api/order-items/{itemId}/review")
    public ReviewMediaService.Editor editor(Authentication auth,@PathVariable Integer itemId) {
        return service.editor(auth.getName(),itemId);
    }
    @PutMapping(value="/api/order-items/{itemId}/review",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(Authentication auth,@PathVariable Integer itemId,@Valid @RequestPart("payload") Submission payload,
                     @RequestPart(value="files",required=false) List<MultipartFile> files){
        if(payload.review()==null)throw new com.project.cybershop.shared.exception.BusinessException("Thiếu nội dung đánh giá.");
        service.save(auth.getName(),itemId,payload.review(),payload.retainedMediaIds(),files);
    }
    @GetMapping("/api/reviews/{reviewId}/media")
    public List<ReviewMediaResponse> list(@PathVariable Integer reviewId){
        return service.list(reviewId);
    }

    @GetMapping("/api/review-media/{id}")
    public ResponseEntity<ByteArrayResource> file(@PathVariable Integer id){
        var file=service.content(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.getContentType())).contentLength(file.getFileSize())
                .header("X-Content-Type-Options","nosniff").header("Content-Disposition","inline")
                .body(new ByteArrayResource(file.getData()));
    }
}
