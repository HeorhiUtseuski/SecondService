package by.gvu.secondservice.baselogic.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(value = "dummyjson", url = "https://dummyjson.com/")
public interface ClientCategories {
    @GetMapping(value = "/products/category-list")
    List<String> getCategoryList();
}
