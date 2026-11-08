package com.project.cybershop.review.internal.config;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.*;
@Configuration
public class ReviewUploadConfig {
    @Bean public MultipartConfigElement multipartConfigElement(){
        return new MultipartConfigElement("",30L*1024*1024,46L*1024*1024,0);
    }
}
