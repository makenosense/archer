package archer.util;

public class FileUtil {
    public static String getSizeString(double size) {
        int unitSize = 1 << 10;
        String unit = "B";
        if (size > unitSize) {
            size /= unitSize;
            unit = "KB";
            if (size > unitSize) {
                size /= unitSize;
                unit = "MB";
                if (size > unitSize) {
                    size /= unitSize;
                    unit = "GB";
                    if (size > unitSize) {
                        size /= unitSize;
                        unit = "TB";
                    }
                }
            }
        }
        return String.format("%.2f %s", size, unit);
    }
}
