package service;

import model.Customer;
import model.enums.CustTier;
import repository.CustomerRepository;
import security.PasswordPolicy;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Business rules for administrator-managed customer accounts. */
public class CustomerAdminService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{9,11}$");
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "BANNED");

    private final CustomerRepository customerRepository;
    private final Consumer<String> pendingOrderCanceller;

    public CustomerAdminService(CustomerRepository customerRepository,
                                Consumer<String> pendingOrderCanceller) {
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository");
        this.pendingOrderCanceller = Objects.requireNonNull(
                pendingOrderCanceller, "pendingOrderCanceller");
    }

    public List<Customer> list() {
        return customerRepository.findAll().stream()
                .sorted((first, second) -> first.getCustomerId().compareToIgnoreCase(
                        second.getCustomerId()))
                .toList();
    }

    public List<Customer> search(String keyword) {
        String normalized = required(keyword, "Từ khóa tìm kiếm").toLowerCase(Locale.ROOT);
        return customerRepository.findAll().stream()
                .filter(customer -> contains(customer.getCustomerId(), normalized)
                        || contains(customer.getEmail(), normalized)
                        || contains(customer.getName(), normalized)
                        || contains(customer.getPhone(), normalized))
                .toList();
    }

    public Customer create(String name, String email, String phone, String address,
                           String avatarUrl, String tier, String password) {
        String validName = validName(name);
        String validEmail = validEmail(email);
        String validPhone = validPhone(phone);
        String validAddress = required(address, "Địa chỉ");
        CustTier validTier = parseTier(tier, CustTier.STANDARD);
        validatePassword(password);

        synchronized (customerRepository) {
            ensureEmailAvailable(validEmail, null);
            Customer customer = new Customer(
                    customerRepository.generateNewCustomerId(), validName, validEmail,
                    validPhone, validAddress, normalizedOptional(avatarUrl), validTier, "ACTIVE");
            customer.setPassword(password);
            customerRepository.save(customer);
            return customer;
        }
    }

    public Customer update(String customerId, String name, String email, String phone,
                           String address, String avatarUrl, String tier, String newPassword) {
        Customer customer = requireCustomer(customerId);
        String updatedName = isBlank(name) ? customer.getName() : validName(name);
        String updatedEmail = isBlank(email) ? customer.getEmail() : validEmail(email);
        String updatedPhone = isBlank(phone) ? customer.getPhone() : validPhone(phone);
        String updatedAddress = isBlank(address) ? customer.getAddress() : address.trim();
        CustTier updatedTier = parseTier(tier, customer.getTier());

        if (!isBlank(newPassword)) {
            validatePassword(newPassword);
        }

        synchronized (customerRepository) {
            ensureEmailAvailable(updatedEmail, customer.getCustomerId());
            customer.setName(updatedName);
            customer.setEmail(updatedEmail);
            customer.setPhone(updatedPhone);
            customer.setAddress(updatedAddress);
            if (!isBlank(avatarUrl)) {
                customer.setAvatarUrl(avatarUrl.trim());
            }
            customer.setTier(updatedTier);
            if (!isBlank(newPassword)) {
                customer.setPassword(newPassword);
            }
            customerRepository.save(customer);
            return customer;
        }
    }

    public Customer setStatus(String customerId, String status) {
        String normalizedStatus = required(status, "Trạng thái").toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Trạng thái chỉ được là ACTIVE hoặc BANNED.");
        }

        Customer customer = requireCustomer(customerId);
        if (normalizedStatus.equalsIgnoreCase(customer.getStatus())) {
            return customer;
        }
        if ("BANNED".equals(normalizedStatus)) {
            cancelPendingOrders(customerId, "khóa");
        }
        customer.setStatus(normalizedStatus);
        customerRepository.save(customer);
        return customer;
    }

    public Customer delete(String customerId) {
        Customer customer = requireCustomer(customerId);
        cancelPendingOrders(customer.getCustomerId(), "xóa");
        if (!customerRepository.deleteById(customer.getCustomerId())) {
            throw new IllegalStateException("Không thể xóa tài khoản khách hàng.");
        }
        return customer;
    }

    private Customer requireCustomer(String customerId) {
        String id = required(customerId, "Mã khách hàng");
        Customer customer = customerRepository.findById(id);
        if (customer == null) {
            throw new IllegalArgumentException("Không tìm thấy khách hàng: " + id);
        }
        return customer;
    }

    private void cancelPendingOrders(String customerId, String action) {
        try {
            pendingOrderCanceller.accept(customerId);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Không thể " + action + " khách hàng vì đơn PENDING/APPROVED chưa hủy hết.",
                    exception);
        }
    }

    private void ensureEmailAvailable(String email, String currentCustomerId) {
        boolean duplicate = customerRepository.findAll().stream()
                .anyMatch(customer -> customer.getEmail() != null
                        && customer.getEmail().equalsIgnoreCase(email)
                        && !customer.getCustomerId().equalsIgnoreCase(
                                currentCustomerId == null ? "" : currentCustomerId));
        if (duplicate) {
            throw new IllegalArgumentException("Email đã tồn tại trong hệ thống.");
        }
    }

    private static void validatePassword(String password) {
        PasswordPolicy.validationError(password).ifPresent(error -> {
            throw new IllegalArgumentException(error);
        });
    }

    private static CustTier parseTier(String value, CustTier defaultTier) {
        if (isBlank(value)) {
            return defaultTier != null ? defaultTier : CustTier.STANDARD;
        }
        try {
            return CustTier.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Hạng khách hàng phải là STANDARD, SILVER, GOLD hoặc DIAMOND.");
        }
    }

    private static String validName(String value) {
        String name = required(value, "Họ tên");
        if (name.codePointCount(0, name.length()) < 2) {
            throw new IllegalArgumentException("Họ tên phải có ít nhất 2 ký tự.");
        }
        return name;
    }

    private static String validEmail(String value) {
        String email = required(value, "Email");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email không đúng định dạng.");
        }
        return email;
    }

    private static String validPhone(String value) {
        String phone = required(value, "Số điện thoại");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Số điện thoại phải gồm 9 đến 11 chữ số.");
        }
        return phone;
    }

    private static String required(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " không được để trống.");
        }
        return value.trim();
    }

    private static String normalizedOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
