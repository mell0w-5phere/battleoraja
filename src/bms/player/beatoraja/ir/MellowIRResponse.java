package bms.player.beatoraja.ir;

public class MellowIRResponse<T> implements IRResponse<T> {
    private boolean success;
    private String msg;
    private T data;

    public MellowIRResponse(boolean success, String msg, T data) {
        this.success = success;
        this.msg = msg;
        this.data = data;
    }

    public boolean isSucceeded() {
        return success;
    }

    public String getMessage() {
        return msg;
    }

    public T getData() {
        return data;
    }
}
