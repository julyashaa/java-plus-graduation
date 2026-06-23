package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public List<CategoryDto> getAll(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        return categoryRepository.findAll(pageable)
                .map(categoryMapper::toDto)
                .getContent();
    }

    public List<CategoryDto> getAll() {
        return categoryRepository.findAll().stream().map(categoryMapper::toDto).toList();
    }

    public CategoryDto getById(long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        return categoryMapper.toDto(category);
    }

    @Transactional
    public CategoryDto create(NewCategoryDto dto) {
        Category category = categoryMapper.toEntity(dto);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toDto(saved);
    }

    @Transactional
    public CategoryDto update(long catId, NewCategoryDto dto) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        category.setName(dto.getName());
        Category saved = categoryRepository.save(category);
        return categoryMapper.toDto(saved);
    }

    @Transactional
    public void delete(long catId) {
        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("Category with id=" + catId + " was not found");
        }

        categoryRepository.deleteById(catId);
    }
}
