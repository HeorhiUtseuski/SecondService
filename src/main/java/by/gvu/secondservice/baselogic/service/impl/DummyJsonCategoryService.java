package by.gvu.secondservice.baselogic.service.impl;

import by.gvu.secondservice.baselogic.feign.ClientCategories;
import by.gvu.secondservice.baselogic.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DummyJsonCategoryService implements CategoryService {
    private final ClientCategories clientCategories;

    @Override
    public List<String> findAll() {
        return clientCategories.getCategoryList();
    }
}
