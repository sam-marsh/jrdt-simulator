package transport;

/**
 * The unit of data passed by the application layer to the transport protocol (the layer students implement)
 */
public class Message {

    private String data; // application level data

    public Message(String inputData) {
        data = inputData;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return data;
    }

}
