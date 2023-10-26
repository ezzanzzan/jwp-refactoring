package kitchenpos.dto.request;

public class OrderChangeStatusRequest {
    private String orderStatus;

    public OrderChangeStatusRequest() {
    }

    public OrderChangeStatusRequest(final String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderStatus() {
        return orderStatus;
    }
}
