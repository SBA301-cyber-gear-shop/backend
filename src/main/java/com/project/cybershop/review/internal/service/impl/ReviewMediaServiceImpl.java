package com.project.cybershop.review.internal.service.impl;

import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.review.api.dto.request.ReviewRequest;
import com.project.cybershop.review.api.dto.response.ReviewMediaResponse;
import com.project.cybershop.review.internal.entity.ReviewMedia;
import com.project.cybershop.review.internal.repository.*;
import com.project.cybershop.review.internal.service.*;
import com.project.cybershop.shared.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.io.IOException;

@Service @RequiredArgsConstructor @Transactional
public class ReviewMediaServiceImpl implements ReviewMediaService {
    private final ReviewRepository reviews;
    private final ReviewMediaRepository media;
    private final ReviewService reviewService;
    private final IdentityApi identity;

    private void requireOwner(String email,Integer itemId) {
        var statuses=reviews.findOwnedOrderStatusForUpdate(identity.requireActiveUser(email).getId(),itemId);
        if(statuses.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy sản phẩm đã mua.");
        if(!"completed".equals(statuses.get(0))) throw new BusinessException("Chỉ được đánh giá sau khi đã nhận hàng.");
    }
    @Override public Editor editor(String email,Integer itemId) {
        requireOwner(email,itemId);
        var review=reviews.findFirstByOrderItemIdOrderByIdAsc(itemId).orElse(null);
        return review==null ? new Editor(5,"",List.of(),false) : new Editor(review.getRating(),review.getComment(),list(review.getId()),true);
    }
    @Override public void save(String email,Integer itemId,ReviewRequest request,List<Integer> retained,List<MultipartFile> files) {
        requireOwner(email,itemId);
        var old=reviews.findFirstByOrderItemIdOrderByIdAsc(itemId).orElse(null);
        if(old!=null) throw new BusinessException("Sản phẩm trong đơn này đã được đánh giá. Mỗi sản phẩm chỉ được đánh giá một lần.");
        var previous=old==null ? List.<ReviewMediaRepository.Metadata>of() : media.metadata(old.getId());
        var keep=retained==null ? List.<Integer>of() : retained;
        if(keep.size()>4 || keep.stream().anyMatch(Objects::isNull) || new HashSet<>(keep).size()!=keep.size()
                || !previous.stream().map(ReviewMediaRepository.Metadata::getId).toList().containsAll(keep))
            throw new BusinessException("Danh sách tệp giữ lại không hợp lệ.");
        var uploads=files==null ? List.<MultipartFile>of() : files;
        if(uploads.size()+keep.size()>4) throw new BusinessException("Tối đa 3 ảnh và 1 video cho mỗi sản phẩm.");
        List<ReviewMedia> added=new ArrayList<>();
        long images=previous.stream().filter(m->keep.contains(m.getId()) && "image".equals(m.getMediaType())).count();
        long videos=previous.stream().filter(m->keep.contains(m.getId()) && "video".equals(m.getMediaType())).count();
        for(var file:uploads) {
            ReviewMedia entry=validate(file);
            if("image".equals(entry.getMediaType())) images++; else videos++;
            added.add(entry);
        }
        if(images>3 || videos>1) throw new BusinessException("Tối đa 3 ảnh và 1 video cho mỗi sản phẩm.");
        reviewService.save(email,itemId,request);
        var saved=reviews.findFirstByOrderItemIdOrderByIdAsc(itemId).orElseThrow();
        var removed=previous.stream().map(ReviewMediaRepository.Metadata::getId).filter(id->!keep.contains(id)).toList();
        if(!removed.isEmpty()) media.deleteByReviewIdAndIdIn(saved.getId(),removed);
        int order=0;
        for(var entry:added) {entry.setReviewId(saved.getId());entry.setDisplayOrder(keep.size()+order++);}
        media.saveAllAndFlush(added);
    }
    private ReviewMedia validate(MultipartFile file) {
        String type=file.getContentType();
        boolean image=List.of("image/jpeg","image/png","image/webp","image/gif").contains(type==null ? "" : type);
        boolean video=List.of("video/mp4","video/webm").contains(type==null ? "" : type);
        if(!image && !video) throw new BusinessException("Chỉ nhận ảnh JPG, PNG, WEBP, GIF và video MP4, WEBM.");
        if(file.isEmpty() || file.getSize()>(image ? 5L : 30L)*1024*1024)
            throw new BusinessException(image ? "Mỗi ảnh phải có dữ liệu và không quá 5 MB." : "Video phải có dữ liệu và không quá 30 MB.");
        byte[] bytes;
        try {bytes=file.getBytes();}catch(IOException e){throw new BusinessException("Không đọc được tệp tải lên.");}
        boolean matches=switch(type) {
            case "image/jpeg" -> starts(bytes,255,216,255);
            case "image/png" -> starts(bytes,137,80,78,71,13,10,26,10);
            case "image/gif" -> text(bytes,0,"GIF87a") || text(bytes,0,"GIF89a");
            case "image/webp" -> text(bytes,0,"RIFF") && text(bytes,8,"WEBP");
            case "video/mp4" -> text(bytes,4,"ftyp") && bytes.length>=12;
            case "video/webm" -> starts(bytes,26,69,223,163);
            default -> false;
        };
        if(!matches) throw new BusinessException("Nội dung tệp không khớp định dạng ảnh/video.");
        var entry=new ReviewMedia();entry.setContentType(type);entry.setMediaType(image ? "image" : "video");entry.setFileSize(bytes.length);entry.setData(bytes);return entry;
    }
    private boolean starts(byte[] b,int...prefix){if(b.length<prefix.length)return false;for(int i=0;i<prefix.length;i++)if((b[i]&255)!=prefix[i])return false;return true;}
    private boolean text(byte[] b,int offset,String value){if(b.length<offset+value.length())return false;for(int i=0;i<value.length();i++)if(b[offset+i]!=(byte)value.charAt(i))return false;return true;}
    @Override @Transactional(readOnly=true) public List<ReviewMediaResponse> list(Integer id) {
        if(media.published(id)==0) return List.of();
        return media.metadata(id).stream().map(m->new ReviewMediaResponse(m.getId(),m.getMediaType(),m.getContentType(),m.getFileSize(),"/api/review-media/"+m.getId())).toList();
    }
    @Override @Transactional(readOnly=true) public ReviewMedia content(Integer id) {
        var file=media.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tệp đánh giá."));
        if(media.published(file.getReviewId())==0)throw new ResourceNotFoundException("Không tìm thấy tệp đánh giá.");
        return file;
    }
}
