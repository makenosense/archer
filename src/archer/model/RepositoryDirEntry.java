package archer.model;

import archer.util.FileUtil;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNNodeKind;

import java.text.Collator;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

public class RepositoryDirEntry {
    private static final String TYPE_CODE_DIR = "DIR";
    private static final String TYPE_CODE_FILE = "FILE";
    private static final String TYPE_CODE_UNKNOWN = "UNKNOWN";

    private static final String TYPE_DIR = "文件夹";
    private static final String TYPE_FILE = "文件";
    private static final String TYPE_UNKNOWN = "未知";

    private static final String ICON_CLASS_DIR = "fas fa-folder";
    private static final String ICON_CLASS_FILE = "far fa-file";

    private static final String ICON_CLASS_FILE_WORD = "far fa-file-word";
    private static final String ICON_CLASS_FILE_EXCEL = "far fa-file-excel";
    private static final String ICON_CLASS_FILE_POWERPOINT = "far fa-file-powerpoint";
    private static final String ICON_CLASS_FILE_PDF = "far fa-file-pdf";
    private static final String ICON_CLASS_FILE_IMAGE = "far fa-file-image";
    private static final String ICON_CLASS_FILE_AUDIO = "far fa-file-audio";
    private static final String ICON_CLASS_FILE_VIDEO = "far fa-file-video";
    private static final String ICON_CLASS_FILE_CODE = "far fa-file-code";
    private static final String ICON_CLASS_FILE_ARCHIVE = "far fa-file-archive";

    private static final HashSet<String> EXT_WORD = extStringToSet("doc,docx");
    private static final HashSet<String> EXT_EXCEL = extStringToSet("xls,xlsx");
    private static final HashSet<String> EXT_POWERPOINT = extStringToSet("ppt,pptx");
    private static final HashSet<String> EXT_PDF = extStringToSet("pdf");
    private static final HashSet<String> EXT_IMAGE = extStringToSet("png,jpg,jpeg,gif");
    private static final HashSet<String> EXT_AUDIO = extStringToSet("mp3");
    private static final HashSet<String> EXT_VIDEO = extStringToSet("mp4");
    private static final HashSet<String> EXT_CODE = extStringToSet("java,py");
    private static final HashSet<String> EXT_ARCHIVE = extStringToSet("zip,rar");

    private static HashSet<String> extStringToSet(String extString) {
        return new HashSet<>(Arrays.asList(extString.split(",")));
    }

    private static String getFileIconClass(String ext) {
        if (ext.length() > 0) {
            if (EXT_WORD.contains(ext)) {
                return ICON_CLASS_FILE_WORD;
            } else if (EXT_EXCEL.contains(ext)) {
                return ICON_CLASS_FILE_EXCEL;
            } else if (EXT_POWERPOINT.contains(ext)) {
                return ICON_CLASS_FILE_POWERPOINT;
            } else if (EXT_PDF.contains(ext)) {
                return ICON_CLASS_FILE_PDF;
            } else if (EXT_IMAGE.contains(ext)) {
                return ICON_CLASS_FILE_IMAGE;
            } else if (EXT_AUDIO.contains(ext)) {
                return ICON_CLASS_FILE_AUDIO;
            } else if (EXT_VIDEO.contains(ext)) {
                return ICON_CLASS_FILE_VIDEO;
            } else if (EXT_CODE.contains(ext)) {
                return ICON_CLASS_FILE_CODE;
            } else if (EXT_ARCHIVE.contains(ext)) {
                return ICON_CLASS_FILE_ARCHIVE;
            }
        }
        return ICON_CLASS_FILE;
    }

    private final SVNDirEntry entry;

    public RepositoryDirEntry(SVNDirEntry entry) {
        this.entry = entry;
    }

    public String getTypeCode() {
        if (entry.getKind() == SVNNodeKind.DIR) {
            return TYPE_CODE_DIR;
        } else if (entry.getKind() == SVNNodeKind.FILE) {
            return TYPE_CODE_FILE;
        } else {
            return TYPE_CODE_UNKNOWN;
        }
    }

    public String getType() {
        switch (getTypeCode()) {
            case TYPE_CODE_DIR:
                return TYPE_DIR;
            case TYPE_CODE_FILE:
                return String.format("%s%s", getExt(), TYPE_FILE);
            default:
                return TYPE_UNKNOWN;
        }
    }

