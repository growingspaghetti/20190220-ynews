/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

// ■Appとは
// 　・YNews記事保存する時は〜
// 　・目次ページから、リンクとタイトルが書かれている区画を全て抽出して、列挙する時は〜
// 　・リンクとタイトルが書かれている区画から、リンクとタイトルを抽出する時は〜
// 　・リンクとタイトルで、記事のページをダウンロードして、記事本文を抽出。ニュース記事で戻る時は〜
// 　・ニュース記事を、HTMLファイルに追加保存する時は〜
// 　・ニュース記事を、全てデータベースに投げる時は〜
public class AppSqlite {

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
        String sql = "INSERT INTO articles (url, title, contents) VALUES (?,?,?);";
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
    private Connection getConnection(boolean autoCommit) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:articles.db");
        conn.setAutoCommit(autoCommit);
        return conn;
    }
    
    // データベース補助コード
    private void prepareDatabase() throws Exception {
        try (Statement stmt = getConnection(true).createStatement();) {
            stmt.execute("CREATE TABLE IF NOT EXISTS articles (url TEXT, title TEXT, contents TEXT)");
        }
    }
    
    // ニュース記事を、全てデータベースに投げる
    private void saveArticlesDatabase(List<Article> articles) throws Exception {
        prepareDatabase();
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
        // リンクとタイトルで、記事のページをダウンロードして、記事本文を抽出。ニュース記事で戻る
        List<Article> articles = getArticle(urlsTitles);
        // ニュース記事を、HTMLファイルに追加保存
        saveArticles(articles);
        // ニュース記事を、全てデータベースに投げる
        saveArticlesDatabase(articles);
    }
    
    // プログラム本文
    // 　Appを作成して
    // 　そのrecordYnewsを実行
    public static void main(String[] args) throws Exception {
        new AppSqlite().recordYnews();
    }
}