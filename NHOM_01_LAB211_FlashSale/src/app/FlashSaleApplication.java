package app;

import controller.CustomerController;
import controller.DataController;
import controller.FlashSaleController;
import controller.OrderController;
import controller.OrderTrackingController;
import controller.SimulatorController;
import repository.CustomerRepository;
import repository.FlashItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.VoucherRepository;
import service.DataGeneratorService;
import service.FlashSaleService;
import service.FlashSaleServiceImpl;
import service.SimulatorService;
import view.FlashSaleView;
import view.MainView;
import view.OrderView;
import view.ReportView;
import view.SimulatorView;

public class FlashSaleApplication {
    public static void main(String[] args) {
        FlashItemRepository flashItemRepository = new FlashItemRepository();
        CustomerRepository customerRepository = new CustomerRepository();
        OrderRepository orderRepository = new OrderRepository();
        OrderDetailRepository orderDetailRepository = new OrderDetailRepository();
        OrderTransactionRepository transactionRepository = new OrderTransactionRepository();
        VoucherRepository voucherRepository = new VoucherRepository();

        FlashSaleService flashSaleService = new FlashSaleServiceImpl(flashItemRepository, orderRepository, orderDetailRepository, customerRepository, voucherRepository);
        DataGeneratorService dataGeneratorService = new DataGeneratorService();
        SimulatorService simulatorService = new SimulatorService(transactionRepository);

        FlashSaleController flashSaleController = new FlashSaleController(flashItemRepository);
        OrderController orderController = new OrderController(flashSaleService);
        CustomerController customerController = new CustomerController(customerRepository);
        DataController dataController = new DataController(dataGeneratorService);
        SimulatorController simulatorController = new SimulatorController(simulatorService);
        OrderTrackingController orderTrackingController = new OrderTrackingController(
                orderRepository, orderDetailRepository, transactionRepository);

        repository.FlashSaleEventRepository eventRepository = new repository.FlashSaleEventRepository();

        MainView mainView = new MainView(
                flashSaleController,
                orderController,
                customerController,
                dataController,
                simulatorController,
                orderTrackingController,
                new FlashSaleView(),
                new OrderView(),
                new SimulatorView(),
                new ReportView(),
                eventRepository,
                customerRepository,
                voucherRepository,
                orderRepository,
                flashItemRepository
        );

        mainView.display();
    }
}
