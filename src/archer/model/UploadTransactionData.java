package archer.model;

import javafx.concurrent.Task;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;
import java.util.*;

public class UploadTransactionData {

    private final List<File> dirList;
    private final List<File> fileList;
    private final Map<File, SVNNodeKind> kindMap = new HashMap<>();
    private final Map<File, Long> fileSizeMap = new HashMap<>();
    private final Map<File, Long> prevSizeMap = new HashMap<>();
    private long totalSize;

    private Long lastRecordTime;
    private long lastTotalSent = 0;
    private String lastRemainingTimeString = "inf";

    public UploadTransactionData(SVNRepository repository, List<File> dirList, List<File> fileList,
                                 Map<File, String> uploadPathMap, Task<Void> task) throws Exception {
        this.dirList = dirList != null ? dirList : new LinkedList<>();
        this.fileList = fileList != null ? fileList : new LinkedList<>();
        if (this.dirList.isEmpty() && this.fileList.isEmpty()) {
            throw new Exception("上传队列为空");
        }
        for (File dir : this.dirList) {
            if (task.isCancelled()) {
                throw new UploadCancelledException();
            }
            SVNNodeKind kind = repository.checkPath(uploadPathMap.get(dir), -1);
            if (kind == SVNNodeKind.FILE) {
                throw new Exception("存在同名文件，不能上传文件夹：" + dir.getCanonicalPath());
            }
            kindMap.put(dir, kind);
        }
        totalSize = 0;
        for (File file : this.fileList) {
            if (task.isCancelled()) {
                throw new UploadCancelledException();
            }
            SVNNodeKind kind = repository.checkPath(uploadPathMap.get(file), -1);
            if (kind == SVNNodeKind.DIR) {
                throw new Exception("存在同名文件夹，不能上传文件：" + file.getCanonicalPath());
            }
            kindMap.put(file, kind);
            fileSizeMap.put(file, file.length());
            prevSizeMap.put(file, totalSize);
            totalSize += file.length();
        }
    }

    public List<File> dirList() {
        return dirList;
    }

    public List<File> fileList() {
        return fileList;
    }

    public int lengthOfDirs() {
        return dirList.size();
    }

    public int lengthOfFiles() {
        return fileList.size();
    }

    public int indexOfDir(File dir) {
        return dirList.indexOf(dir);
    }

    public int indexOfFile(File file) {
        return fileList.indexOf(file);
    }

    public SVNNodeKind getKind(File file) {
        return kindMap.getOrDefault(file, SVNNodeKind.NONE);
    }

    public long getSize(File file) {
        return fileSizeMap.getOrDefault(file, 0L);
    }

    public long getPrevSize(File file) {
        return prevSizeMap.getOrDefault(file, 0L);
    }

    public long getTotalSize() {
        return totalSize;
    }

    public String getRemainingTimeString(long totalSent) {
        long recordTime = new Date().getTime();
        if (lastRecordTime == null) {
            lastRecordTime = recordTime;
            lastTotalSent = totalSent;
            return lastRemainingTimeString;
        }
        if (recordTime - lastRecordTime < 5000
                && !"inf".equals(lastRemainingTimeString)) {
            return lastRemainingTimeString;
        }
        if (totalSent >= totalSize) {
            lastRemainingTimeString = "00:00:00";
            return lastRemainingTimeString;
        }
        if (totalSent <= lastTotalSent) {
            lastRecordTime = recordTime;
            lastTotalSent = totalSent;
            lastRemainingTimeString = "inf";
            return lastRemainingTimeString;
        }
        double remainingTime = 1. * (totalSize - totalSent) * (recordTime - lastRecordTime) / (totalSent - lastTotalSent);
        remainingTime /= 1000;
        int hour = (int) (remainingTime / 3600);
        int minute = (int) ((remainingTime % 3600) / 60);
        int second = (int) (remainingTime % 60);
        lastRecordTime = recordTime;
        lastTotalSent = totalSent;
        lastRemainingTimeString = String.format("%d:%02d:%02d", hour, minute, second);
        return lastRemainingTimeString;
    }
}
