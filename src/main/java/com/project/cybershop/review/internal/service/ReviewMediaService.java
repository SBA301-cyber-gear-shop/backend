package com.project.cybershop.review.internal.service;
import com.project.cybershop.review.api.dto.request.ReviewRequest;
import com.project.cybershop.review.api.dto.response.ReviewMediaResponse;
import com.project.cybershop.review.internal.entity.ReviewMedia;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
public interface ReviewMediaService {
    record Editor(Integer rating, String comment, List<ReviewMediaResponse> media, boolean reviewed) {}
    Editor editor(String email,Integer itemId);
    void save(String email,Integer itemId,ReviewRequest request,List<Integer> retained,List<MultipartFile> files);
    List<ReviewMediaResponse> list(Integer reviewId);
    ReviewMedia content(Integer id);
}