    public boolean isDir() {
        return TYPE_CODE_DIR.equals(getTypeCode());
    }

    public String getIconClass() {
        switch (getTypeCode()) {
            case TYPE_CODE_DIR:
                return ICON_CLASS_DIR;
            case TYPE_CODE_FILE:
                return getFileIconClass(getExt());
            default:
                return "";
        }
    }

    @Override
    public String toString() {
        return entry.getName();
    }

    public String getName() {
        return entry.getName();
    }

    public Date getDate() {
        return entry.getDate();
    }

    public String getMtime() {
        Date date = getDate();
        return String.format("%tF %tT", date, date);
    }

    public String getExt() {
        String name = getName();
        int rIdx = name.lastIndexOf('.');
        return rIdx >= 0 ? name.substring(rIdx + 1) : "";
    }

    public long getSize() {
        return entry.getSize();
    }

    public String getSizeString() {
        return TYPE_CODE_FILE.equals(getTypeCode()) ? FileUtil.getSizeString(getSize()) : "-";
    }

    /**
     * 比较器
     */
    private static final String STARTS_WITH_LETTER_OR_DIGIT = "^\\w";
    private static final Collator CHINESE_COMPARATOR = Collator.getInstance(Locale.CHINA);

    private static int nameCompare(RepositoryDirEntry o1, RepositoryDirEntry o2) {
        if (o1.getName().matches(STARTS_WITH_LETTER_OR_DIGIT)
                || o2.getName().matches(STARTS_WITH_LETTER_OR_DIGIT)) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
        return CHINESE_COMPARATOR.compare(o1.getName(), o2.getName());
    }

    private static int mtimeCompare(RepositoryDirEntry o1, RepositoryDirEntry o2) {
        return o1.getMtime().compareTo(o2.getMtime());
    }

    private static int typeCompare(RepositoryDirEntry o1, RepositoryDirEntry o2) {
        return o1.getType().compareTo(o2.getType());
    }

    public static int entryNameCompare(RepositoryDirEntry o1, RepositoryDirEntry o2) {
        if (o1.isDir()) {
            return o2.isDir() ? nameCompare(o1, o2) : -1;
        } else {
            return !o2.isDir() ? nameCompare(o1, o2) : 1;
        }
    }

    public static int entryMtimeCompare(RepositoryDirEntry o1, RepositoryDirEntry o2) {
        if (o1.isDir()) {
            return o2.isDir() ? mtimeCompare(o1, o2) : -1;
        } else {
            return !o2.isDir() ? mtimeCompare(o1, o2) : 1;
        }
    }

    public static int entryTypeCompare(RepositoryDirEntry o1, RepositoryDirEntry o2) {
        if (typeCompare(o1, o2) == 0) {
            return nameCompare(o1, o2);
        }
        return typeCompare(o1, o2);
    }

    public static int entrySizeCompare(RepositoryDirEntry o1, RepositoryDirEntry o2) {
        if (o1.isDir()) {
            return o2.isDir() ? nameCompare(o1, o2) : -1;
        } else {
            return !o2.isDir() ? Long.compare(o1.getSize(), o2.getSize()) : 1;
        }
    }

    public static int entryNameCompareRev(RepositoryDirEntry o1, RepositoryDirEntry o2) {
        if (o1.isDir()) {
            return o2.isDir() ? nameCompare(o2, o1) : -1;
        } else {
            return !o2.isDir() ? nameCompare(o2, o1) : 1;
        }
    }

    public static int entryMtimeCompareRev(RepositoryDirEntry o1, RepositoryDirEntry o2) {
        if (o1.isDir()) {
            return o2.isDir() ? mtimeCompare(o2, o1) : -1;
        } else {
            return !o2.isDir() ? mtimeCompare(o2, o1) : 1;
        }
    }

    public static int entryTypeCompareRev(RepositoryDirEntry o1, RepositoryDirEntry o2) {
        if (typeCompare(o1, o2) == 0) {
            return nameCompare(o1, o2);
        }
        return typeCompare(o2, o1);
    }

    public static int entrySizeCompareRev(RepositoryDirEntry o1, RepositoryDirEntry o2) {
        if (o1.isDir()) {
            return o2.isDir() ? nameCompare(o1, o2) : -1;
        } else {
            return !o2.isDir() ? Long.compare(o2.getSize(), o1.getSize()) : 1;
        }
    }
}
