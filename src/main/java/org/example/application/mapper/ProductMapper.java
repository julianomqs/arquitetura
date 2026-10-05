package org.example.application.mapper;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

import org.example.application.controller.ProductController.CreateProductDto;
import org.example.application.controller.ProductController.PatchProductDto;
import org.example.application.controller.ProductController.ProductDto;
import org.example.domain.entity.Product;
import org.example.infrastructure.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ProductMapper {

  Product toProduct(CreateProductDto dto);

  Product toProduct(ProductEntity entity);

  ProductDto toDto(Product product);

  Product updateProduct(CreateProductDto dto, @MappingTarget Product product);

  Product updateProduct(Product product, @MappingTarget Product productToUpdate);

  @Mapping(target = "name", nullValuePropertyMappingStrategy = IGNORE)
  Product patchProduct(PatchProductDto dto, @MappingTarget Product product);

  Product fromId(Integer id);

  ProductEntity toProductEntity(Product client);
}
