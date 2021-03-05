package archer.editor;

import archer.model.AppSettings;
import archer.model.RepositoryPathNode;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

public class DownloadEditor implements ISVNEditor {

    private final SVNDeltaProcessor deltaProcessor = new SVNDeltaProcessor();
    private final RepositoryPathNode parentPathNode;
    private final File downloadParent;
    private final LinkedList<File> newEntries = new LinkedList<>();

    public DownloadEditor(RepositoryPathNode parentPathNode, File downloadParent) {
        this.parentPathNode = parentPathNode != null ? parentPathNode : new RepositoryPathNode(Paths.get("/"));
        this.downloadParent = downloadParent != null ? downloadParent : AppSettings.load().getDownloadParent();
    }

    private File getDownloadTarget(String srcPathString) throws SVNException {
        Path parentPath = parentPathNode.getPath();
        Path srcPath = Paths.get("/", srcPathString).normalize();
        if (!srcPath.startsWith(parentPath) || parentPath.startsWith(srcPath)) {
            throwIOException("目标路径不在下载范围内");
        }
        return new File(downloadParent, parentPathNode.relativize(srcPath).toString());
    }

    private void throwIOException(String msg) throws SVNException {
        SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.IO_ERROR),
                new Exception(msg), SVNLogType.DEFAULT);
    }

    @Override
    public void targetRevision(long revision) {
        // Do nothing
    }

    @Override
    public void openRoot(long revision) throws SVNException {
        if (!downloadParent.isDirectory()) {
            if (!downloadParent.mkdirs()) {
                throwIOException("下载文件夹创建失败：" + downloadParent.getAbsolutePath());
            }
        }
    }

    @Override
    public void deleteEntry(String path, long revision) throws SVNException {
        // Do nothing
    }

    @Override
    public void absentDir(String path) throws SVNException {
        // Do nothing
    }

    @Override
    public void absentFile(String path) throws SVNException {
        // Do nothing
    }

    @Override
    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        File newDir = getDownloadTarget(path);
        if (!newDir.isDirectory()) {
            if (newDir.exists()) {
                throwIOException("不能在此路径创建文件夹：" + newDir.getAbsolutePath());
            }
            newEntries.addFirst(newDir);
            if (!newDir.mkdirs()) {
                throwIOException("文件夹创建失败：" + newDir.getAbsolutePath());
            }
        }
    }

    @Override
    public void openDir(String path, long revision) throws SVNException {
        // Do nothing
    }

    @Override
    public void changeDirProperty(String name, SVNPropertyValue value) {
        // Do nothing
    }

    @Override
    public void closeDir() throws SVNException {
        // Do nothing
    }

    @Override
    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        File newFile = getDownloadTarget(path);
        if (newFile.exists()) {
            throwIOException("文件已存在：" + newFile.getAbsolutePath());
        }
        newEntries.addFirst(newFile);
        try {
            newFile.createNewFile();
        } catch (IOException e) {
            throwIOException("文件创建失败：" + newFile.getAbsolutePath());
        }
    }

    @Override
    public void openFile(String path, long revision) throws SVNException {
        // Do nothing
    }

    @Override
    public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) {
        // Do nothing
    }

    @Override
    public void closeFile(String path, String textChecksum) throws SVNException {
        // Do nothing
    }

    @Override
    public SVNCommitInfo closeEdit() throws SVNException {
        return null;
    }

    @Override
    public void abortEdit() throws SVNException {
        for (File entry : newEntries) {
            if (entry.exists()) {
                File[] children = entry.listFiles();
                if (!(children != null && children.length > 0)) {
                    entry.delete();
                }
            }
        }
    }

    @Override
    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
        deltaProcessor.applyTextDelta((File) null, getDownloadTarget(path), false);
    }

    @Override
    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        return deltaProcessor.textDeltaChunk(diffWindow);
    }

    @Override
    public void textDeltaEnd(String path) {
        deltaProcessor.textDeltaEnd();
    }
}
