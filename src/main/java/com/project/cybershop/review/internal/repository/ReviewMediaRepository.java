package com.project.cybershop.review.internal.repository;
import com.project.cybershop.review.internal.entity.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia,Integer> {
    interface Metadata { Integer getId(); String getMediaType(); String getContentType(); long getFileSize(); }
    @Query("select m.id as id,m.mediaType as mediaType,m.contentType as contentType,m.fileSize as fileSize from ReviewMedia m where m.reviewId=:id order by m.displayOrder,m.id")
    List<Metadata> metadata(@Param("id") Integer reviewId);
    void deleteByReviewIdAndIdIn(Integer reviewId, List<Integer> ids);
    @Query(value="SELECT count(*) FROM reviews r JOIN order_items oi ON oi.id=r.order_item_id JOIN orders o ON o.id=oi.order_id WHERE r.id=:id AND o.status='completed'",nativeQuery=true)
    int published(@Param("id") Integer reviewId);
}
