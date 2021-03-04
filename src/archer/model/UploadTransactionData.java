package archer.model;

import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UploadTransactionData {

    private final List<File> dirList;
    private final List<File> fileList;
    private final Map<File, SVNNodeKind> kindMap = new HashMap<>();
    private final Map<File, Long> fileSizeMap = new HashMap<>();
    private final Map<File, Long> prevSizeMap = new HashMap<>();
    private long totalSize;

    public UploadTransactionData(SVNRepository repository, List<File> dirList, List<File> fileList,
                                 Map<File, String> uploadPathMap) throws Exception {
        this.dirList = dirList != null ? dirList : new LinkedList<>();
        this.fileList = fileList != null ? fileList : new LinkedList<>();
        if (this.dirList.isEmpty() && this.fileList.isEmpty()) {
            throw new Exception("上传队列为空");
        }
        for (File dir : this.dirList) {
            SVNNodeKind kind = repository.checkPath(uploadPathMap.get(dir), -1);
            if (kind == SVNNodeKind.FILE) {
                throw new Exception("存在同名文件，不能上传文件夹：" + dir.getCanonicalPath());
            }
            kindMap.put(dir, kind);
        }
        totalSize = 0;
        for (File file : this.fileList) {
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

    public int length() {
        return fileList.size();
    }

    public int indexOf(File file) {
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
}
