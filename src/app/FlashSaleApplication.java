package app;

import controller.AdminCustomerController;
import controller.AdminOrderController;
import controller.AdminReportController;
import controller.CustomerController;
import controller.DataController;
import controller.FlashSaleController;
import controller.OrderController;
import controller.OrderTrackingController;
import controller.ProductController;
import controller.SimulationReportController;
import controller.SimulatorController;
import controller.VoucherController;
import repository.CustomerRepository;
import repository.FlashItemRepository;
import repository.FlashSaleEventRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.ProductRepository;
import repository.VoucherRepository;
import security.AdminCredentials;
import security.AdminPasswordTool;
import service.AdminOrderService;
import service.AdminReportService;
import service.CustomerAdminService;
import service.DataGeneratorService;
import service.FlashSaleEventService;
import service.FlashSaleService;
import service.FlashSaleServiceImpl;
import service.SimulationReportService;
import service.SimulatorService;
import service.VoucherService;
import view.AdminCustomerView;
import view.AdminOrderView;
import view.AdminReportView;
import view.AdminVoucherView;
import view.AdminView;
import view.ConsoleInput;
import view.CustomerAccountView;
import view.CustomerVoucherView;
import view.FlashSaleEventAdminView;
import view.FlashSaleItemAdminView;
import view.FlashSaleShoppingView;
import view.FlashSaleView;
import view.MainView;
import view.OrderTrackingView;
import view.OrderView;
import view.ProductAdminView;
import view.ReportView;
import view.ResearcherView;
import view.SimulatorView;

import java.util.Locale;
import java.util.Scanner;

/** Composition root for the console application. */
public final class FlashSaleApplication {
    private FlashSaleApplication() {
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.forLanguageTag("vi-VN"));
        if (args.length > 0 && "--generate-data".equalsIgnoreCase(args[0])) {
            generateDataFromCommandLine();
            return;
        }
        if (args.length > 0 && "--admin-hash".equalsIgnoreCase(args[0])) {
            AdminPasswordTool.run();
            return;
        }
        if (args.length > 0 && ("--benchmark".equalsIgnoreCase(args[0])
                || "--benchmark-report".equalsIgnoreCase(args[0]))) {
            int exitCode = BenchmarkCommand.run(
                    java.nio.file.Path.of("data"), System.out, System.err);
            if (exitCode != 0) {
                System.exit(exitCode);
            }
            return;
        }
        ConsoleInput input = new ConsoleInput(new Scanner(System.in));

        ProductRepository productRepository = new ProductRepository();
        FlashItemRepository flashItemRepository = new FlashItemRepository();
        FlashSaleEventRepository eventRepository = new FlashSaleEventRepository();
        CustomerRepository customerRepository = new CustomerRepository();
        OrderRepository orderRepository = new OrderRepository();
        OrderDetailRepository detailRepository = new OrderDetailRepository();
        OrderTransactionRepository transactionRepository = new OrderTransactionRepository();
        VoucherRepository voucherRepository = new VoucherRepository();

        FlashSaleEventService eventService = new FlashSaleEventService(eventRepository);
        FlashSaleService flashSaleService = new FlashSaleServiceImpl(
                flashItemRepository, orderRepository, detailRepository,
                customerRepository, voucherRepository, transactionRepository,
                eventRepository);
        SimulatorService simulatorService = new SimulatorService(transactionRepository);
        VoucherService voucherService = new VoucherService(voucherRepository);

        ProductController productController = new ProductController(productRepository);
        FlashSaleController flashSaleController = new FlashSaleController(
                flashItemRepository, eventService);
        OrderController orderController = new OrderController(flashSaleService);
        CustomerController customerController = new CustomerController(customerRepository);
        DataController dataController = new DataController(new DataGeneratorService());
        SimulatorController simulatorController = new SimulatorController(simulatorService);
        SimulationReportController simulationReportController = new SimulationReportController(
                new SimulationReportService());
        VoucherController voucherController = new VoucherController(voucherService);
        OrderTrackingController trackingController = new OrderTrackingController(
                orderRepository, detailRepository, transactionRepository,
                productRepository, flashItemRepository);

        CustomerAdminService customerAdminService = new CustomerAdminService(
                customerRepository, flashSaleService::cancelAllPendingAndApprovedOrders);
        AdminCustomerController adminCustomerController = new AdminCustomerController(
                customerAdminService);
        AdminOrderController adminOrderController = new AdminOrderController(
                new AdminOrderService(orderRepository, detailRepository, productRepository),
                orderController);
        AdminReportController adminReportController = new AdminReportController(
                new AdminReportService(orderRepository, customerRepository,
                        voucherRepository, transactionRepository));

        FlashSaleView flashSaleView = new FlashSaleView();
        OrderView orderView = new OrderView();
        SimulatorView simulatorView = new SimulatorView();
        ReportView reportView = new ReportView();

        CustomerAccountView accountView = new CustomerAccountView(customerController, input);
        FlashSaleShoppingView shoppingView = new FlashSaleShoppingView(
                flashSaleController, orderController, flashSaleView, orderView, input);
        OrderTrackingView trackingView = new OrderTrackingView(trackingController, input);
        CustomerVoucherView customerVoucherView = new CustomerVoucherView(voucherController);
        ResearcherView researcherView = new ResearcherView(
                simulatorController, simulationReportController, simulatorView, input);

        AdminView adminView = new AdminView(
                new ProductAdminView(productController, input),
                new FlashSaleEventAdminView(flashSaleController, input),
                new FlashSaleItemAdminView(flashSaleController, productController,
                        flashSaleView, input),
                new AdminCustomerView(adminCustomerController, input),
                new AdminVoucherView(voucherController, input),
                new AdminOrderView(adminOrderController, input),
                new AdminReportView(adminReportController),
                input);

        new MainView(
                customerController,
                dataController,
                accountView,
                shoppingView,
                trackingView,
                customerVoucherView,
                adminView,
                researcherView,
                reportView,
                AdminCredentials.fromEnvironment(),
                input)
                .display();
    }

    private static void generateDataFromCommandLine() {
        try {
            new ReportView().displayDataGenerationResult(
                    new DataGeneratorService().generateAll());
        } catch (java.io.IOException exception) {
            System.err.println("Không thể tạo dữ liệu CSV. Hãy kiểm tra quyền ghi thư mục data.");
            System.exit(1);
        }
    }
}
