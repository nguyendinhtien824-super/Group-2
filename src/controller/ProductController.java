package controller;

import model.Product;
import repository.ProductRepository;

import java.util.List;

public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(Product product) {
        productRepository.create(product);
        return product;
    }

    public Product findProduct(String productId) {
        return productRepository.findById(productId);
    }

    public List<Product> listProducts() {
        return productRepository.findAll();
    }

    public boolean updateProduct(Product product, int expectedVersion) {
        return productRepository.update(product, expectedVersion);
    }

    public boolean deleteProduct(String productId) {
        return productRepository.deleteById(productId);
    }

    public List<Product> searchProducts(String category, int minimumPrice, int maximumPrice) {
        return productRepository.searchByCategoryAndPriceRange(
                category, minimumPrice, maximumPrice);
    }
}
