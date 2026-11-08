package com.project.cybershop.review.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name="review_media") @Getter @Setter @NoArgsConstructor
public class ReviewMedia {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(name="review_id", nullable=false) private Integer reviewId;
    @Column(name="media_type", nullable=false, length=10) private String mediaType;
    @Column(name="content_type", nullable=false, length=50) private String contentType;
    @Column(name="file_size", nullable=false) private long fileSize;
    @Column(name="display_order", nullable=false) private int displayOrder;
    @Column(nullable=false, columnDefinition="bytea") private byte[] data;
}
