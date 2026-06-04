package repository;

import model.Customer;

public class CustomerRepository extends CsvRepository<Customer> {
    public CustomerRepository() {
        super("customers.csv", "customerId,name,email,phone,address,avatarUrl,tier,status");
    }

    public CustomerRepository(String dataDirectory) {
        super(dataDirectory, "customers.csv", "customerId,name,email,phone,address,avatarUrl,tier,status");
    }

    @Override
    protected Customer parseLine(String line) {
        return Customer.fromCsvLine(line);
    }

    public String generateNewCustomerId() {
        int maxId = findAll().stream()
                .map(Customer::getCustomerId)
                .filter(id -> id != null && id.startsWith("C-"))
                .mapToInt(id -> {
                    try {
                        return Integer.parseInt(id.substring(2));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return String.format("C-%05d", maxId + 1);
    }
}

