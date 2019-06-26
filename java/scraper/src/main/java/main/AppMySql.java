/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

// ■Appとは
// 　・データベース
// 　・YNews記事保存する時は〜
// 　・目次ページから、リンクとタイトルが書かれている区画を全て抽出して、列挙する時は〜
// 　・リンクとタイトルが書かれている区画から、リンクとタイトルを抽出する時は〜
// 　・すでに取得済みのリンクとタイトルを除外する時は〜
// 　・リンクとタイトルで、記事のページをダウンロードして、記事本文を抽出。ニュース記事で戻る時は〜
// 　・ニュース記事を、HTMLファイルに追加保存する時は〜
// 　・ニュース記事を、全てLデータベースに投げる時は〜
public class AppMySql {
    
    // メンバ変数
    private final Database db = new Database();
    
    // データ型宣言
    // ■UrlTitle
    // 　・url
    // 　・title
    private class UrlTitle {
        private final String url;
        private final String title;

        public UrlTitle(String url , String title) {
            this.url = url;
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }
    }
    // データ型宣言
    // ■UrlTitle
    // 　・url
    // 　・title
    // 　・contents
    private class Article {
        private final String url;
        private final String title;
        private final String contents;

        public Article(String url, String title, String contents) {
            this.url = url;
            this.title = title;
            this.contents = contents;
        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }

        public String getContents() {
            return contents;
        }
    }
    
    // データベース補助コード
    private PreparedStatement createPreparedStatementToInsert(Connection con, List<Article> articles) throws SQLException {
        String sql = "INSERT IGNORE INTO articles (url, title, contents) VALUES (?,?,?);";
        PreparedStatement ps = con.prepareStatement(sql);
        for (Article article: articles) {
            ps.setString(1, article.getUrl());
            ps.setString(2, article.getTitle());
            ps.setString(3, article.getContents());
            ps.addBatch();
        }
        return ps;
    }
    
    // データベース補助コード
    private PreparedStatement createPreparedStatementToSelect(Connection con, List<UrlTitle> urlTitles) throws SQLException {
        String questions = urlTitles.stream().map(unt->"?").collect(Collectors.joining(","));
        String sql = "SELECT url FROM articles WHERE url IN (" + questions + ");";
        PreparedStatement ps = con.prepareStatement(sql);
        for (int i = 0; i < urlTitles.size(); i++) {
            ps.setString(i+1, urlTitles.get(i).getUrl());
        }
        return ps;
    }
    
    // データベース補助コード
    private Connection getConnection(boolean autoCommit) throws Exception {
        Connection conn = db.getHikari().getConnection();
        conn.setAutoCommit(autoCommit);
        return conn;
    }
    
    // ニュース記事を、全てデータベースに投げる
    private void saveArticlesDatabase(List<Article> articles) throws Exception {
        try (Connection        conn = getConnection(false);
             PreparedStatement ps   = createPreparedStatementToInsert(conn, articles);) {
           ps.executeBatch();
           conn.commit();
        }
    }
    
    // ニュース記事を、HTMLファイルに追加保存
    private void saveArticles(List<Article> articles) throws Exception {
        for (Article article: articles) {
            FileUtils.writeStringToFile(new File("articles.html"), "<hr>\n<h1>" + article.getTitle() + "</h1>\n" + article.getContents() + "\n", "UTF-8", true);
        }
    }
    
    // リンクとタイトルで、記事のページをダウンロードして、記事本文を抽出。ニュース記事で戻る
    private List<Article> getArticle(List<UrlTitle> urlsTitles) throws Exception {
        List<Article> articles = new ArrayList<>();
        for (UrlTitle urlTitle: urlsTitles) {
            String articlePage = IOUtils.toString(new URL(urlTitle.getUrl()), "UTF-8");
            String txt =  StringUtils.substringBetween(articlePage, "<div class=\"paragraph\">", "<!-- /.ynDetailText -->");
            articles.add(new Article(urlTitle.getUrl(), urlTitle.getTitle(), txt));
            System.out.println("＝＝＝記事本文＝＝＝\n" + txt);
        }
        return articles;
    }
    
