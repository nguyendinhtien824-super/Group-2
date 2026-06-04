package controller;

import model.Customer;
import repository.CustomerRepository;

import java.util.Map;
import java.util.Optional;

public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Map<String, Object> register(Map<String, String> payload) {
        String email = payload.get("email");
        String name = payload.get("name");
        String phone = payload.get("phone");
        String address = payload.get("address");
        String avatarUrl = payload.get("avatarUrl");

        if (email == null || name == null || phone == null) {
            return Map.of("success", false, "message", "Thieu thong tin bat buoc");
        }

        boolean exists = customerRepository.findAll().stream()
                .anyMatch(c -> c.getEmail().equalsIgnoreCase(email));

        if (exists) {
            return Map.of("success", false, "message", "Email da ton tai");
        }

        int currentSize = customerRepository.findAll().size();
        String newId = String.format("C-%05d", currentSize + 1);

        Customer newCustomer = new Customer(newId, name, email, phone, address != null ? address : "Default Address", avatarUrl != null ? avatarUrl : "");
        customerRepository.save(newCustomer);

        return Map.of("success", true, "message", "Dang ky thanh cong", "customer", newCustomer);
    }

    public Optional<Customer> login(Map<String, String> payload) {
        String email = payload.get("email");

        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        return customerRepository.findAll().stream()
                .filter(c -> c.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst();
    }
}
