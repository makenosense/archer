package archer.model;

import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadTransactionData {

    private final List<File> fileList;
    private final Map<File, SVNNodeKind> fileKindMap = new HashMap<>();
    private final Map<File, Long> fileSizeMap = new HashMap<>();
    private final Map<File, Long> prevSizeMap = new HashMap<>();
    private long totalSize;

    public UploadTransactionData(SVNRepository repository, List<File> fileList,
                                 Map<File, String> RepositoryFilePathMap) throws Exception {
        if (fileList == null || fileList.isEmpty()) {
            throw new Exception("上传文件队列为空");
        }
        this.fileList = fileList;
        totalSize = 0;
        for (File file : fileList) {
            fileKindMap.put(file, repository.checkPath(RepositoryFilePathMap.get(file), -1));
            fileSizeMap.put(file, file.length());
            prevSizeMap.put(file, totalSize);
            totalSize += file.length();
        }
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
        return fileKindMap.getOrDefault(file, SVNNodeKind.NONE);
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
