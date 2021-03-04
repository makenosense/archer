package archer.model;

public class UploadCancelledException extends Exception {
    public UploadCancelledException() {
        super("上传已取消");
    }
}
