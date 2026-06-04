package repository;

import model.Product;

import java.util.List;
import java.util.stream.Collectors;

public class ProductRepository extends CsvRepository<Product> {
    public ProductRepository() {
        super("products.csv", "productId,name,brand,category,price,stock,description");
    }

    public ProductRepository(String dataDirectory) {
        super(dataDirectory, "products.csv", "productId,name,brand,category,price,stock,description");
    }

    @Override
    protected Product parseLine(String line) {
        return Product.fromCsvLine(line);
    }

    public List<Product> searchByCategoryAndPrice(String category, int minPrice, int maxPrice) {
        String normalizedCategory = category == null ? "" : category.trim().toLowerCase();
        return findAll().stream()
                .filter(product -> normalizedCategory.isEmpty()
                        || product.getCategory().toLowerCase().contains(normalizedCategory))
                .filter(product -> product.getPrice() >= minPrice && product.getPrice() <= maxPrice)
                .collect(Collectors.toList());
    }
}