    // すでに取得済みのリンクとタイトルを除外
    private List<UrlTitle> removeDuplication(List<UrlTitle> urlTitles) throws Exception {
        Set<String> urls = new HashSet<>();
        try (Connection        conn = getConnection(true);
             PreparedStatement ps   = createPreparedStatementToSelect(conn, urlTitles);
             ResultSet         rs   = ps.executeQuery();) {
           while (rs.next()) {
               urls.add(rs.getString("url"));
           }
        }
        return urlTitles.stream()
                .filter(unt->!urls.contains(unt.getUrl()))
                .collect(Collectors.toList());
    }
    
    // リンクとタイトルが書かれている区画から、リンクとタイトルを抽出
    private List<UrlTitle> extractUrlTitle(List<String> linkBlocks) {
        List<UrlTitle> urlTitles = new ArrayList<>();
        for (String linkBlock: linkBlocks) {
            String url = StringUtils.substringBetween(linkBlock, "href=\"", "\"");
            String ttl = StringUtils.substringBetween(linkBlock, "class=\"titl\">", "</dt");
            urlTitles.add(new UrlTitle(url, ttl));
            System.out.println("＝＝＝リンクとタイトル＝＝＝\n" + url + "\t" + ttl);
        }
        return urlTitles;
    }
    
    // 目次ページから、リンクとタイトルが書かれている区画を全て抽出して、列挙
    private List<String> extractLinkBlock(String indexPage) throws Exception {
        List<String> linkBlocks
            = Arrays.asList(StringUtils.substringsBetween(indexPage, "<li class=\"listFeedWrap \">", "</li>"));
        System.out.println("===リンクブロック==\n" + String.join(",", linkBlocks));
        return linkBlocks;
    }
    
    // YNews記事保存
    public void recordYnews() throws Exception {
        // 目次ページを取得
        String indexPage = IOUtils.toString(new URL("https://headlines.yahoo.co.jp/list/?m=kyodonews"), "UTF-8");
        // 目次ページから、リンクとタイトルが書かれている区画を全て抽出して、列挙
        List<String> linkBlocks = extractLinkBlock(indexPage);
        // リンクとタイトルが書かれている区画から、リンクとタイトルを抽出
        List<UrlTitle> urlsTitles = extractUrlTitle(linkBlocks);
        // すでに取得済みのリンクとタイトルを除外
        List<UrlTitle> urlsTitlesFiltered = removeDuplication(urlsTitles);
        // リンクとタイトルで、記事のページをダウンロードして、記事本文を抽出。ニュース記事で戻る
        List<Article> articles = getArticle(urlsTitlesFiltered);
        // ニュース記事を、HTMLファイルに追加保存
        saveArticles(articles);
        // ニュース記事を、全てデータベースに投げる
        saveArticlesDatabase(articles);
    }
    
    // プログラム本文
    // 　Appを作成して
    // 　そのrecordYnewsを実行
    // 　そのデータベースを破棄
    public static void main(String[] args) throws Exception {
        AppMySql app = new AppMySql();
        app.recordYnews();
        app.db.getHikari().close();
    }
   
}
// https://jyn.jp/java-hikaricp-mysql-sqlite/
class Database {
    private final HikariDataSource hikari;
    public Database() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://news-recorder.cnfoeuugi3ju.ap-northeast-1.rds.amazonaws.com/newsrecorder"
                + "?useUnicode=true"
                + "&characterEncoding=utf8"
                + "&useSSL=true&requireSSL=true"
                + "&verifyServerCertificate=true"
                + "&trustCertificateKeyStoreUrl=" + new File("rds.jks").toURI() 
                + "&trustCertificateKeyStoreType=JKS"
                + "&trustCertificateKeyStorePassword=amazon");
        config.setConnectionTimeout(4000);
        config.addDataSourceProperty("user", "root");
        config.addDataSourceProperty("password", System.getenv("NEWS_PASS"));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.setInitializationFailFast(true);
        config.setConnectionInitSql("SELECT 1");
        hikari = new HikariDataSource(config);
    }
    public HikariDataSource getHikari() {
        return hikari;
    }
}