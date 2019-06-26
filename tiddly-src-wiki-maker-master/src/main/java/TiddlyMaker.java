import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.FileSystems;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class TiddlyMaker {
    private final String path;
    private final String extension;
    private final String postfix;
    private final String bTitle;
    private final String charset;
    private final String lang;
    private final boolean splitDir;

    public TiddlyMaker(String  path,
                       String  extension,
                       String  postfix,
                       String  bTitle,
                       String  charset,
                       String  lang,
                       boolean splitDir) {
        this.path      = path;
        this.extension = extension;
        this.postfix   = postfix;
        this.bTitle    = bTitle;
        this.charset   = charset;
        this.splitDir  = splitDir;
        this.lang      = lang;
    }

    private String buildTags(String dir) {
        List<String> tags = new ArrayList<>();
        tags.add(bTitle);
        tags.add(dir);
        if (splitDir) {
            tags.add(dir.replace("/", " "));
        }
        return tags.stream().collect(Collectors.joining(" "));
    }

    private String getUniqueTitle(String title, Set<String> titles) {
        if (!titles.contains(title)) {
            titles.add(title);
            return title;
        }
        for (int i = 2;; i++) {
            String numberedTitle = title + "_" + Integer.toString(i);
            if (!titles.contains(numberedTitle)) {
                titles.add(numberedTitle);
                return numberedTitle;
            }
        }
    }

    private List<Path> getPathes(String rootPath, String glob) throws Exception {
        try (Stream<Path> entries = Files.walk(Paths.get(rootPath))) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher(glob);
            return entries.filter(matcher::matches).collect(Collectors.toList());
        }
    }

    private void compileTiddlyWiki() throws Exception {
        File          toSave  = new File(String.join("", "tiddlywiki_", postfix, ".html"));
        File          partF   = new File(String.join("", "tiddlywiki_", postfix, "-part.html"));
        Set<String>   titles  = new TreeSet<>();
        String        tidHead = FileUtils.readFileToString(new File("tidHeadMd.html"), "UTF-8");
        String        tidFoot = FileUtils.readFileToString(new File("tidFootMd.html"), "UTF-8");
        StringBuilder stringB = new StringBuilder();

        List<Path> pz = getPathes(path, "glob:**." + extension);
        for (Path ps : pz) {
            String title     = StringUtils.substringBeforeLast(ps.getFileName().toString(), ".");
            String uniqTitle = getUniqueTitle(title, titles);
            String afterRoot = StringUtils.substringAfter(ps.toString(), path);
            String dir       = StringUtils.substringBeforeLast(afterRoot, "/"); //last if not java 
            String contents  = FileUtils.readFileToString(ps.toFile(), charset).trim();
            String tags      = buildTags(dir);
            stringB
                .append("<div title=\"")
                .append(uniqTitle)
                .append("\" creator=\"system\" modifier=\"system\" created=\"201001010101\" modified=\"201001010101\"")
                .append(" tags=\"")
                .append(tags)
                .append("\" changecount=\"1\">\n")
                .append("\n<pre>&lt;md&gt;\n```")
                .append(lang)
                .append("\n")
                .append(contents.replace("<", "&lt;").replace(">", "&gt;"))
                .append("\n```\n&lt;/md&gt;</pre>\n")
                .append("</div>\n");
        }
        FileUtils.writeStringToFile(partF, stringB.toString(), "UTF-8");
        stringB
            .insert(0, "<!-- book tiddlers -->\n")
            .insert(0, tidHead)
            .append("<!-- /book tiddlers -->\n")
            .append(tidFoot);
        FileUtils.writeStringToFile(toSave, stringB.toString(), "UTF-8");
    }

    public static void main(String[] args) throws Exception {
        new TiddlyMaker(
            "/home/ryoji/Downloads/python_jissen-sample/src/",
            "py",                  /*.nako*/
            "python-jissen",             /*tiddlywiki_nadeshiko.html*/
            "実践力を身につけるpythonの教科書",
            "UTF-8",             /*UTF-8, SHIFT-JIS ソースファイル文字コード*/
            "python",
            true)                    /*Chap05/05-03/エディタの編集.nako tags+=[Chap05, 05-03] */
            .compileTiddlyWiki();
    }
}
////contents.replaceAll("<(\"[^\"]*\"|'[^']*'|[^'\">])*>", "")
