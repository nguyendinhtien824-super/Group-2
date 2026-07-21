package repository;

import exception.InvalidProductException;
import model.Product;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;

/**
 * Repository xử lý I/O dữ liệu sản phẩm từ file CSV.
 */
public class ProductRepository extends CsvRepository<Product> {
    public ProductRepository() {
        super(Product.class, "products.csv",
                "productId,name,brand,category,price,stock,description,version");
    }

    public ProductRepository(String dataDirectory) {
        super(Product.class, dataDirectory, "products.csv",
                "productId,name,brand,category,price,stock,description,version");
    }

    public void create(Product product) {
        validate(product);
        if (findById(product.getId()) != null) {
            throw new InvalidProductException("Mã sản phẩm đã tồn tại: " + product.getId());
        }
        super.save(product);
    }

    @Override
    public void save(Product product) {
        validate(product);
        super.save(product);
    }

    @Override
    public void saveAll(List<Product> products) {
        products.forEach(ProductRepository::validate);
        super.saveAll(products);
    }

    @Override
    public boolean update(Product product, int expectedVersion) {
        validate(product);
        Lock writeLock = getRwLock().writeLock();
        writeLock.lock();
        try {
            Product current = findById(product.getId());
            if (current == null || current.getVersion() != expectedVersion) {
                return false;
            }
            product.setVersion(expectedVersion + 1);
            return super.update(product, expectedVersion);
        } finally {
            writeLock.unlock();
        }
    }

    public List<Product> searchByCategoryAndPriceRange(
            String category, int minimumPrice, int maximumPrice) {
        if (minimumPrice < 0 || maximumPrice < minimumPrice) {
            throw new InvalidProductException("Khoảng giá tìm kiếm không hợp lệ");
        }
        String normalizedCategory = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        return findAll().stream()
                .filter(product -> normalizedCategory.isEmpty()
                        || product.getCategory().toLowerCase(Locale.ROOT).equals(normalizedCategory))
                .filter(product -> product.getPrice() >= minimumPrice
                        && product.getPrice() <= maximumPrice)
                .toList();
    }

    private static void validate(Product product) {
        if (product == null || product.getProductId() == null || product.getProductId().isBlank()) {
            throw new InvalidProductException("Mã sản phẩm không được để trống");
        }
        if (product.getName() == null || product.getName().isBlank()) {
            throw new InvalidProductException("Tên sản phẩm không được để trống");
        }
        if (product.getCategory() == null || product.getCategory().isBlank()) {
            throw new InvalidProductException("Danh mục sản phẩm không được để trống");
        }
        if (product.getPrice() <= 0 || product.getStock() < 0 || product.getVersion() < 0) {
            throw new InvalidProductException("Giá, tồn kho hoặc phiên bản sản phẩm không hợp lệ");
        }
    }
}
